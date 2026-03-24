package io.github.architectplatform.core.watch

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Recursive directory watcher using Java's [WatchService] API.
 *
 * Monitors a root directory and all subdirectories for file changes, with optional
 * glob-based filtering and debounce support.
 *
 * @param rootPath The root directory to watch
 * @param globPatterns Optional glob patterns to filter which files trigger events (e.g., "*.kt", "*.java")
 * @param debounceMs Minimum interval between consecutive change callbacks
 * @param onChange Callback invoked when a matching file changes (receives the changed path)
 */
class FileWatchService(
    private val rootPath: Path,
    private val globPatterns: List<String> = emptyList(),
    private val debounceMs: Long = 500,
    private val onChange: (Path) -> Unit,
) {

  private val running = AtomicBoolean(false)
  private val watchKeys = ConcurrentHashMap<WatchKey, Path>()
  @Volatile private var watchThread: Thread? = null

  private val matchers: List<PathMatcher> by lazy {
    val fs = FileSystems.getDefault()
    globPatterns.map { pattern ->
      fs.getPathMatcher("glob:$pattern")
    }
  }

  /**
   * Starts watching the root directory recursively. Blocks the calling thread
   * until [stop] is called.
   */
  fun start() {
    if (!running.compareAndSet(false, true)) return
    val ws = FileSystems.getDefault().newWatchService()
    try {
      registerAll(rootPath, ws)
      watchLoop(ws)
    } finally {
      ws.close()
      running.set(false)
    }
  }

  /**
   * Starts watching in a background daemon thread. Returns immediately.
   */
  fun startAsync() {
    val t = Thread({ start() }, "architect-file-watcher")
    t.isDaemon = true
    watchThread = t
    t.start()
  }

  /**
   * Stops the watch loop. Safe to call from any thread.
   */
  fun stop() {
    running.set(false)
    watchThread?.interrupt()
  }

  val isRunning: Boolean get() = running.get()

  private fun registerAll(start: Path, ws: WatchService) {
    Files.walkFileTree(start, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (shouldSkipDirectory(dir)) return FileVisitResult.SKIP_SUBTREE
        val key = try {
          dir.register(ws,
              arrayOf(
                  StandardWatchEventKinds.ENTRY_CREATE,
                  StandardWatchEventKinds.ENTRY_MODIFY,
                  StandardWatchEventKinds.ENTRY_DELETE),
              com.sun.nio.file.SensitivityWatchEventModifier.HIGH)
        } catch (_: UnsupportedOperationException) {
          dir.register(ws,
              StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_MODIFY,
              StandardWatchEventKinds.ENTRY_DELETE)
        }
        watchKeys[key] = dir
        return FileVisitResult.CONTINUE
      }
    })
  }

  private fun watchLoop(ws: WatchService) {
    var lastEventTime = 0L

    while (running.get()) {
      val key = try {
        ws.take()
      } catch (_: InterruptedException) {
        break
      } catch (_: ClosedWatchServiceException) {
        break
      }

      val dir = watchKeys[key] ?: continue

      for (event in key.pollEvents()) {
        val kind = event.kind()
        if (kind == StandardWatchEventKinds.OVERFLOW) continue

        @Suppress("UNCHECKED_CAST")
        val ev = event as WatchEvent<Path>
        val child = dir.resolve(ev.context())

        // Register new directories
        if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(child)) {
          registerAll(child, ws)
        }

        // Filter by patterns
        if (!matchesPatterns(child)) continue

        // Debounce
        val now = System.currentTimeMillis()
        if (now - lastEventTime < debounceMs) continue
        lastEventTime = now

        onChange(child)
      }

      val valid = key.reset()
      if (!valid) {
        watchKeys.remove(key)
        if (watchKeys.isEmpty()) break
      }
    }
  }

  private fun matchesPatterns(path: Path): Boolean {
    if (matchers.isEmpty()) return true
    val fileName = path.fileName ?: return false
    return matchers.any { it.matches(fileName) }
  }

  private fun shouldSkipDirectory(dir: Path): Boolean {
    val name = dir.fileName?.toString() ?: return false
    return name.startsWith(".") || name == "node_modules" || name == "build" || name == "target" || name == "__pycache__"
  }
}
