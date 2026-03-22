package io.github.architectplatform.engine.core.common

/**
 * A type that represents either success or failure.
 * 
 * This sealed class provides a type-safe way to handle operations that can fail,
 * avoiding the use of exceptions for control flow and providing better error handling.
 * 
 * @param T The type of the success value
 */
sealed class Result<out T> {
    
    /**
     * Represents a successful operation with a value.
     */
    data class Success<T>(val value: T) : Result<T>()
    
    /**
     * Represents a failed operation with error details.
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null,
        val errorCode: String? = null
    ) : Result<Nothing>()
    
    /**
     * Returns true if the result is a success.
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Returns true if the result is a failure.
     */
    fun isFailure(): Boolean = this is Failure
    
    /**
     * Returns the value if this is a success, or null if it's a failure.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
    
    /**
     * Returns the value if this is a success, or throws the error if it's a failure.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw IllegalStateException(message, cause)
    }
    
    /**
     * Returns the value if this is a success, or the default value if it's a failure.
     */
    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> default
    }
    
    /**
     * Maps the success value using the provided transformation function.
     */
    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
    
    /**
     * Flat maps the success value using the provided transformation function.
     */
    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }
    
    /**
     * Executes the provided action if this is a success.
     */
    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(value)
        }
        return this
    }
    
    /**
     * Executes the provided action if this is a failure.
     */
    fun onFailure(action: (Failure) -> Unit): Result<T> {
        if (this is Failure) {
            action(this)
        }
        return this
    }
    
    /**
     * Folds the result by applying the appropriate function based on success or failure.
     */
    fun <R> fold(onSuccess: (T) -> R, onFailure: (Failure) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(this)
    }
    
    companion object {
        /**
         * Creates a success result with the given value.
         */
        fun <T> success(value: T): Result<T> = Success(value)
        
        /**
         * Creates a failure result with the given message.
         */
        fun <T> failure(
            message: String,
            cause: Throwable? = null,
            errorCode: String? = null
        ): Result<T> = Failure(message, cause, errorCode)
        
        /**
         * Wraps a potentially throwing operation in a Result.
         */
        fun <T> catching(operation: () -> T): Result<T> = try {
            Success(operation())
        } catch (e: Exception) {
            Failure(e.message ?: "Operation failed", e)
        }
    }
}
