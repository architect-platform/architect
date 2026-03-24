package io.github.architectplatform.core.plugin.app

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class ClassloaderIsolationTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `child-first classloader resolves plugin class before parent`() {
    // A class in the plugin JAR should be found by the child, not the parent
    val jar = createJarWithClass("com.example.PluginClass", "1")
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
    )
    // The class should be loadable (it's in the plugin JAR, not in the parent)
    val clazz = loader.loadClass("com.example.PluginClass")
    assertNotNull(clazz)
    assertEquals("com.example.PluginClass", clazz.name)
    assertEquals(loader, clazz.classLoader)
    loader.close()
  }

  @Test
  fun `shared API classes delegate to parent classloader`() {
    val jar = createEmptyJar()
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
    )
    // architect-api classes should come from parent
    val apiClass = loader.loadClass("io.github.architectplatform.api.core.plugins.ArchitectPlugin")
    assertNotNull(apiClass)
    // Should NOT be loaded from the plugin JAR
    assertNotEquals(loader, apiClass.classLoader)
    loader.close()
  }

  @Test
  fun `bootstrap classes delegate to parent`() {
    val jar = createEmptyJar()
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
    )
    val stringClass = loader.loadClass("java.lang.String")
    assertEquals(String::class.java, stringClass)
    loader.close()
  }

  @Test
  fun `kotlin classes delegate to parent`() {
    val jar = createEmptyJar()
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
    )
    val unitClass = loader.loadClass("kotlin.Unit")
    assertNotNull(unitClass)
    assertNotEquals(loader, unitClass.classLoader)
    loader.close()
  }

  @Test
  fun `two isolated classloaders do not share plugin classes`() {
    val jarA = createJarWithClass("com.example.Conflict", "A")
    val jarB = createJarWithClass("com.example.Conflict", "B")

    val loaderA = IsolatedPluginClassLoader(
      arrayOf(jarA.toURI().toURL()),
      this::class.java.classLoader,
    )
    val loaderB = IsolatedPluginClassLoader(
      arrayOf(jarB.toURI().toURL()),
      this::class.java.classLoader,
    )

    val classA = loaderA.loadClass("com.example.Conflict")
    val classB = loaderB.loadClass("com.example.Conflict")

    // Same class name but loaded from different classloaders → different Class objects
    assertNotSame(classA, classB)
    assertEquals(loaderA, classA.classLoader)
    assertEquals(loaderB, classB.classLoader)

    loaderA.close()
    loaderB.close()
  }

  @Test
  fun `debug flag does not affect resolution`() {
    val jar = createJarWithClass("com.example.DebugTest", "1")
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
      debug = true,
    )
    val clazz = loader.loadClass("com.example.DebugTest")
    assertNotNull(clazz)
    assertEquals(loader, clazz.classLoader)
    loader.close()
  }

  @Test
  fun `class not found in plugin or parent throws ClassNotFoundException`() {
    val jar = createEmptyJar()
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
    )
    assertThrows(ClassNotFoundException::class.java) {
      loader.loadClass("com.does.not.Exist")
    }
    loader.close()
  }

  @Test
  fun `resource resolution is child-first`() {
    val jar = createJarWithResource("plugin.properties", "version=1.0")
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
    )
    val resource = loader.getResource("plugin.properties")
    assertNotNull(resource)
    val content = resource!!.readText()
    assertEquals("version=1.0", content)
    loader.close()
  }

  @Test
  fun `custom shared packages are respected`() {
    val jar = createEmptyJar()
    val loader = IsolatedPluginClassLoader(
      arrayOf(jar.toURI().toURL()),
      this::class.java.classLoader,
      sharedPackages = setOf(
        "io.github.architectplatform.api.",
        "org.slf4j.",
        "org.junit.",
      ),
    )
    // org.junit classes should come from parent due to custom shared packages
    val testClass = loader.loadClass("org.junit.jupiter.api.Test")
    assertNotNull(testClass)
    assertNotEquals(loader, testClass.classLoader)
    loader.close()
  }

  @Test
  fun `default shared packages include architect API and slf4j`() {
    assertTrue(IsolatedPluginClassLoader.DEFAULT_SHARED_PACKAGES.contains("io.github.architectplatform.api."))
    assertTrue(IsolatedPluginClassLoader.DEFAULT_SHARED_PACKAGES.contains("org.slf4j."))
  }

  // --- Helpers ---

  private fun createEmptyJar(): File {
    val jar = tempDir.resolve("empty-${System.nanoTime()}.jar").toFile()
    JarOutputStream(jar.outputStream()).use { jos ->
      // empty JAR with just manifest
    }
    return jar
  }

  private fun createJarWithClass(className: String, marker: String): File {
    val jar = tempDir.resolve("plugin-$marker-${System.nanoTime()}.jar").toFile()
    val classPath = className.replace('.', '/') + ".class"

    // Generate minimal valid class bytecode
    val bytecode = generateMinimalClass(className)

    JarOutputStream(jar.outputStream()).use { jos ->
      jos.putNextEntry(JarEntry(classPath))
      jos.write(bytecode)
      jos.closeEntry()
    }
    return jar
  }

  private fun createJarWithResource(name: String, content: String): File {
    val jar = tempDir.resolve("resource-${System.nanoTime()}.jar").toFile()
    JarOutputStream(jar.outputStream()).use { jos ->
      jos.putNextEntry(JarEntry(name))
      jos.write(content.toByteArray())
      jos.closeEntry()
    }
    return jar
  }

  /**
   * Generate minimal JVM bytecode for a class with the given fully qualified name.
   * Produces a valid .class file for an empty public class extending Object.
   */
  private fun generateMinimalClass(fqn: String): ByteArray {
    val internalName = fqn.replace('.', '/')

    val pool = mutableListOf<ByteArray>()

    // Helper to add constant pool entries
    fun addUtf8(s: String): Int {
      val bytes = s.toByteArray(Charsets.UTF_8)
      val entry = ByteArray(3 + bytes.size)
      entry[0] = 1 // CONSTANT_Utf8
      entry[1] = (bytes.size shr 8).toByte()
      entry[2] = (bytes.size and 0xFF).toByte()
      System.arraycopy(bytes, 0, entry, 3, bytes.size)
      pool.add(entry)
      return pool.size
    }

    fun addClassRef(nameIndex: Int): Int {
      pool.add(byteArrayOf(7, (nameIndex shr 8).toByte(), (nameIndex and 0xFF).toByte()))
      return pool.size
    }

    // Constant pool entries
    val thisNameIdx = addUtf8(internalName) // #1
    val thisClassIdx = addClassRef(thisNameIdx) // #2
    val superNameIdx = addUtf8("java/lang/Object") // #3
    val superClassIdx = addClassRef(superNameIdx) // #4

    // Build the class file
    val out = java.io.ByteArrayOutputStream()
    fun u2(v: Int) {
      out.write(v shr 8)
      out.write(v and 0xFF)
    }
    fun u4(v: Int) {
      out.write(v shr 24)
      out.write((v shr 16) and 0xFF)
      out.write((v shr 8) and 0xFF)
      out.write(v and 0xFF)
    }

    u4(0xCAFEBABE.toInt()) // magic
    u2(0) // minor version
    u2(52) // major version (Java 8)
    u2(pool.size + 1) // constant_pool_count
    pool.forEach { out.write(it) }
    u2(0x0021) // access_flags: ACC_PUBLIC | ACC_SUPER
    u2(thisClassIdx) // this_class
    u2(superClassIdx) // super_class
    u2(0) // interfaces_count
    u2(0) // fields_count
    u2(0) // methods_count
    u2(0) // attributes_count

    return out.toByteArray()
  }
}
