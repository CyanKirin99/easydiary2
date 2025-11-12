// 文件位置: app/src/main/java/com/example/easydiary/ui/DiaryViewModel.kt
// [已修复]: 方案F：修复导入/导出逻辑，使其支持图片文件的备份和恢复
// [已修复]: (KSP 错误) 修复 exportHumanReadableCsv 中的 'item.id' 空指针问题
package com.example.easydiary.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.easydiary.data.AppTheme
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.DiaryRepository
import com.example.easydiary.data.ExportData
import com.example.easydiary.data.SettingsRepository
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.DiaryEntryWithDetails
import com.example.easydiary.data.model.LogItem
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.data.model.LogType
import com.example.easydiary.data.model.TextEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import android.os.ParcelFileDescriptor
import java.io.FileDescriptor

// (UiStates 保持不变)
data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val logTypes: List<LogType> = emptyList()
)

data class EntryScreenState(
    val moodScore: Int = 2,
    val tomorrowPlans: List<String> = listOf(""),
    val logData: Map<Long, LogData> = emptyMap(),
    val expandedLogTypeId: Long? = null
) {
    data class LogData(
        val texts: List<String> = listOf(""),
        val duration: Float = 0f,
        val mediaPath: String? = null
    )
}

class DiaryViewModel(
    private val repository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModel() {

    // ... (1. 到 5. 的所有状态和回调 保持不变) ...

    // --- 1. 全局 UI 状态 ---
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()
    // --- 2. EntryScreen 的 临时编辑状态 ---
    private val _entryState = MutableStateFlow(EntryScreenState())
    val entryState: StateFlow<EntryScreenState> = _entryState.asStateFlow()
    // --- 3. 设置 (Settings) Flow ---
    val appTheme: Flow<AppTheme> = settingsRepository.appTheme
    val calendarView: Flow<CalendarView> = settingsRepository.calendarView
    val allEntries: StateFlow<List<DiaryEntry>> = repository.getAllDiaryEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val allLogItemsWithTexts: StateFlow<List<LogItemWithTexts>> = repository.getAllLogItemsWithTexts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        repository.getLogTypes()
            .onEach { types ->
                _uiState.update { it.copy(logTypes = types) }
                _entryState.update { entryScreenState ->
                    val newLogData = types.associate {
                        it.id to (entryScreenState.logData[it.id] ?: EntryScreenState.LogData())
                    }
                    entryScreenState.copy(logData = newLogData)
                }
            }
            .launchIn(viewModelScope)
    }

    // --- 4. EntryScreen 的数据加载和事件 ---
    fun getDiaryForDate(date: String): Flow<DiaryEntryWithDetails?> {
        return repository.getDiaryEntryWithDetails(date)
    }
    fun loadEntryForDate(details: DiaryEntryWithDetails?) {
        val defaultExpandedId = _uiState.value.logTypes.firstOrNull()?.id
        if (details == null) {
            _entryState.update {
                EntryScreenState(
                    logData = _uiState.value.logTypes.associate {
                        it.id to EntryScreenState.LogData()
                    },
                    expandedLogTypeId = defaultExpandedId
                )
            }
        } else {
            val newLogData = _uiState.value.logTypes.associate { logType ->
                val logItem = details.logItems.find { it.logItem.logTypeId == logType.id }
                logType.id to EntryScreenState.LogData(
                    texts = logItem?.texts?.map { it.content }?.ifEmpty { listOf("") } ?: listOf(""),
                    duration = logItem?.logItem?.duration ?: 0f,
                    mediaPath = logItem?.logItem?.mediaPath
                )
            }
            _entryState.update {
                EntryScreenState(
                    moodScore = details.entry.moodScore,
                    tomorrowPlans = details.entry.tomorrowPlan?.split("\n")?.ifEmpty { listOf("") } ?: listOf(""),
                    logData = newLogData,
                    expandedLogTypeId = defaultExpandedId
                )
            }
        }
    }

    // --- 5. 所有 "状态提升" 的事件回调 ---
    fun onMoodChange(score: Int) {
        _entryState.update { it.copy(moodScore = score) }
    }
    fun onTomorrowPlanChange(texts: List<String>) {
        _entryState.update { it.copy(tomorrowPlans = texts) }
    }
    fun onLogTextsChange(logTypeId: Long, texts: List<String>) {
        val currentLogData = _entryState.value.logData[logTypeId] ?: EntryScreenState.LogData()
        _entryState.update {
            it.copy(
                logData = it.logData + (logTypeId to currentLogData.copy(texts = texts))
            )
        }
    }
    fun onLogDurationChange(logTypeId: Long, duration: Float) {
        val currentLogData = _entryState.value.logData[logTypeId] ?: EntryScreenState.LogData()
        _entryState.update {
            it.copy(
                logData = it.logData + (logTypeId to currentLogData.copy(duration = duration))
            )
        }
    }
    fun onMediaPathChange(logTypeId: Long, path: String?) {
        val currentLogData = _entryState.value.logData[logTypeId] ?: EntryScreenState.LogData()
        _entryState.update {
            it.copy(
                logData = it.logData + (logTypeId to currentLogData.copy(mediaPath = path))
            )
        }
    }
    fun onLogCardToggled(logTypeId: Long) {
        _entryState.update {
            if (it.expandedLogTypeId == logTypeId) {
                it.copy(expandedLogTypeId = null)
            } else {
                it.copy(expandedLogTypeId = logTypeId)
            }
        }
    }


    // --- 6. (核心) 保存逻辑 (L10) ---
    // (保持不变)
    fun saveEntry(date: LocalDate) {
        viewModelScope.launch {
            val currentState = _entryState.value
            val dateString = date.toString()
            val entry = DiaryEntry(
                date = dateString,
                moodScore = currentState.moodScore,
                tomorrowPlan = currentState.tomorrowPlans.filter { it.isNotBlank() }.joinToString("\n")
            )
            repository.saveDiaryEntry(entry)
            for (logType in _uiState.value.logTypes) {
                val logData = currentState.logData[logType.id] ?: continue
                val texts = logData.texts.filter { it.isNotBlank() }
                val duration = logData.duration
                val mediaPath = logData.mediaPath
                if (texts.isEmpty() && duration == 0f && mediaPath == null) {
                    continue
                }
                val logItemId = repository.saveLogItem(
                    LogItem(
                        diaryDate = dateString,
                        logTypeId = logType.id,
                        duration = if (logType.hasDuration) duration else null,
                        mediaPath = if (logType.hasMedia) mediaPath else null
                    )
                )
                if (logType.hasText) {
                    texts.forEachIndexed { index, content ->
                        repository.saveTextEntry(
                            TextEntry(
                                logItemId = logItemId,
                                content = content,
                                order = index
                            )
                        )
                    }
                }
            }
        }
    }

    // (保持不变)
    fun deleteEntry(date: LocalDate) {
        viewModelScope.launch {
            val details = getDiaryForDate(date.toString()).first()
            repository.deleteDiaryEntryByDate(date.toString())
            details?.logItems?.forEach { logItemWithTexts ->
                deleteMediaFile(logItemWithTexts.logItem.mediaPath)
            }
        }
    }

    // --- 7. & 8. (设置逻辑 保持不变) ---
    fun updateLogTypes(updatedTypes: List<LogType>) {
        viewModelScope.launch {
            repository.updateLogTypes(updatedTypes)
        }
    }
    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateAppTheme(theme)
        }
    }
    fun updateCalendarView(view: CalendarView) {
        viewModelScope.launch {
            settingsRepository.updateCalendarView(view)
        }
    }

    // --- [媒体处理逻辑 (方案E - 保持不变)] ---
    // (copyMediaToInternal, resizeAndCompressImage, deleteMediaFile, handleMediaSelection 均保持不变)
    private suspend fun copyMediaToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        val tempFile = File(appContext.cacheDir, "${UUID.randomUUID()}.tmp")
        try {
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            return@withContext null
        }
        val bitmap = resizeAndCompressImage(tempFile, 1080)
        if (bitmap == null) {
            tempFile.delete()
            return@withContext null
        }
        val destFile = File(File(appContext.filesDir, "media").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        try {
            FileOutputStream(destFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }
            bitmap.recycle()
            tempFile.delete()
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            destFile.delete()
            return@withContext null
        }
    }
    private fun resizeAndCompressImage(file: File, maxPixelSize: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null
            var inSampleSize = 1
            if (srcHeight > maxPixelSize || srcWidth > maxPixelSize) {
                val halfHeight = srcHeight / 2
                val halfWidth = srcWidth / 2
                while ((halfHeight / inSampleSize) >= maxPixelSize || (halfWidth / inSampleSize) >= maxPixelSize) {
                    inSampleSize *= 2
                }
            }
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            var bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                bitmap = rotatedBitmap
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    private suspend fun deleteMediaFile(path: String?) = withContext(Dispatchers.IO) {
        if (path == null) return@withContext
        try {
            val file = File(path)
            val mediaDir = File(appContext.filesDir, "media")
            if (file.exists() && file.absolutePath.startsWith(mediaDir.absolutePath)) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun handleMediaSelection(logTypeId: Long, uri: Uri?) {
        viewModelScope.launch {
            val oldPath = _entryState.value.logData[logTypeId]?.mediaPath
            var newPath: String? = null
            if (uri != null) {
                newPath = copyMediaToInternal(uri)
                if (newPath == null) {
                    return@launch
                }
            }
            if (oldPath != newPath) {
                deleteMediaFile(oldPath)
            }
            onMediaPathChange(logTypeId, newPath)
        }
    }


    // --- [导入/导出 逻辑 (已修复图片备份)] ---

    private fun csvEscape(data: String?): String {
        if (data == null) return ""
        val escaped = data.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun csvEscape(data: Any?): String {
        if (data == null) return "null"
        return csvEscape(data.toString())
    }

    private fun entriesToCsv(entries: List<DiaryEntry>): String {
        val sb = StringBuilder("date,moodScore,tomorrowPlan\n")
        entries.forEach {
            sb.appendLine("${csvEscape(it.date)},${it.moodScore},${csvEscape(it.tomorrowPlan)}")
        }
        return sb.toString()
    }

    private fun logTypesToCsv(logTypes: List<LogType>): String {
        val sb = StringBuilder("id,name,`order`,hasText,hasDuration,hasMedia\n")
        logTypes.forEach {
            sb.appendLine("${it.id},${csvEscape(it.name)},${it.order},${it.hasText},${it.hasDuration},${it.hasMedia}")
        }
        return sb.toString()
    }

    private fun logItemsToCsv(logItems: List<LogItem>): String {
        val sb = StringBuilder("id,diaryDate,logTypeId,duration,mediaPath\n")
        logItems.forEach {
            sb.appendLine("${it.id},${csvEscape(it.diaryDate)},${it.logTypeId},${csvEscape(it.duration)},${csvEscape(it.mediaPath)}")
        }
        return sb.toString()
    }

    private fun textEntriesToCsv(textEntries: List<TextEntry>): String {
        val sb = StringBuilder("id,logItemId,content,`order`\n")
        textEntries.forEach {
            sb.appendLine("${it.id},${it.logItemId},${csvEscape(it.content)},${it.order}")
        }
        return sb.toString()
    }

    private val csvRegex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()

    private fun String.removeCsvQuotes(): String {
        return this.removeSurrounding("\"").replace("\"\"", "\"")
    }

    private fun parseEntriesCsv(content: String): List<DiaryEntry> {
        return content.lines().drop(1).filter { it.isNotBlank() }.map { line ->
            val parts = line.split(csvRegex, limit = 3)
            DiaryEntry(
                date = parts[0].removeCsvQuotes(),
                moodScore = parts[1].trim().toInt(),
                tomorrowPlan = if (parts[2].trim() == "null") null else parts[2].removeCsvQuotes()
            )
        }
    }
    private fun parseLogTypesCsv(content: String): List<LogType> {
        return content.lines().drop(1).filter { it.isNotBlank() }.map { line ->
            val parts = line.split(csvRegex, limit = 6)
            LogType(
                id = parts[0].trim().toLong(),
                name = parts[1].removeCsvQuotes(),
                order = parts[2].trim().toInt(),
                hasText = parts[3].trim().toBoolean(),
                hasDuration = parts[4].trim().toBoolean(),
                hasMedia = parts[5].trim().toBoolean()
            )
        }
    }

    suspend fun exportDataToZip(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = repository.getExportData()
            val mediaDir = File(appContext.filesDir, "media")

            // 1. 创建可移植的 LogItems (将绝对路径转为相对路径)
            val portableLogItems = data.logItems.map { logItem ->
                if (logItem.mediaPath == null) {
                    logItem
                } else {
                    val file = File(logItem.mediaPath)
                    if (file.exists() && file.absolutePath.startsWith(mediaDir.absolutePath)) {
                        logItem.copy(mediaPath = "media/${file.name}")
                    } else {
                        logItem.copy(mediaPath = null)
                    }
                }
            }

            // 2. 使用可移植的 LogItems 生成 CSV
            val csvEntries = entriesToCsv(data.entries)
            val csvLogTypes = logTypesToCsv(data.logTypes)
            val csvLogItems = logItemsToCsv(portableLogItems) // <-- 使用修改后的列表
            val csvTextEntries = textEntriesToCsv(data.textEntries)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipStream ->
                    // 3. 写入 4 个 CSV 文件
                    zipStream.putNextEntry(ZipEntry("diary_entries.csv"))
                    zipStream.write(csvEntries.toByteArray())
                    zipStream.closeEntry()

                    zipStream.putNextEntry(ZipEntry("log_types.csv"))
                    zipStream.write(csvLogTypes.toByteArray())
                    zipStream.closeEntry()

                    zipStream.putNextEntry(ZipEntry("log_items.csv"))
                    zipStream.write(csvLogItems.toByteArray())
                    zipStream.closeEntry()

                    zipStream.putNextEntry(ZipEntry("text_entries.csv"))
                    zipStream.write(csvTextEntries.toByteArray())
                    zipStream.closeEntry()

                    // 4. 遍历原始数据，写入图片文件
                    for (logItem in data.logItems.filter { it.mediaPath != null }) {
                        val file = File(logItem.mediaPath!!)
                        if (file.exists() && file.absolutePath.startsWith(mediaDir.absolutePath)) {
                            zipStream.putNextEntry(ZipEntry("media/${file.name}"))
                            FileInputStream(file).use { fileInput ->
                                fileInput.copyTo(zipStream)
                            }
                            zipStream.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDataFromZip(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csvMap = mutableMapOf<String, String>()
            val newMediaPaths = mutableMapOf<String, String>()

            val mediaDir = File(appContext.filesDir, "media").apply { mkdirs() }
            mediaDir.listFiles()?.forEach { it.delete() }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName.endsWith(".csv")) {
                            val content = zipStream.bufferedReader().readText()
                            csvMap[entryName] = content
                        } else if (entryName.startsWith("media/") && !entry.isDirectory) {
                            try {
                                val fileName = File(entryName).name
                                val destFile = File(mediaDir, fileName)

                                FileOutputStream(destFile).use { fileOutput ->
                                    zipStream.copyTo(fileOutput)
                                }
                                newMediaPaths[entryName] = destFile.absolutePath
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }

            val entries = parseEntriesCsv(csvMap["diary_entries.csv"] ?: "")
            val logTypes = parseLogTypesCsv(csvMap["log_types.csv"] ?: "")

            val logItems = csvMap["log_items.csv"]?.lines()?.drop(1)?.filter { it.isNotBlank() }?.map {
                val parts = it.split(csvRegex, limit = 5)

                val relativePath = if(parts[4].trim() == "null") null else parts[4].removeCsvQuotes().trim()
                val newAbsolutePath = relativePath?.let { newMediaPaths[it] }

                LogItem(
                    parts[0].trim().toLong(),
                    parts[1].removeCsvQuotes(),
                    parts[2].trim().toLong(),
                    if(parts[3].trim() == "null") null else parts[3].trim().toFloat(),
                    newAbsolutePath
                )
            } ?: emptyList()

            val textEntries = csvMap["text_entries.csv"]?.lines()?.drop(1)?.filter { it.isNotBlank() }?.map {
                val parts = it.split(csvRegex, limit = 4)
                TextEntry(
                    parts[0].trim().toLong(),
                    parts[1].trim().toLong(),
                    parts[2].removeCsvQuotes(),
                    parts[3].trim().toInt()
                )
            } ?: emptyList()

            if (entries.isEmpty() || logTypes.isEmpty()) {
                return@withContext Result.failure(Exception("文件为空或 'diary_entries.csv' / 'log_types.csv' 缺失"))
            }

            repository.importData(ExportData(entries, logTypes, logItems, textEntries))
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    suspend fun exportHumanReadableCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = repository.getExportData()
            val (entries, logTypes, logItems, textEntries) = data

            val sortedLogTypes = logTypes.sortedBy { it.order }
            val logItemsMap = logItems.groupBy { it.diaryDate }
            val textEntriesMap = textEntries.groupBy { it.logItemId }
            val sortedEntries = entries.sortedBy { it.date }

            val sb = StringBuilder()

            val headers = mutableListOf("日期", "心情", "明日计划")
            for (logType in sortedLogTypes) {
                if (logType.hasText) headers.add("${logType.name}-记录")
                if (logType.hasDuration) headers.add("${logType.name}-时长")
                if (logType.hasMedia) headers.add("${logType.name}-媒体")
            }
            sb.appendLine(headers.joinToString(",") { csvEscape(it) })

            for (entry in sortedEntries) {
                val row = mutableListOf<String>()

                row.add(csvEscape(entry.date))
                row.add(csvEscape(entry.moodScore + 1))
                row.add(csvEscape(entry.tomorrowPlan))

                val dailyLogItems = logItemsMap[entry.date] ?: emptyList()

                for (logType in sortedLogTypes) {
                    val item = dailyLogItems.find { it.logTypeId == logType.id }

                    if (logType.hasText) {
                        // --- [编译错误修复点] ---
                        // 'item' 是 LogItem? (可空)，所以我们用 item?.id
                        val texts = (textEntriesMap[item?.id] ?: emptyList()).sortedBy { it.order }
                        // --- [修复点结束] ---
                        val textContent = texts.joinToString("\n") { it.content }
                        row.add(csvEscape(textContent))
                    }
                    if (logType.hasDuration) {
                        val duration = item?.duration?.let { if (it > 0f) it.toString() else "" } ?: ""
                        row.add(csvEscape(duration))
                    }
                    if (logType.hasMedia) {
                        val media = item?.mediaPath ?: ""
                        row.add(csvEscape(media))
                    }
                }

                sb.appendLine(row.joinToString(","))
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write(sb.toString())
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    // --- [导入/导出 结束] ---
}
// --- 结束 ---


// --- [Factory (保持不变)] ---
class DiaryViewModelFactory(
    private val repository: DiaryRepository,
    private val settingsRepository: SettingsRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(repository, settingsRepository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}