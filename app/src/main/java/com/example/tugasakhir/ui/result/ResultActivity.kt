package com.example.tugasakhir.ui.result

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tugasakhir.R
import com.example.tugasakhir.database.HistoryRepository
import com.example.tugasakhir.databinding.ActivityResultBinding
import com.example.tugasakhir.helper.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var recommendationsJson: JSONObject
    private lateinit var historyRepository: HistoryRepository

    private var currentLabel: String = ""
    private var currentTitle: String = ""
    private var currentDescription: String = ""
    private var currentImageUri: Uri? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load recommendations.json
        loadRecommendations()

        historyRepository = HistoryRepository(this)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString == null) {
            Log.e(TAG, "No image URI provided")
            Toast.makeText(this, getString(R.string.empty_image_warning), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        currentImageUri = imageUri
        showImage(imageUri)

        // Instantiate helper: ensure model + labels are present in assets
        val imageClassifierHelper = ImageClassifierHelper(
            context = this,
            modelFileName = "best_banana_ripeness_model.tflite",        // pastikan ada di assets/
            labelsFileName = "best_banana_ripeness_labels.txt",         // pastikan ada di assets/
            inputSize = 224,
            numThreads = 4,
            maxResults = 4,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    Log.e(TAG, "Classifier error: $error")
                    runOnUiThread {
//                        binding.textCategory.text = error
                        Toast.makeText(this@ResultActivity, error, Toast.LENGTH_SHORT).show()

                    }
                }

                // New signature from Interpreter-based helper
                override fun onResultsSimple(results: List<Pair<String, Float>>, inferenceTime: Long) {
                    runOnUiThread {
                        handleResultsSimple(results, inferenceTime)
                    }

                }
            }
        )

        // Run classification
        imageClassifierHelper.classifyImage(imageUri)

        binding.btnSave.setOnClickListener {
            if (currentLabel.isBlank() || currentImageUri == null) {
                showToast(getString(R.string.invalid_result))
                return@setOnClickListener
            }
            saveHistory(currentImageUri!!)
        }
    }

    // ---------- UI helpers ----------
    private fun showImage(uri: Uri) {
        binding.resultImage.setImageURI(uri)
    }

    // Handle results in format List<Pair<label,score>>
    private fun handleResultsSimple(results: List<Pair<String, Float>>, inferenceTime: Long) {
        if (results.isEmpty()) {
            binding.textRecomendationDescription.text =
                getString(R.string.no_banana_ripeness_detected)
            return
        }

        val top = results[0]
        val labelRaw = top.first.trim()
        val score = top.second

        val displayLabel = labelRaw.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        currentLabel = displayLabel
//        binding.textCategory.text = displayLabel
        binding.textAccuracy.text =
            getString(R.string.confidence_label, score * 100)

        showRecommendationForLabel(labelRaw)
    }


    // ---------- recommendations.json helpers ----------
    private fun loadRecommendations() {
        recommendationsJson = try {
            val input: InputStream = assets.open("recommendations.json")
            val jsonText = input.bufferedReader().use { it.readText() }
            JSONObject(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recommendations.json: ${e.message}")
            JSONObject()
        }
    }

    private fun showRecommendationForLabel(label: String) {
        try {
            val key = label.lowercase(Locale.getDefault())

            if (!recommendationsJson.has(key)) {
                binding.textRecomendationDescription.text =
                    getString(R.string.recommendation_not_found)
                currentTitle = ""
                currentDescription = ""
                return
            }

            val obj = recommendationsJson.getJSONObject(key)

            // ===== DATA =====
            val title = obj.optString("title")
                .takeIf { it.isNotBlank() }
                ?: key.replaceFirstChar { it.uppercaseChar() }

            val description = obj.optString("description").trim()
            val recommendation = obj.optString("recommendation").trim()

            // ===== UI (lengkap) =====
            val displayText = StringBuilder()
                .append(title)

            if (description.isNotBlank()) {
                displayText.append("\n\n")
                    .append(getString(R.string.description))
                    .append(":\n")
                    .append(description)
            }

            if (recommendation.isNotBlank()) {
                displayText.append("\n\n")
                    .append(getString(R.string.recommendation_title))
                    .append(":\n")
                    .append(recommendation)
            }

            binding.textRecomendationDescription.text = displayText.toString()

            // ===== DATABASE (HANYA TITLE & DESCRIPTION) =====
            currentTitle = title
            currentDescription = description

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing recommendation for '$label': ${e.message}")
            binding.textRecomendationDescription.text =
                getString(R.string.recommendation_not_found)
            currentTitle = ""
            currentDescription = ""
        }
    }


    // ---------- Persist history ----------
    private fun saveHistory(sourceUri: Uri) {
        lifecycleScope.launch {
            try {
                val fileName = "history_${System.currentTimeMillis()}.jpg"
                val destFile = File(filesDir, fileName)

                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val createdAt = SimpleDateFormat(
                        "dd-MM-yyyy HH:mm",
                        Locale.getDefault()
                    ).format(Date())

                    historyRepository.insertHistory(
                        imagePath = destFile.absolutePath,
                        title = currentTitle,
                        description = currentDescription,
                        createdAt = createdAt
                    )
                }

                showToast(getString(R.string.data_saved_success))
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Save history failed", e)
                showToast(getString(R.string.data_save_failed))
            }
        }
    }


    // ---------- utils ----------
    private fun Float.formatToPercentString(): String = String.format(Locale.getDefault(), "%.2f%%", this * 100)
    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
        const val TAG = "ResultActivity"
    }
}