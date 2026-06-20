package com.metarandom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
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
            // Opened from launcher: show instructions and stay on screen
            statusText.text = getString(R.string.instructions)
            return
        }

        lifecycleScope.launch {
            val processed = processAll(uris)
            if (processed.isNotEmpty()) reshare(processed)
            finish()
        }
    }

    private fun extractUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM))
        Intent.ACTION_SEND_MULTIPLE ->
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
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

    private fun reshare(files: List<File>) {
        val contentUris = files.map { file ->
            FileProvider.getUriForFile(this, "com.metarandom.fileprovider", file)
        }

        val allVideos = files.all { it.extension == "mp4" }
        val mimeType  = if (allVideos) "video/mp4" else "image/jpeg"

        val shareIntent = if (contentUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (allVideos) "video/*" else "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(contentUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
}
