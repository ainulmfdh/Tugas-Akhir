package com.example.tugasakhir.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.tugasakhir.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAXIMAL_SIZE = 10000000 // 10 MB
private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
private val timeStamp: String =
    SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())

fun getImageUri(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
        }
        context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    } else {
        getImageUriForPreQ(context)
    }
}

private fun getImageUriForPreQ(context: Context): Uri {
    val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File(filesDir, "MyCamera/$timeStamp.jpg")
    imageFile.parentFile?.mkdirs()

    return FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        imageFile
    )
}

fun createCustomTempFile(context: Context): File {
    val filesDir = context.externalCacheDir
    return File.createTempFile(timeStamp, ".jpg", filesDir)
}

fun uriToFile(imageUri: Uri, context: Context): File {
    val file = createCustomTempFile(context)
    context.contentResolver.openInputStream(imageUri).use { input ->
        FileOutputStream(file).use { output ->
            input?.copyTo(output)
        }
    }
    return file
}

fun File.reduceFileImage(): File {
    val bitmap = BitmapFactory.decodeFile(path).getRotatedBitmap(this)
    var compressQuality = 100
    var streamLength: Int

    do {
        val bmpStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
        streamLength = bmpStream.size()
        compressQuality -= 5
    } while (streamLength > MAXIMAL_SIZE)

    FileOutputStream(this).use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, it)
    }
    return this
}

// Mengatur Rotasi Gambar Bitmap
fun Bitmap.getRotatedBitmap(file: File): Bitmap {
    val exif = ExifInterface(file)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(270f)
        else -> this
    }
}

// Menjalankan Rotasi Gambar Bitmap
fun Bitmap.rotateImage(angle: Float): Bitmap =
    Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply {
        postRotate(angle)
    }, true)
