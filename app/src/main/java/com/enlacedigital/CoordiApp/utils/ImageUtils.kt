package com.enlacedigital.CoordiApp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Base64
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprime una imagen a un tamaño máximo especificado.
 *
 * @param imageBytes Bytes de la imagen original.
 * @param maxSize Tamaño máximo en bytes al que se desea comprimir la imagen (por defecto 1.2 MB).
 * @param initialQuality Calidad inicial para la compresión JPEG (por defecto 80).
 * @return Bytes de la imagen comprimida.
 */
fun compressImageToTargetSize(imageBytes: ByteArray, maxSize: Int = 1200000, initialQuality: Int = 80): ByteArray {
    var quality = initialQuality
    var compressedBytes: ByteArray
    do {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        compressedBytes = outputStream.toByteArray()

        quality -= 5 // Reduce la calidad en cada iteración
    } while (compressedBytes.size > maxSize && quality > 5) // Continúa hasta que cumpla el tamaño o la calidad sea muy baja

    return compressedBytes
}

/**
 * Convierte una imagen en base64.
 *
 * @param file Archivo de imagen a codificar.
 * @return Cadena de texto en formato Base64.
 */
fun encodeImageToBase64(file: File): String {
    val base64 = try {
        val bytes = file.readBytes() // Lee los bytes del archivo
        val compressedBytes = compressImageToTargetSize(bytes) // Comprime los bytes
        "data:image/jpg;base64," + Base64.encodeToString(compressedBytes, Base64.NO_WRAP) // Codifica en Base64
    } catch (e: IOException) {
        e.printStackTrace()
        ""
    }
    return base64
}

/**
 * Muestra un cuadro de diálogo con opciones para tomar una foto o elegir una de la galería.
 *
 * @param context Contexto de la aplicación.
 * @param takePhotoAction Acción a realizar al seleccionar "Tomar foto".
 * @param chooseFromGalleryAction Acción a realizar al seleccionar "Elegir desde la galería".
 */
fun showPhotoOptions(
    context: Context,
    takePhotoAction: () -> Unit,
    chooseFromGalleryAction: () -> Unit
) {
    val options = arrayOf("Tomar foto", "Elegir desde la galería") // Opciones para el usuario
    android.app.AlertDialog.Builder(context)
        .setTitle("Elegir opción")
        .setItems(options) { _, which ->
            when (which) {
                0 -> takePhotoAction() // Ejecuta la acción para tomar foto
                1 -> chooseFromGalleryAction() // Ejecuta la acción para elegir de la galería
            }
        }
        .show()
}

/**
 * Crea un archivo temporal para almacenar una imagen.
 *
 * @param context Contexto de la aplicación.
 * @return Par con el archivo creado y su ruta absoluta.
 * @throws IOException Si ocurre un error al crear el archivo.
 */
@Throws(IOException::class)
fun createImageFile(context: Context): Pair<File, String> {
    val timeStamp = SimpleDateFormat.getDateTimeInstance().format(Date()) // Genera una marca de tiempo
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Directorio de almacenamiento
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir) // Crea un archivo temporal
    val currentPhotoPath = file.absolutePath
    return Pair(file, currentPhotoPath)
}

/**
 * Extrae texto de una imagen utilizando ML Kit de Google.
 *
 * @param imageFile Archivo de imagen del cual extraer texto.
 * @param onSuccess Acción a realizar en caso de éxito con el texto extraído.
 * @param onFailure Acción a realizar en caso de fallo con un mensaje de error.
 */
fun extractTextFromImage(
    imageFile: File,
    onSuccess: (String?) -> Unit,
    onFailure: (String) -> Unit
) {
    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) // Decodifica el archivo en un bitmap
    val inputImage = InputImage.fromBitmap(bitmap, 0) // Convierte el bitmap en un InputImage
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) // Inicializa el reconocedor de texto

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text // Extrae el texto reconocido
            onSuccess(extractedText) // Llama a la acción de éxito
            Log.d("extraido", extractedText) // Muestra el texto en los logs
        }
        .addOnFailureListener { e ->
            onFailure("Error al reconocer texto, intenta tomar una mejor foto") // Llama a la acción de fallo
            Log.e("MLKit", "Error al reconocer texto", e) // Muestra el error en los logs
        }
}
