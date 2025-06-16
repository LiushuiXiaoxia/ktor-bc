package cn.mycommons.ktor_bc

import java.io.File


class CacheFile(val path: String) {

    companion object {
        const val CACHE_ROOT = "CACHE_ROOT"
    }

    val file: File by lazy {
        File(CACHE_ROOT, path)
    }


    fun check(): Boolean {
        if (path.contains("..")) {
            return false
        }
        if (!path.startsWith("/")) {
            return false
        }
        return true
    }

    fun exists() = file.exists() && file.isFile


    fun save(bytes: ByteArray) {
        file.parentFile.mkdirs()
        file.writeBytes(bytes)
    }

    fun delete() {
        if (exists()) {
            file.delete()
        }
    }
}