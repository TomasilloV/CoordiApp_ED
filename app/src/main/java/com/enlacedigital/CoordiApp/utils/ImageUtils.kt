package com.enlacedigital.CoordiApp.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

fun compressImageToTargetSize(imageBytes: ByteArray, maxSize: Int = 1200000, initialQuality: Int = 80): ByteArray {
    var quality = initialQuality
    var compressedBytes: ByteArray
    do {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        compressedBytes = outputStream.toByteArray()

        quality -= 5
    } while (compressedBytes.size > maxSize && quality > 5)

    return compressedBytes
}

fun encodeImageToBase64(file: File): String {
    val base64 = try {
        val bytes = file.readBytes()
        val compressedBytes = compressImageToTargetSize(bytes)
        "data:image/jpg;base64," + Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
    } catch (e: IOException) {
        e.printStackTrace()
        ""
    }
    return base64
}

fun showPhotoOptions(
    context: Context,
    photoType: String,
    takePhotoAction: () -> Unit,
    chooseFromGalleryAction: () -> Unit
) {
    val options = arrayOf("Tomar foto", "Elegir desde la galería")
    android.app.AlertDialog.Builder(context)
        .setTitle("Elegir opción")
        .setItems(options) { _, which ->
            when (which) {
                0 -> takePhotoAction()
                1 -> chooseFromGalleryAction()
            }
        }
        .show()
}

@Throws(IOException::class)
fun createImageFile(context: Context): Pair<File, String> {
    val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    val currentPhotoPath = file.absolutePath
    return Pair(file, currentPhotoPath)
}