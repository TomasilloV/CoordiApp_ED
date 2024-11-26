package com.enlacedigital.CoordiApp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import android.content.Context
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun compressImageToTargetSize(
    imageBytes: ByteArray,
    maxSize: Int = 1200000,
    initialQuality: Int = 80
): ByteArray {
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
        val rotationSuccess = rotateImageIfRequiredAndSave(file.absolutePath)
        if (!rotationSuccess) return ""
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

fun extractTextFromImage(
    imageFile: File,
    onSuccess: (String?) -> Unit,
    onFailure: (String) -> Unit
) {
    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            onSuccess(extractedText)
            Log.d("extraido", extractedText)
        }
        .addOnFailureListener { e ->
            onFailure("Error al reconocer texto, intenta tomar una mejor foto")
            Log.e("MLKit", "Error al reconocer texto", e)
        }
}

fun getPhotoOrientation(filePath: String): Int {
    return try {
        val exif = ExifInterface(filePath)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        orientation
    } catch (e: IOException) {
        e.printStackTrace()
        ExifInterface.ORIENTATION_UNDEFINED
    }
}

fun rotateImageIfRequiredAndSave(filePath: String): Boolean {
    try {
        val bitmap = BitmapFactory.decodeFile(filePath) ?: return false
        val orientation = getPhotoOrientation(filePath)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return true
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        FileOutputStream(filePath).use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        rotatedBitmap.recycle()
        bitmap.recycle()
        return true
    } catch (e: Exception) {
        Log.e("RotateImage", "Error al rotar y guardar la imagen.", e)
        return false
    }
}