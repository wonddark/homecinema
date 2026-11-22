package com.homecinema.core.storage

class FakeFileIO : FileIO {
    val files = mutableMapOf<String, String>()

    override fun read(path: String): String? = files[path]

    override fun write(path: String, text: String) {
        files[path] = text
    }

    override fun exists(path: String): Boolean = files.containsKey(path)

    override fun delete(path: String): Boolean = files.remove(path) != null
}
