package com.metarandom

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
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

class ShareActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var confirmationBox: View
    private lateinit var confirmationText: TextView
    private lateinit var hintText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        statusText      = findViewById(R.id.statusText)
        progressBar     = findViewById(R.id.progressBar)
        confirmationBox = findViewById(R.id.confirmationBox)
        confirmationText = findViewById(R.id.confirmationText)
        hintText        = findViewById(R.id.hintText)

        val uris = extractUris(intent)

        if (uris.isEmpty()) {
            // Opened from launcher — show how-to instructions
            progressBar.visibility = View.GONE
            statusText.text = getString(R.string.instructions)
            return
        }

        lifecycleScope.launch {
            val results = processAll(uris)

            if (results.isEmpty()) {
                Toast.makeText(
                    this@ShareActivity,
                    getString(R.string.error_processing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@launch
            }

            // Show confirmation screen with the actual fake values that were injected
            withContext(Dispatchers.Main) {
                progressBar.visibility  = View.GONE
                statusText.visibility   = View.GONE
                confirmationBox.visibility = View.VISIBLE

                val body = if (results.size == 1) {
                    results.first().second   // e.g. "IMG_83920193.jpg · samsung SM-A546B · Paris · 2022-03-14"
                } else {
                    results.joinToString("\n") { "• ${it.second}" }
                }
                confirmationText.text = body
                hintText.text = getString(R.string.hint_telegram)
            }

            // Auto-share after a short pause so the user can read the values
            delay(3_000L)

            reshare(results.map { it.first })
            finish()
        }
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
        // FileProvider lookup on IO thread
        val contentUris = withContext(Dispatchers.IO) {
            files.map { FileProvider.getUriForFile(this@ShareActivity, "com.metarandom.fileprovider", it) }
        }

        val allVideos = files.all { it.extension == "mp4" }
        val mimeType  = if (allVideos) "video/mp4" else "image/jpeg"

        // ClipData is REQUIRED alongside FLAG_GRANT_READ_URI_PERMISSION when using
        // Intent.createChooser: the chooser relays grants only via ClipData, not EXTRA_STREAM.
        val clip = ClipData.newRawUri("", contentUris.first()).also { cd ->
            contentUris.drop(1).forEach { cd.addItem(ClipData.Item(it)) }
        }

        val shareIntent = if (contentUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
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
