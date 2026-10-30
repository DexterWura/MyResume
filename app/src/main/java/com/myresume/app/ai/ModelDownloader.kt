package com.myresume.app.ai

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.myresume.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class ModelDownloader(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun isPresent(): Boolean = ModelPaths.isModelPresent(context)

    fun modelFile(): File = ModelPaths.modelFile(context)

    fun deleteModel() {
        ModelPaths.modelFile(context).delete()
        File(ModelPaths.modelDir(context), "${ModelPaths.MODEL_FILE_NAME}.partial").delete()
        workManager.cancelUniqueWork(ModelDownloadWorker.UNIQUE_WORK)
    }

    fun enqueueDownload(preferWifi: Boolean = true) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (preferWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ModelDownloadWorker.KEY_URL to BuildConfig.MODEL_URL))
            .build()
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.UNIQUE_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun observeProgress(): Flow<DownloadState> =
        workManager.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.UNIQUE_WORK).map { infos ->
            val info = infos.firstOrNull()
            when {
                isPresent() -> DownloadState.Ready
                info == null -> DownloadState.Idle
                info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED -> {
                    val progress = info.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                    DownloadState.Downloading(progress)
                }
                info.state == WorkInfo.State.SUCCEEDED -> DownloadState.Ready
                info.state == WorkInfo.State.FAILED -> {
                    val error = info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                        ?: "Download failed"
                    DownloadState.Failed(error)
                }
                else -> DownloadState.Idle
            }
        }
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val percent: Int) : DownloadState()
    data object Ready : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
