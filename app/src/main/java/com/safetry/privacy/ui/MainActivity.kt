package com.safetry.privacy.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.safetry.privacy.databinding.ActivityMainBinding
import com.safetry.privacy.utils.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { launchProcessing(it) } }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) pickImageLauncher.launch("image/*")
        else Toast.makeText(this, "Storage permission required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferencesManager(this)
        setupButtons()
        loadPreferences()
    }

    private fun setupButtons() {
        binding.btnScanFile.setOnClickListener { checkPermissionsAndPickImage() }
        binding.btnShareProtection.setOnClickListener { showShareProtectionInfo() }
        binding.switchBlurFaces.setOnCheckedChangeListener { _, v ->
            lifecycleScope.launch { prefs.setBlurFaces(v) }
        }
        binding.switchLicensePlates.setOnCheckedChangeListener { _, v ->
            lifecycleScope.launch { prefs.setBlurLicensePlates(v) }
        }
        binding.switchStreetSigns.setOnCheckedChangeListener { _, v ->
            lifecycleScope.launch { prefs.setBlurStreetSigns(v) }
        }
        binding.switchIdBadges.setOnCheckedChangeListener { _, v ->
            lifecycleScope.launch { prefs.setBlurIdBadges(v) }
        }
        binding.switchTextDocs.setOnCheckedChangeListener { _, v ->
            lifecycleScope.launch { prefs.setBlurTextDocs(v) }
        }
        binding.switchAutoRemoveMetadata.setOnCheckedChangeListener { _, v ->
            lifecycleScope.launch { prefs.setAutoRemoveMetadata(v) }
        }
        binding.btnGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/harriocute")))
        }
        binding.btnLinkedin.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/harricode")))
        }
    }

    private fun loadPreferences() {
        lifecycleScope.launch { prefs.getBlurFaces().collect { binding.switchBlurFaces.isChecked = it } }
        lifecycleScope.launch { prefs.getBlurLicensePlates().collect { binding.switchLicensePlates.isChecked = it } }
        lifecycleScope.launch { prefs.getBlurStreetSigns().collect { binding.switchStreetSigns.isChecked = it } }
        lifecycleScope.launch { prefs.getBlurIdBadges().collect { binding.switchIdBadges.isChecked = it } }
        lifecycleScope.launch { prefs.getBlurTextDocs().collect { binding.switchTextDocs.isChecked = it } }
        lifecycleScope.launch { prefs.getAutoRemoveMetadata().collect { binding.switchAutoRemoveMetadata.isChecked = it } }
    }

    private fun checkPermissionsAndPickImage() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED })
            pickImageLauncher.launch("image/*")
        else permissionLauncher.launch(permissions)
    }

    private fun launchProcessing(uri: Uri) {
        startActivity(Intent(this, ProcessingActivity::class.java).apply {
            putExtra(ProcessingActivity.EXTRA_IMAGE_URI, uri.toString())
            putExtra(ProcessingActivity.EXTRA_SOURCE, ProcessingActivity.SOURCE_MANUAL)
        })
    }

    private fun showShareProtectionInfo() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Share Protection Active")
            .setMessage("Open Gallery → Share any photo → Choose AI Metadata Remover Pro → Review report → Share Clean File\n\nAll processing is 100% ON-DEVICE.")
            .setPositiveButton("Got it!") { d, _ -> d.dismiss() }
            .show()
    }
}
