package com.myresume.app.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class AssistTask {
    DRAFT_SUMMARY,
    REWRITE_BULLET,
    TAILOR_TO_JOB
}

class ResumeAssistant(private val context: Context) {
    private val mutex = Mutex()
    private var engine: Engine? = null

    fun deviceLikelyTooSmall(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024L * 1024L)
        return totalMb < ModelPaths.MIN_RECOMMENDED_RAM_MB
    }

    fun isReady(): Boolean = ModelPaths.isModelPresent(context)

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (engine != null) return@withLock
            val path = ModelPaths.modelFile(context).absolutePath
            val config = EngineConfig(
                modelPath = path,
                backend = preferredBackend(),
                cacheDir = context.cacheDir.path
            )
            val created = Engine(config)
            created.initialize()
            engine = created
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }

    fun generate(
        task: AssistTask,
        resumeContext: String,
        userInput: String,
        jobDescription: String = ""
    ): Flow<String> = flow {
        ensureLoaded()
        val localEngine = engine ?: error("Model failed to load")
        val prompt = buildPrompt(task, resumeContext, userInput, jobDescription)
        localEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT)
            )
        ).use { conversation ->
            val builder = StringBuilder()
            conversation.sendMessageAsync(prompt).collect { message: Message ->
                val chunk = messageToText(message)
                if (chunk.isNotEmpty()) {
                    builder.append(chunk)
                    emit(builder.toString())
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun messageToText(message: Message): String {
        val fromContents = message.contents.contents.joinToString(separator = "") { content ->
            content.toString()
        }.trim()
        return fromContents.ifBlank { message.toString() }
    }

    private fun preferredBackend(): Backend {
        return if (hasOpenCl()) Backend.GPU() else Backend.CPU()
    }

    private fun hasOpenCl(): Boolean {
        return try {
            Build.SUPPORTED_ABIS.isNotEmpty()
            File("/system/vendor/lib64/libOpenCL.so").exists() ||
                File("/system/vendor/lib/libOpenCL.so").exists()
        } catch (_: Throwable) {
            false
        }
    }

    private fun buildPrompt(
        task: AssistTask,
        resumeContext: String,
        userInput: String,
        jobDescription: String
    ): String = when (task) {
        AssistTask.DRAFT_SUMMARY -> """
            Draft a concise professional resume summary (3-4 sentences).
            Resume context:
            $resumeContext
            Target role notes:
            $userInput
            Return only the summary text.
        """.trimIndent()
        AssistTask.REWRITE_BULLET -> """
            Rewrite this resume bullet to be clearer and more impact-focused. Keep it to one sentence.
            Resume context:
            $resumeContext
            Bullet:
            $userInput
            Return only the rewritten bullet.
        """.trimIndent()
        AssistTask.TAILOR_TO_JOB -> """
            Suggest tailored wording for the resume given this job description.
            Focus on summary and key bullet adjustments. Be concise.
            Resume context:
            $resumeContext
            Job description:
            $jobDescription
            Extra notes:
            $userInput
            Return plain text suggestions only.
        """.trimIndent()
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "You are a resume writing assistant. Help only with resume content. " +
                "Do not invent employers, degrees, or certifications the user did not provide. " +
                "Keep language professional and concise."
    }
}
