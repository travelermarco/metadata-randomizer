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
        var muxerStarted = false

        try {
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

            if (trackMap.isEmpty()) {
                throw IllegalArgumentException("No audio or video tracks found in provided media")
            }

            muxer.start()
            muxerStarted = true

            var buffer = ByteBuffer.allocate(2 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()

            while (extractor.sampleTrackIndex >= 0) {
                val sampleSize = extractor.sampleSize
                if (sampleSize <= 0L) break

                // Grow buffer if needed; guard against Long→Int overflow on huge samples
                val needed = (sampleSize + 4096L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                if (needed > buffer.capacity()) {
                    buffer = ByteBuffer.allocate(needed)
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

                if (!extractor.advance()) break
            }

            muxer.stop()
        } catch (e: Exception) {
            // Delete partial/corrupt output so it is never shared
            outputFile.delete()
            throw e
        } finally {
            // Always release native resources regardless of success or failure
            if (muxerStarted) {
                runCatching { muxer.release() }
            } else {
                muxer.release()
            }
            extractor.release()
        }

        return outputFile
    }
}
