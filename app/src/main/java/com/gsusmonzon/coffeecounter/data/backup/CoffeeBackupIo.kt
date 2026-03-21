package com.gsusmonzon.coffeecounter.data.backup

import android.content.Context
import android.net.Uri
import java.io.IOException

interface CoffeeBackupIo {
    @Throws(IOException::class)
    fun readText(uri: Uri): String

    @Throws(IOException::class)
    fun writeText(
        uri: Uri,
        value: String,
    )
}

class ContentResolverCoffeeBackupIo(
    context: Context,
) : CoffeeBackupIo {
    private val contentResolver = context.applicationContext.contentResolver

    override fun readText(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for backup import")

        return inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
    }

    override fun writeText(
        uri: Uri,
        value: String,
    ) {
        val outputStream = contentResolver.openOutputStream(uri)
            ?: throw IOException("Unable to open output stream for backup export")

        outputStream.bufferedWriter().use { writer ->
            writer.write(value)
        }
    }
}
