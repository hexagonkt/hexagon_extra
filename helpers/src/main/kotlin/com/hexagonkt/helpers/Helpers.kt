package com.hexagonkt.helpers

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.*
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Print receiver to stdout. Convenient utility to debug variables quickly.
 *
 * @receiver Reference to the object to print. Can be `null`.
 * @param prefix String to print before the actual object information. Empty string by default.
 * @return Receiver's reference. Returned to allow method call chaining.
 */
fun <T> T.out(prefix: String = ""): T =
    apply { println("$prefix$this") }

/**
 * Print receiver to stderr. Convenient utility to debug variables quickly.
 *
 * @receiver Reference to the object to print. Can be `null`.
 * @param prefix String to print before the actual object information. Empty string by default.
 * @return Receiver's reference. Returned to allow method call chaining.
 */
fun <T> T.err(prefix: String = ""): T =
    apply { System.err.println("$prefix$this") }

/**
 * Load a '*.properties' file from a URL transforming the content into a plain map. If the resource
 * can not be found, a [ResourceNotFoundException] is thrown.
 *
 * @param url URL pointing to the file to load.
 * @return Map containing the properties file data.
 */
fun properties(url: URL): Map<String, String> =
    Properties()
        .apply { url.openStream().use { load(it.reader()) } }
        .toMap()
        .mapKeys { it.key as String }
        .mapValues { it.value as String }

// PROCESSES ///////////////////////////////////////////////////////////////////////////////////////
/**
 * Execute a lambda until no exception is thrown or a number of times is reached.
 *
 * @param times Number of times to try to execute the callback. Must be greater than 0.
 * @param delay Milliseconds to wait to next execution if there was an error. Must be 0 or greater.
 * @param block Code to be executed.
 * @return Callback's result if succeeded.
 * @throws [MultipleException] if the callback didn't succeed in the given times.
 */
fun <T> retry(times: Int, delay: Long, block: () -> T): T {
    require(times > 0)
    require(delay >= 0)

    val exceptions = mutableListOf<Exception>()
    for (ii in 1 .. times) {
        try {
            return block()
        }
        catch (e: Exception) {
            exceptions.add(e)
            Thread.sleep(delay)
        }
    }

    throw MultipleException("Error retrying $times times ($delay ms)", exceptions)
}

/**
 * [TODO](https://github.com/hexagonkt/hexagon/issues/271).
 *
 * TODO Assure JVM closes properly after process execution (dispose process resources, etc.)
 */
fun List<String>.exec(
    workingDirectory: File = File(System.getProperty("user.dir")),
    timeout: Long = Long.MAX_VALUE,
    fail: Boolean = false,
): String {

    val command = filter { it.isNotBlank() }.toTypedArray()

    require(command.isNotEmpty()) { "Command is empty" }
    require(timeout > 0) { "Process timeout should be greater than zero: $timeout" }

    val process = ProcessBuilder(*command).directory(workingDirectory).start()

    if (!process.waitFor(timeout, SECONDS)) {
        process.destroy()
        error("Command timed out: $this")
    }

    val exitValue = process.exitValue()
    val output = BufferedReader(InputStreamReader(process.inputStream)).readText()

    if (fail && exitValue != 0)
        throw CodedException(exitValue, output)

    return output
}

/**
 * TODO Add use case and example in documentation.
 * TODO Support multiple words parameters by processing " and '
 *
 * Run the receiver's text as a process in the host operating system. The command can have multiple
 * lines and may or may not contain the shell continuation string (` \\n`).
 *
 * @receiver String holding the command to be executed.
 * @param workingDirectory Directory on which the process will be executed. Defaults to current
 *  directory.
 * @param timeout Maximum number of seconds allowed for process execution. Defaults to the maximum
 *  long value. It must be greater than zero.
 * @param fail If true Raise an exception if the result code is different from zero. The default
 *  value is `false`.
 * @throws CodedException Thrown if the process return an error code (the actual code is passed
 *  inside [CodedException.code] and the command output is set at [CodedException.message]).
 * @throws IllegalStateException If the command doesn't end within the allowed time or the command
 *  string is blank, an exception will be thrown.
 * @return The output of the command.
 */
fun String.exec(
    workingDirectory: File = File(System.getProperty("user.dir")),
    timeout: Long = Long.MAX_VALUE,
    fail: Boolean = false,
): String =
    replace("""(\s+\\\s*)?\n""".toRegex(), "")
        .split(" ")
        .map { it.trim() }
        .toList()
        .exec(workingDirectory, timeout, fail)

// ERROR HANDLING //////////////////////////////////////////////////////////////////////////////////
/**
 * [TODO](https://github.com/hexagonkt/hexagon/issues/271).
 */
fun check(message: String = "Multiple exceptions", vararg blocks: () -> Unit) {
    val exceptions: List<Exception> = blocks.mapNotNull {
        try {
            it()
            null
        }
        catch(e: Exception) {
            e
        }
    }

    if (exceptions.isNotEmpty())
        throw MultipleException(message, exceptions)
}
