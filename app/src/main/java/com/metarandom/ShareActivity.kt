package com.metarandom

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ShareActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        statusText = findViewById(R.id.statusText)

        val uris = extractUris(intent)

        if (uris.isEmpty()) {
            statusText.text = getString(R.string.instructions)
            return
        }

        lifecycleScope.launch {
            val processed = processAll(uris)
            if (processed.isNotEmpty()) {
                reshare(processed)
            } else {
                Toast.makeText(
                    this@ShareActivity,
                    getString(R.string.error_processing),
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun extractUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            listOfNotNull(uri)
        }
        Intent.ACTION_SEND_MULTIPLE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
            } else {
                @Suppress("UNCHECKED_CAST")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
        }
        else -> emptyList()
    }

    private suspend fun processAll(uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        uris.forEachIndexed { index, uri ->
            withContext(Dispatchers.Main) {
                statusText.text = if (uris.size == 1)
                    getString(R.string.processing_single)
                else
                    getString(R.string.processing_multiple, index + 1, uris.size)
            }
            runCatching {
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val file = if (mime.startsWith("video/"))
                    VideoProcessor.process(this@ShareActivity, uri)
                else
                    MetadataProcessor.process(this@ShareActivity, uri)
                results.add(file)
            }.onFailure { it.printStackTrace() }
        }
        results
    }

    private suspend fun reshare(files: List<File>) {
        // FileProvider lookup on IO thread — avoid StrictMode violations
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
