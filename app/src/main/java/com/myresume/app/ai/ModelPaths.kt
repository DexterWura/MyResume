package com.myresume.app.ai

import android.content.Context
import java.io.File

object ModelPaths {
    const val MODEL_FILE_NAME = "gemma3-1b-it-int4.litertlm"
    const val APPROX_SIZE_MB = 530
    const val MIN_RECOMMENDED_RAM_MB = 3072

    fun modelDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    fun modelFile(context: Context): File = File(modelDir(context), MODEL_FILE_NAME)

    fun isModelPresent(context: Context): Boolean {
        val file = modelFile(context)
        return file.exists() && file.length() > 1024L * 1024L
    }
}
