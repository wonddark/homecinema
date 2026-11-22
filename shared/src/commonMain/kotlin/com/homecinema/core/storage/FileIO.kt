package com.homecinema.core.storage

interface FileIO {
    fun read(path: String): String?
    fun write(path: String, text: String)
    fun writeBytes(path: String, bytes: ByteArray)
    fun exists(path: String): Boolean
    fun delete(path: String): Boolean

    /** Absolute paths of the direct children of [dir]; empty when [dir] does not exist. */
    fun list(dir: String): List<String>
}
