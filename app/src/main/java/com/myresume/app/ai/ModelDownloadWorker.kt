package com.myresume.app.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.myresume.app.BuildConfig
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val urlString = inputData.getString(KEY_URL)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.MODEL_URL
        if (urlString.isBlank()) {
            return Result.failure(workDataOf(KEY_ERROR to "MODEL_URL is not configured"))
        }

        val target = ModelPaths.modelFile(applicationContext)
        val partial = File(target.parentFile, "${target.name}.partial")
        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return Result.failure(
                    workDataOf(KEY_ERROR to "Download failed: HTTP ${connection.responseCode}")
                )
            }
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var downloaded = 0L
                    var lastProgress = -1
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                setProgress(workDataOf(KEY_PROGRESS to progress))
                            }
                        }
                    }
                    output.flush()
                }
            }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            Result.success(workDataOf(KEY_PROGRESS to 100))
        } catch (t: Throwable) {
            partial.delete()
            Result.failure(workDataOf(KEY_ERROR to (t.message ?: "Download failed")))
        }
    }

    companion object {
        const val UNIQUE_WORK = "model_download"
        const val KEY_URL = "url"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
    }
}
