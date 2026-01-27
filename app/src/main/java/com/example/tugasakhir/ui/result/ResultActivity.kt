package com.example.tugasakhir.ui.result

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tugasakhir.R
import com.example.tugasakhir.databinding.ActivityResultBinding
import com.example.tugasakhir.helper.ImageClassifierHelper
import org.json.JSONObject
import java.io.InputStream
import java.util.Locale


class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var recommendationsJson: JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load recommendations.json from assets (expected structure: { "ripe": { "title": "...", "description": "...", "recommendation": "..." }, ... })
        loadRecommendations()

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString == null) {
            Log.e(TAG, "No image URI provided")
            Toast.makeText(this, getString(R.string.empty_image_warning), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
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
                        binding.textCategory.text = error
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
            val resultText = binding.textCategory.text.toString()
            if (resultText.isBlank()) {
                showToast(getString(R.string.invalid_result))
                return@setOnClickListener
            }
            // Save image + result
//            saveHistoryFromIntentUri(imageUri, resultText)
        }
    }

    // ---------- UI helpers ----------
    private fun showImage(uri: Uri) {
        binding.resultImage.setImageURI(uri)
    }

    // Handle results in format List<Pair<label,score>>
    private fun handleResultsSimple(results: List<Pair<String, Float>>, inferenceTime: Long) {
        if (results.isEmpty()) {
            binding.textRecomendationDescription.text = getString(R.string.no_banana_ripeness_detected)
            binding.textRecomendationDescription.text = getString(R.string.recommendation_not_found)
            return
        }

        // Take top-1
        val top = results[0]
        val labelRaw = top.first.trim()
        val score = top.second

        // Display label & confidence & inference time
        val displayLabel = labelRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
//        binding.textCategory.text = getString(R.string.label_detected, displayLabel)
        binding.textAccuracy.text = getString(R.string.confidence_label, score * 100)
//        binding.inferenceInfo.text = getString(R.string.inference_time, inferenceTime)

        // Show recommendation from JSON
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
            // Expecting each key maps to object { "title": "...", "description": "...", "recommendation": "..." }
            val key = label.lowercase(Locale.getDefault())
            if (!recommendationsJson.has(key)) {
                binding.textRecomendationDescription.text = getString(R.string.recommendation_not_found)
                return
            }
            val obj = recommendationsJson.getJSONObject(key)
            // Prefer title + recommendation combined; adapt to your layout
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: key.replaceFirstChar { it.uppercaseChar() }
            val desc = obj.optString("description").takeIf { it.isNotBlank() } ?: ""
            val reco = obj.optString("recommendation").takeIf { it.isNotBlank() } ?: ""

            // Compose display text
            val builder = StringBuilder()
            builder.append(title)
//            builder.append("\n\n").append(binding.textCategory.text).append(":").append(title)
            if (desc.isNotBlank()) {
                builder.append("\n\n").append(getString(R.string.description)).append(":\n").append(desc)
//                builder.append("\n\n").append(desc)
            }
            if (reco.isNotBlank()) {
                builder.append("\n\n").append(getString(R.string.recommendation_title)).append(":\n").append(reco)
            }

            binding.textRecomendationDescription.text = builder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing recommendation for '$label': ${e.message}")
            binding.textRecomendationDescription.text = getString(R.string.recommendation_not_found)
        }
    }



    // ---------- Persist history ----------
//    private fun saveHistoryFromIntentUri(sourceUri: Uri, resultText: String) {
//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                try {
//                    val fileName = "classified_image_${System.currentTimeMillis()}.jpg"
//                    val dest = File(cacheDir, fileName)
//                    contentResolver.openInputStream(sourceUri)?.use { input ->
//                        FileOutputStream(dest).use { out -> input.copyTo(out) }
//                    }
//                    val destinationUri = Uri.fromFile(dest)
//                    val history = History(imagePath = destinationUri.toString(), result = resultText)
//
//                    val db = HistoryDatabase.getDatabase(applicationContext)
//                    db.historyDao().insertHistory(history)
//                    Log.d(TAG, "History saved: $history")
//
//                    withContext(Dispatchers.Main) {
//                        // navigate to HistoryActivity or finish
//                        moveToHistory(destinationUri, resultText)
//                    }
//                } catch (e: Exception) {
//                    Log.e(TAG, "Failed to save history: ${e.message}", e)
//                    withContext(Dispatchers.Main) {
//                        showToast(getString(R.string.data_save_failed))
//                    }
//                }
//            }
//        }
//    }

//    private fun moveToHistory(imageUri: Uri, result: String) {
//        val intent = Intent(this, HistoryActivity::class.java) // pastikan HistoryActivity ada dan terdaftar
//        intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString())
//        intent.putExtra(EXTRA_RESULT, result)
//        startActivity(intent)
//        finish()
//    }

    // ---------- utils ----------
    private fun Float.formatToPercentString(): String = String.format(Locale.getDefault(), "%.2f%%", this * 100)
    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
        const val TAG = "ResultActivity"
    }
}