package com.example.tugasakhir.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.tugasakhir.R
import com.example.tugasakhir.databinding.FragmentHomeBinding
import com.example.tugasakhir.ui.CameraActivity
import com.example.tugasakhir.ui.result.ResultActivity

class HomeFragment : Fragment() {

    //  Variabel Fragment Home binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // URI gambar dari kamera / galeri
    private var currentImageUri: Uri? = null

    // Membuka galeri ponsel pengguna
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            currentImageUri = uri
            showImage(uri)
        } else {
            Log.d(TAG, "Galeri: tidak ada gambar yang dipilih")
        }
    }

    // Menyalakan Kamera ponsel pengguna
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra(EXTRA_CAMERA_IMAGE_URI)
            uriString?.let {
                currentImageUri = it.toUri()
                showImage(currentImageUri!!)
            } ?: showToast("Gagal mengambil gambar dari kamera")
        }
    }

    // IZIN akses Kamera pengguna
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showToast("Izin kamera ditolak")
        }
    }

    // Fungsi View utama
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Memanggil Button Kamera
        binding.btnCamera.setOnClickListener {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // Memanggil dan Menjalankan Button Galeri
        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        // Menjalankan Button Prediksi
        binding.btnPredict.setOnClickListener {
            currentImageUri?.let { uri ->
                navigateToResult(uri)
            } ?: showToast(getString(R.string.empty_image_warning))
        }

        return binding.root
    }

    // Menyimpan data preview gambar orientasi layar
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentImageUri?.let {
            outState.putString("image_uri", it.toString())
        }
    }

    // Mengembalikan data preview gambar orientasi layar
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getString("image_uri")?.let {
            currentImageUri = Uri.parse(it)
            binding.previewImageView.setImageURI(currentImageUri)
        }
    }

    //  Fungsi untuk Mengarahkan ke Camera Activity
    private fun openCamera() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    //  Fungsi untuk Membuka Galeri ponsel pengguna
    private fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    //  Fungsi untuk Mengarahkan ke Result Activity
    private fun navigateToResult(uri: Uri) {
        Log.d(TAG, "Pindah ke halaman hasil prediksi: $uri")
        val intent = Intent(requireActivity(), ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
        }
        startActivity(intent)
    }

    //  Fungsi untuk Menampilkan preview Gambar
    private fun showImage(uri: Uri) {
        binding.previewImageView.setImageURI(uri)
    }

    //  Fungsi untuk Menampilkan text alert
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //  Object Home Fragment & Camera
    companion object {
        private const val TAG = "HomeFragment"
        const val EXTRA_CAMERA_IMAGE_URI = "CAMERA_IMAGE_URI"
    }
}
