package com.example.tugasakhir.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.example.tugasakhir.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifierHelper(
    private val context: Context,
    private val modelFileName: String = "best_banana_ripeness_model.tflite",
    private val labelsFileName: String = "best_banana_ripeness_labels.txt",
    private val inputSize: Int = 224, // sesuaikan dengan model
    private val numThreads: Int = 4,
    private val maxResults: Int = 4,
    private val classifierListener: ClassifierListener?
) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val BANANA_CONFIDENCE_THRESHOLD = 0.5f


    init {
        try {
            val model = loadModelFile(context, modelFileName)
            val options = Interpreter.Options().apply { setNumThreads(numThreads) }
            interpreter = Interpreter(model, options)
            labels = loadLabels(context, labelsFileName)
            Log.d(TAG, "✅ Interpreter initialized (${labels.size} labels)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize model: ${e.message}", e)
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
        }
    }

    /** Jalankan klasifikasi pada gambar */
    fun classifyImage(imageUri: Uri) {
        if (interpreter == null) {
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val bitmap = getBitmap(imageUri)
                val input = preprocessBitmap(bitmap)
                val output = Array(1) { FloatArray(labels.size) }

                val startTime = SystemClock.uptimeMillis()
                interpreter?.run(input, output)
                val inferenceTime = SystemClock.uptimeMillis() - startTime

                val results = output[0]
                    .mapIndexed { index, score -> labels.getOrElse(index) { "Unknown" } to score }
                    .sortedByDescending { it.second }
                    .take(maxResults)

                CoroutineScope(Dispatchers.Main).launch {
                    if (results.isEmpty()) {
                        classifierListener?.onError(context.getString(R.string.no_banana_ripeness_detected))
                    } else {
                        classifierListener?.onResultsSimple(results, inferenceTime)
                    }
                }

                // Logika untuk cek apakah gambar pisang atau bukan
//                CoroutineScope(Dispatchers.Main).launch {
//                    val maxConfidence = results.maxOfOrNull { it.second } ?: 0f
//
//                    if (maxConfidence < BANANA_CONFIDENCE_THRESHOLD) {
//                        classifierListener?.onError("Gambar tidak sesuai")
//                        return@launch
//                    }
//
//                    classifierListener?.onResultsSimple(results, inferenceTime)
//                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error analyzing image: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    classifierListener?.onError(context.getString(R.string.image_analysis_error))
                }
            }
        }
    }

    /** Konversi bitmap ke ByteBuffer (float32 + normalisasi 0–1) */
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        var pixelIndex = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = pixels[pixelIndex++]
                buffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                buffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                buffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        buffer.rewind()
        return buffer
    }

    /** Ambil bitmap dari URI */
    private fun getBitmap(imageUri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(src)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }.copy(Bitmap.Config.ARGB_8888, true)
    }

    /** Load model dari assets */
    @Throws(IOException::class)
    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val channel = input.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    /** Load labels dari assets */
    private fun loadLabels(context: Context, fileName: String): List<String> {
        return try {
            context.assets.open(fileName)
                .bufferedReader()
                .useLines { it.filter { line -> line.isNotBlank() }.map { it.trim() }.toList() }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to load labels: ${e.message}")
            emptyList()
        }
    }

    interface ClassifierListener {
        fun onError(error: String)

        /** Callback hasil klasifikasi: Pair(label, skor) */
        fun onResultsSimple(results: List<Pair<String, Float>>, inferenceTime: Long)
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}