package rf.ztech.gutchecker.data

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class LLMRepository(private val context: Context) {

    private val modelFileName = "gemma-2b-it-cpu-int4.bin"
    private val modelAssetPath = "models/$modelFileName"
    private val TAG = "LLMRepository"

    companion object {
        @Volatile
        private var instance: LlmInference? = null
        private val initMutex = Mutex()
    }

    suspend fun isModelAvailable(): Boolean = withContext(Dispatchers.IO) {
        val internalFile = File(context.filesDir, modelFileName)
        // Если файл уже есть и он весит больше 1 ГБ
        if (internalFile.exists() && internalFile.length() > 1_000_000_000) return@withContext true
        
        try {
            val assets = context.assets.list("models")
            assets?.contains(modelFileName) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun getAvailableSpace(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBytes
    }

    private suspend fun getOrInitLlm(): LlmInference = withContext(Dispatchers.IO) {
        initMutex.withLock {
            instance?.let { return@withLock it }

            val modelFile = File(context.filesDir, modelFileName)
            
            // Проверка целостности и копирование
            if (!modelFile.exists() || modelFile.length() < 1_000_000_000) {
                val requiredSpace = 2_000_000_000L // Примерно 2ГБ
                if (getAvailableSpace() < requiredSpace) {
                    throw RuntimeException("Недостаточно места на устройстве. Нужно еще ~2 ГБ.")
                }

                Log.i(TAG, "Копирование модели... Это займет время.")
                context.assets.open(modelAssetPath).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            Log.i(TAG, "Инициализация MediaPipe LlmInference (CPU)...")
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(512)
                    .setTemperature(0.7f)
                    .setTopK(40)
                    .build()

                instance = LlmInference.createFromOptions(context, options)
                Log.i(TAG, "Успех!")
                instance!!
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка MediaPipe", e)
                throw RuntimeException("Ошибка AI движка. Попробуйте перезагрузить телефон. Подробности: ${e.localizedMessage}")
            }
        }
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val llm = getOrInitLlm()
            val result = llm.generateResponse(prompt)
            if (result.isNullOrBlank()) "AI не смог сформулировать ответ." else result
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Неизвестная ошибка"
            Log.e(TAG, "Error: $errorMsg")
            if (errorMsg.contains("space", ignoreCase = true)) {
                "Ошибка: На устройстве закончилось место для работы AI."
            } else {
                "AI временно недоступен: $errorMsg"
            }
        }
    }
}
