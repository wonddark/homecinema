package com.homecinema.core

expect fun platformDownloadDir(): String

expect fun revealInFileBrowser(path: String)

expect fun appStorageDir(): String

expect fun platformFileIO(): com.homecinema.core.storage.FileIO
