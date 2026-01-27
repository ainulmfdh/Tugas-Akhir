package com.example.tugasakhir.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.tugasakhir.databinding.ActivityCameraBinding
import com.example.tugasakhir.ui.home.HomeFragment
import java.io.File

class CameraActivity : AppCompatActivity() {

    //  Variabel Camera binding
    private lateinit var binding: ActivityCameraBinding

    //  Variabel pilih default kamera belakang
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    //  Variabel simpan
    var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memanggil fungsi hide system
        hideSystemUI()

        // Memanggil fungsi kamera
        startCamera()

        //  Fungsi untuk merubah kamera depan/belakang
        binding.switchCamera.setOnClickListener {
            cameraSelector =
                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else
                    CameraSelector.DEFAULT_BACK_CAMERA

            startCamera()
        }

        //  Fungsi untuk Mengambil gambar menggunakan Kamera
        binding.captureImage.setOnClickListener {
            takePhoto()
        }
    }

    //  Fungsi untuk Menjalankan fungsi CameraX
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.viewFinder.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "startCamera error", exc)
                Toast.makeText(
                    this,
                    "Gagal memunculkan kamera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    //  Fungsi untuk Mengambil Gambar menggunakan CameraX
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile: File = createCustomTempFile(applicationContext)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = photoFile.toURI().toString()

                    //  Mengarahkan Variable Image ke Home Fragment
                    val intent = Intent().apply {
                        putExtra(HomeFragment.EXTRA_CAMERA_IMAGE_URI, uri)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }

                //  Fungsi untuk Menampilkan pesan Error
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil gambar", exc)
                    Toast.makeText(
                        this@CameraActivity,
                        "Kembali ke Beranda",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    //  Fungsi untuk membuat Kamera menjadi Fullscreen
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    //  Fungsi untuk Mengatur Orientasi Gambar
    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
            }
        }
    }

    // Menjalankan orientasi gambar
    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    // Menghentikan orientasi gambar
    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    companion object {
        private const val TAG = "CameraActivity"
//        const val EXTRA_CAMERAX_IMAGE = "CameraX Image"
//        const val CAMERAX_RESULT = 200
    }
}