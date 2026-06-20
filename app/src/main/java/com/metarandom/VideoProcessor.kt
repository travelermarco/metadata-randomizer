package com.metarandom

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

object VideoProcessor {

    fun process(context: Context, uri: Uri): File {
        val outputDir = File(context.cacheDir, "processed").also { it.mkdirs() }
        val outputFile = File(outputDir, FakeMetadata.randomVideoFilename())

        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // Only keep audio + video tracks — metadata/subtitle tracks are dropped
        val trackMap = mutableMapOf<Int, Int>()
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                trackMap[i] = muxer.addTrack(format)
                extractor.selectTrack(i)
            }
        }

        muxer.start()

        var buffer = ByteBuffer.allocate(2 * 1024 * 1024) // 2 MB initial; grows if needed
        val info = MediaCodec.BufferInfo()

        while (extractor.sampleTrackIndex >= 0) {
            val sampleSize = extractor.sampleSize.toInt()
            if (sampleSize > buffer.capacity()) {
                buffer = ByteBuffer.allocate(sampleSize + 4096)
            }
            buffer.clear()

            val read = extractor.readSampleData(buffer, 0)
            if (read < 0) break

            val outTrack = trackMap[extractor.sampleTrackIndex]
            if (outTrack != null) {
                info.offset = 0
                info.size = read
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(outTrack, buffer, info)
            }
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()

        return outputFile
    }
}
