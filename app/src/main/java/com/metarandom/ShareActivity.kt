package com.metarandom

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ShareActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var confirmationBox: LinearLayout
    private lateinit var confirmationText: TextView
    private lateinit var hintText: TextView
    private lateinit var updateBanner: LinearLayout
    private lateinit var updateText: TextView
    private lateinit var updateButton: TextView
    private lateinit var updateBannerLauncher: LinearLayout
    private lateinit var updateTextLauncher: TextView
    private lateinit var updateButtonLauncher: TextView

    companion object {
        const val VERSION_NAME = "1.2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        statusText           = findViewById(R.id.statusText)
        progressBar          = findViewById(R.id.progressBar)
        confirmationBox      = findViewById(R.id.confirmationBox)
        confirmationText     = findViewById(R.id.confirmationText)
        hintText             = findViewById(R.id.hintText)
        updateBanner         = findViewById(R.id.updateBanner)
        updateText           = findViewById(R.id.updateText)
        updateButton         = findViewById(R.id.updateButton)
        updateBannerLauncher = findViewById(R.id.updateBannerLauncher)
        updateTextLauncher   = findViewById(R.id.updateTextLauncher)
        updateButtonLauncher = findViewById(R.id.updateButtonLauncher)

        val uris = extractUris(intent)

        if (uris.isEmpty()) {
            progressBar.visibility = View.GONE
            statusText.text = getString(R.string.instructions)
            checkForUpdates(launcher = true)
            return
        }

        checkForUpdates(launcher = false)

        lifecycleScope.launch {
            val results = processAll(uris)

            if (results.isEmpty()) {
                Toast.makeText(this@ShareActivity, getString(R.string.error_processing), Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility     = View.GONE
                statusText.visibility      = View.GONE
                confirmationBox.visibility = View.VISIBLE
                confirmationText.text = if (results.size == 1)
                    results.first().second
                else
                    results.joinToString("\n") { "• ${it.second}" }
                hintText.text = getString(R.string.hint_telegram)
            }

            delay(3_000L)
            reshare(results.map { it.first })
            finish()
        }
    }

    private fun checkForUpdates(launcher: Boolean) {
        lifecycleScope.launch {
            val info = UpdateChecker.check(VERSION_NAME) ?: return@launch
            withContext(Dispatchers.Main) {
                val label = getString(R.string.update_available, info.version)
                if (launcher) {
                    updateTextLauncher.text = label
                    bindUpdateButton(updateButtonLauncher, info)
                    updateBannerLauncher.visibility = View.VISIBLE
                } else {
                    updateText.text = label
                    bindUpdateButton(updateButton, info)
                    updateBanner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun bindUpdateButton(button: TextView, info: UpdateInfo) {
        if (info.apkUrl != null) {
            // Direct APK available — download and install
            button.setOnClickListener {
                button.text = getString(R.string.update_downloading)
                button.isClickable = false
                lifecycleScope.launch { downloadAndInstall(button, info) }
            }
        } else {
            // No APK asset — open releases page in browser
            button.setOnClickListener { openUrl(info.releaseUrl) }
        }
    }

    private suspend fun downloadAndInstall(button: TextView, info: UpdateInfo) {
        val apkUrl = info.apkUrl ?: return
        try {
            val updateDir = File(cacheDir, "updates").also { it.mkdirs() }
            val apkFile   = File(updateDir, "update.apk")

            withContext(Dispatchers.IO) {
                URL(apkUrl).openStream().use { input ->
                    FileOutputStream(apkFile).use { output -> input.copyTo(output) }
                }
            }

            val apkUri = FileProvider.getUriForFile(this, "com.metarandom.fileprovider", apkFile)
            val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data  = apkUri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
            }
            startActivity(install)

        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                button.text = getString(R.string.update_download)
                button.isClickable = true
                button.setOnClickListener { openUrl(info.releaseUrl) }
                Toast.makeText(this@ShareActivity, getString(R.string.update_download_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    @Suppress("DEPRECATION")
    private fun extractUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            listOfNotNull(uri)
        }
        Intent.ACTION_SEND_MULTIPLE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
            else
                @Suppress("UNCHECKED_CAST")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
        }
        else -> emptyList()
    }

    private suspend fun processAll(uris: List<Uri>): List<Pair<File, String>> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<Pair<File, String>>()
            uris.forEachIndexed { index, uri ->
                withContext(Dispatchers.Main) {
                    statusText.text = if (uris.size == 1)
                        getString(R.string.processing_single)
                    else
                        getString(R.string.processing_multiple, index + 1, uris.size)
                }
                runCatching {
                    val mime = contentResolver.getType(uri) ?: "image/jpeg"
                    val result = if (mime.startsWith("video/"))
                        VideoProcessor.process(this@ShareActivity, uri)
                    else
                        MetadataProcessor.process(this@ShareActivity, uri)
                    results.add(result)
                }.onFailure { it.printStackTrace() }
            }
            results
        }

    private suspend fun reshare(files: List<File>) {
        val contentUris = withContext(Dispatchers.IO) {
            files.map { FileProvider.getUriForFile(this@ShareActivity, "com.metarandom.fileprovider", it) }
        }
        val allVideos = files.all { it.extension == "mp4" }
        val clip = ClipData.newRawUri("", contentUris.first()).also { cd ->
            contentUris.drop(1).forEach { cd.addItem(ClipData.Item(it)) }
        }
        val shareIntent = if (contentUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = if (allVideos) "video/mp4" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUris.first())
                clipData = clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (allVideos) "video/*" else "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(contentUris))
                clipData = clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
}
