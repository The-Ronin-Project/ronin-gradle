package com.projectronin.gradle.test

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream

fun File.getArchiveEntries(gzipped: Boolean = true): List<String> {
    val stream = if (gzipped) {
        GzipCompressorInputStream(FileInputStream(this))
    } else {
        FileInputStream(this)
    }
    return stream.use { input ->
        val entries = mutableListOf<String>()
        TarArchiveInputStream(input).use { tar ->
            var nextEntry: TarArchiveEntry? = tar.nextTarEntry
            while (nextEntry != null) {
                entries += nextEntry.name
                nextEntry = tar.nextTarEntry
            }
        }
        entries
    }
}
