// 文件位置: app/src/main/java/com/example/easydiary/ui/DiaryViewModel.kt
package com.example.easydiary.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.easydiary.data.AppFontFamily
import com.example.easydiary.data.AppTheme
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.ThemePreset
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 全局 UI 状态，主要包含应用范围内的配置，如日志类型。
 */
data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val logTypes: List<LogType> = emptyList(),
    val statisticsFilterLogTypeId: Long? = null
)

/**
 * [EntryScreen] 的临时编辑状态。
 * 当用户进入编辑模式时，此状态会从数据库加载；保存时，此状态被写回数据库。
 */
data class EntryScreenState(
    val moodScore: Int = 2,
    val tomorrowPlans: List<String> = listOf(""),
    val logData: Map<Long, LogData> = emptyMap(), // Key: LogType ID
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

    // --- UI 状态 ---

    // 全局 UI 状态
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    // EntryScreen 的临时编辑状态
    private val _entryState = MutableStateFlow(EntryScreenState())
    val entryState: StateFlow<EntryScreenState> = _entryState.asStateFlow()

    // --- 数据流 (Flows) ---

    // 应用设置
    val appTheme: Flow<AppTheme> = settingsRepository.appTheme
    val calendarView: Flow<CalendarView> = settingsRepository.calendarView
    val themePreset: Flow<ThemePreset> = settingsRepository.themePreset
    val appFontFamily: Flow<AppFontFamily> = settingsRepository.appFontFamily

    // 数据库数据
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
        // 订阅 LogTypes 的变化，并更新 UI 状态
        repository.getLogTypes()
            .onEach { types ->
                _uiState.update { it.copy(logTypes = types) }
                // 确保 EntryState 也同步更新，以防 LogTypes 发生变化（例如用户在设置中修改）
                _entryState.update { entryScreenState ->
                    val newLogData = types.associate {
                        it.id to (entryScreenState.logData[it.id] ?: EntryScreenState.LogData())
                    }
                    entryScreenState.copy(logData = newLogData)
                }
            }
            .launchIn(viewModelScope)
    }

    // --- EntryScreen 数据加载 ---

    fun getDiaryForDate(date: String): Flow<DiaryEntryWithDetails?> {
        return repository.getDiaryEntryWithDetails(date)
    }

    /**
     * 当进入编辑模式时，加载特定日期的数据到临时的 [entryState]。
     */
    fun loadEntryForDate(details: DiaryEntryWithDetails?) {
        val defaultExpandedId = _uiState.value.logTypes.firstOrNull()?.id

        if (details == null) {
            // 如果当天没有数据，则重置为默认空状态
            _entryState.update {
                EntryScreenState(
                    logData = _uiState.value.logTypes.associate {
                        it.id to EntryScreenState.LogData()
                    },
                    expandedLogTypeId = defaultExpandedId
                )
            }
        } else {
            // 如果有数据，则填充
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

    // --- EntryScreen 事件回调 (状态提升) ---

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


    // --- 核心保存与删除 ---

    /**
     * 将临时的 [entryState] 保存到数据库。
     */
    fun saveEntry(date: LocalDate) {
        viewModelScope.launch {
            val currentState = _entryState.value
            val dateString = date.toString()

            // 1. 保存顶层 Entry
            val entry = DiaryEntry(
                date = dateString,
                moodScore = currentState.moodScore,
                tomorrowPlan = currentState.tomorrowPlans.filter { it.isNotBlank() }.joinToString("\n")
            )
            repository.saveDiaryEntry(entry)

            // 2. 遍历所有 LogTypes，保存对应的 LogItem 和 TextEntries
            for (logType in _uiState.value.logTypes) {
                val logData = currentState.logData[logType.id] ?: continue
                val texts = logData.texts.filter { it.isNotBlank() }
                val duration = logData.duration
                val mediaPath = logData.mediaPath

                // 如果没有数据，则跳过
                if (texts.isEmpty() && duration == 0f && mediaPath == null) {
                    continue
                }

                // 2a. 保存 LogItem
                val logItemId = repository.saveLogItem(
                    LogItem(
                        diaryDate = dateString,
                        logTypeId = logType.id,
                        duration = if (logType.hasDuration) duration else null,
                        mediaPath = if (logType.hasMedia) mediaPath else null
                    )
                )

                // 2b. 如果有文本，保存 TextEntries
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

    /**
     * 删除指定日期的所有记录，并清理关联的媒体文件。
     */
    fun deleteEntry(date: LocalDate) {
        viewModelScope.launch {
            // 必须先获取详情，以便拿到媒体路径
            val details = getDiaryForDate(date.toString()).first()

            // 删除数据库条目 (级联删除 LogItems 和 TextEntries)
            repository.deleteDiaryEntryByDate(date.toString())

            // 删除关联的媒体文件
            details?.logItems?.forEach { logItemWithTexts ->
                deleteMediaFile(logItemWithTexts.logItem.mediaPath)
            }
        }
    }

    // --- 设置逻辑 ---

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

    fun updateThemePreset(preset: ThemePreset) {
        viewModelScope.launch {
            settingsRepository.updateThemePreset(preset)
        }
    }

    fun updateAppFontFamily(font: AppFontFamily) {
        viewModelScope.launch {
            settingsRepository.updateAppFontFamily(font)
        }
    }

    fun updateStatisticsFilter(logTypeId: Long?) {
        _uiState.update { it.copy(statisticsFilterLogTypeId = logTypeId) }
    }

    // --- 媒体文件处理 ---

    /**
     * 接收来自媒体选择器 (ContentResolver) 的 Uri，将其复制到应用的内部存储。
     * 复制过程中会进行压缩和旋转修正。
     * @return 内部存储的绝对路径。
     */
    private suspend fun copyMediaToInternal(uri: Uri): String? = withContext(Dispatchers.IO) {
        // 1. 复制到临时文件
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

        // 2. 压缩和旋转修正
        val bitmap = resizeAndCompressImage(tempFile, 1080)
        if (bitmap == null) {
            tempFile.delete()
            return@withContext null
        }

        // 3. 保存到内部存储的 "media" 目录
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

    /**
     * 调整图片大小并根据 EXIF 信息修正旋转角度。
     */
    private fun resizeAndCompressImage(file: File, maxPixelSize: Int): Bitmap? {
        try {
            // 1. 仅解码边界以获取原始尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            // 2. 计算采样率 (inSampleSize)
            var inSampleSize = 1
            if (srcHeight > maxPixelSize || srcWidth > maxPixelSize) {
                val halfHeight = srcHeight / 2
                val halfWidth = srcWidth / 2
                while ((halfHeight / inSampleSize) >= maxPixelSize || (halfWidth / inSampleSize) >= maxPixelSize) {
                    inSampleSize *= 2
                }
            }

            // 3. 读取 EXIF 旋转信息
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            // 4. 实际解码位图
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            var bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null

            // 5. 如果需要，应用旋转
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

    /**
     * 从内部存储中删除指定的媒体文件。
     */
    private suspend fun deleteMediaFile(path: String?) = withContext(Dispatchers.IO) {
        if (path == null) return@withContext
        try {
            val file = File(path)
            val mediaDir = File(appContext.filesDir, "media")
            // 安全检查：确保只删除 "media" 目录下的文件
            if (file.exists() && file.absolutePath.startsWith(mediaDir.absolutePath)) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 处理来自 UI 的媒体选择事件（添加或删除）。
     */
    fun handleMediaSelection(logTypeId: Long, uri: Uri?) {
        viewModelScope.launch {
            val oldPath = _entryState.value.logData[logTypeId]?.mediaPath
            var newPath: String? = null

            if (uri != null) {
                // 如果是添加/替换，复制新文件
                newPath = copyMediaToInternal(uri)
                if (newPath == null) {
                    // 复制失败，保持原状
                    return@launch
                }
            }

            // 如果路径发生变化 (新 -> 旧，旧 -> 空，或 新 -> 空)，删除旧文件
            if (oldPath != newPath) {
                deleteMediaFile(oldPath)
            }

            // 更新 UI 状态
            onMediaPathChange(logTypeId, newPath)
        }
    }


    // --- 导入/导出 逻辑 ---

    private fun csvEscape(data: String?): String {
        if (data == null) return ""
        val escaped = data.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
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
        return this.removeSurrounding("\"").replace("\"\"", "\"").replace("\\n", "\n")
    }

    private fun readCsvLines(content: String): List<String> {
        val rawLines = content.lines()
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (line in rawLines) {
            if (current.isEmpty()) {
                current.append(line)
            } else {
                current.append("\n").append(line)
            }

            for (ch in line) {
                if (ch == '"') inQuotes = !inQuotes
            }

            if (!inQuotes) {
                result.add(current.toString())
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }

    private fun parseEntriesCsv(content: String): List<DiaryEntry> {
        return readCsvLines(content).drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split(csvRegex, limit = 3)
            if (parts.size < 3) return@mapNotNull null
            DiaryEntry(
                date = parts[0].removeCsvQuotes(),
                moodScore = parts[1].trim().toInt(),
                tomorrowPlan = if (parts[2].trim() == "null") null else parts[2].removeCsvQuotes()
            )
        }
    }
    private fun parseLogTypesCsv(content: String): List<LogType> {
        return readCsvLines(content).drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split(csvRegex, limit = 6)
            if (parts.size < 6) return@mapNotNull null
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

    /**
     * 导出所有数据（包括媒体文件）到 .zip 压缩包。
     */
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
                        // "media/filename.jpg"
                        logItem.copy(mediaPath = "media/${file.name}")
                    } else {
                        // 路径无效，置空
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

                    // 4. 遍历原始数据 (data.logItems)，写入图片文件
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

    /**
     * 从 .zip 压缩包导入数据，返回 Result 以便 UI 显示成功或失败信息。
     */
    suspend fun importDataFromZip(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csvMap = mutableMapOf<String, String>()
            val newMediaPaths = mutableMapOf<String, String>() // "media/old.jpg" -> "/data/data/.../media/new.jpg"

            val mediaDir = File(appContext.filesDir, "media").apply { mkdirs() }
            // 清空旧媒体文件
            mediaDir.listFiles()?.forEach { it.delete() }

            // 1. 解压 .zip
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName.endsWith(".csv")) {
                            // 1a. 读取 CSV 内容到内存
                            val content = zipStream.bufferedReader().readText()
                            csvMap[entryName] = content
                        } else if (entryName.startsWith("media/") && !entry.isDirectory) {
                            // 1b. 解压媒体文件到内部存储
                            try {
                                val fileName = File(entryName).name
                                val destFile = File(mediaDir, fileName)

                                FileOutputStream(destFile).use { fileOutput ->
                                    zipStream.copyTo(fileOutput)
                                }
                                // 记录相对路径到绝对路径的映射
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

            // 2. 解析 CSV
            val entries = parseEntriesCsv(csvMap["diary_entries.csv"] ?: "")
            val logTypes = parseLogTypesCsv(csvMap["log_types.csv"] ?: "")

            // 2a. 解析 LogItems，并转换媒体路径
            val logItems = readCsvLines(csvMap["log_items.csv"] ?: "").drop(1).filter { it.isNotBlank() }.mapNotNull {
                val parts = it.split(csvRegex, limit = 5)
                if (parts.size < 5) return@mapNotNull null

                val relativePath = if(parts[4].trim() == "null") null else parts[4].removeCsvQuotes().trim()
                // 使用映射表查找新的绝对路径
                val newAbsolutePath = relativePath?.let { newMediaPaths[it] }

                LogItem(
                    parts[0].trim().toLong(),
                    parts[1].removeCsvQuotes(),
                    parts[2].trim().toLong(),
                    if(parts[3].trim() == "null") null else parts[3].trim().toFloat(),
                    newAbsolutePath // 使用新的路径
                )
            }

            // 2b. 解析 TextEntries
            val textEntries = readCsvLines(csvMap["text_entries.csv"] ?: "").drop(1).filter { it.isNotBlank() }.mapNotNull {
                val parts = it.split(csvRegex, limit = 4)
                if (parts.size < 4) return@mapNotNull null
                TextEntry(
                    parts[0].trim().toLong(),
                    parts[1].trim().toLong(),
                    parts[2].removeCsvQuotes(),
                    parts[3].trim().toInt()
                )
            }

            if (entries.isEmpty() || logTypes.isEmpty()) {
                return@withContext Result.failure(Exception("文件为空或 'diary_entries.csv' / 'log_types.csv' 缺失"))
            }

            // 3. 导入数据库
            repository.importData(ExportData(entries, logTypes, logItems, textEntries))
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    /**
     * 导出人类可读的 CSV 文件（用于 Excel 等）。
     */
    suspend fun exportHumanReadableCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = repository.getExportData()
            val (entries, logTypes, logItems, textEntries) = data

            val sortedLogTypes = logTypes.sortedBy { it.order }
            val logItemsMap = logItems.groupBy { it.diaryDate }
            val textEntriesMap = textEntries.groupBy { it.logItemId }
            val sortedEntries = entries.sortedBy { it.date }

            val sb = StringBuilder()

            // 1. 构建表头
            val headers = mutableListOf("日期", "心情", "明日计划")
            for (logType in sortedLogTypes) {
                if (logType.hasText) headers.add("${logType.name}-记录")
                if (logType.hasDuration) headers.add("${logType.name}-时长")
                if (logType.hasMedia) headers.add("${logType.name}-媒体")
            }
            sb.appendLine(headers.joinToString(",") { csvEscape(it) })

            // 2. 遍历每一天
            for (entry in sortedEntries) {
                val row = mutableListOf<String>()

                row.add(csvEscape(entry.date))
                row.add(csvEscape(entry.moodScore + 1)) // (转为 1-5)
                row.add(csvEscape(entry.tomorrowPlan))

                val dailyLogItems = logItemsMap[entry.date] ?: emptyList()

                // 3. 遍历每种 LogType，填充对应数据
                for (logType in sortedLogTypes) {
                    val item = dailyLogItems.find { it.logTypeId == logType.id }

                    if (logType.hasText) {
                        // 'item' 是 LogItem? (可空)，所以我们用 item?.id
                        val texts = (textEntriesMap[item?.id] ?: emptyList()).sortedBy { it.order }
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

            // 4. 写入文件 (带 BOM 头以便 Excel 正确识别 UTF-8)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("\uFEFF") // BOM
                    writer.write(sb.toString())
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}


// --- ViewModel Factory ---
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