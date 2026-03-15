package com.homecinema.core.usecase

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val currentAttempt: Int = 0
) {
    val canRetry: Boolean get() = currentAttempt < maxAttempts
    fun nextAttempt() = copy(currentAttempt = currentAttempt + 1)
}
