package com.homecinema.core.storage

class FakeFileIO : FileIO {
    val files = mutableMapOf<String, String>()

    override fun read(path: String): String? = files[path]

    override fun write(path: String, text: String) {
        files[path] = text
    }

    override fun writeBytes(path: String, bytes: ByteArray) {
        files[path] = bytes.decodeToString()
    }

    override fun exists(path: String): Boolean = files.containsKey(path)

    override fun delete(path: String): Boolean = files.remove(path) != null

    override fun list(dir: String): List<String> {
        val prefix = dir.trimEnd('/') + "/"
        return files.keys.filter { it.startsWith(prefix) && !it.removePrefix(prefix).contains('/') }
    }
}
