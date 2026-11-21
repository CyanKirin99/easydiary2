package com.example.easydiary.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.easydiary.ui.DiaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val periods = listOf(0 to "全部时间", 7 to "近 7 天", 30 to "近 30 天")
private val dateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日 · EEEE", Locale.CHINA)
private val moods = listOf("😢", "😟", "😐", "😊", "🤩")

private data class SearchResults(val diaryIsEmpty: Boolean, val hits: List<DiaryHit>)

@Composable
fun DiarySearchScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
    onOpen: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableIntStateOf(0) }
    val resultFlow = remember(viewModel) {
        combine(viewModel.searchableDays, snapshotFlow { query.trim() to period }) { days, filter ->
            val (keyword, range) = filter
            val today = LocalDate.now()
            val firstDay = today.minusDays((range - 1).toLong())
            val hits = days.asSequence()
                .filter { range == 0 || it.date in firstDay..today }
                .mapNotNull { it.findHit(keyword) }
                .toList()
            SearchResults(days.isEmpty(), hits)
        }.flowOn(Dispatchers.Default)
    }
    val results by resultFlow.collectAsState(initial = null)
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var openedBefore by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回日历")
            }
            Text("搜索日记", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            label = { Text("关键词或日期") },
            placeholder = { Text("正文、计划、类型或 2026-11") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isEmpty()) null else ({
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "清空关键词")
                }
            }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
        LaunchedEffect(Unit) {
            if (!openedBefore) {
                focus.requestFocus()
                openedBefore = true
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((days, label) in periods) {
                FilterChip(selected = period == days, onClick = { period = days }, label = { Text(label) })
            }
        }
        val current = results
        when {
            current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            current.hits.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (current.diaryIsEmpty) "还没有日记" else "没有找到匹配的日记",
                    style = MaterialTheme.typography.titleMedium)
                if (!current.diaryIsEmpty) {
                    TextButton(onClick = { query = ""; period = 0 }) { Text("查看全部日记") }
                }
            }
            else -> key(query.trim(), period) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("共 ${current.hits.size} 篇日记", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(current.hits, key = { it.date.toEpochDay() }) { hit ->
                        ResultCard(hit, onOpen = { onOpen(hit.date) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultCard(hit: DiaryHit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val highlight = SpanStyle(background = colors.primaryContainer, color = colors.onPrimaryContainer,
        fontWeight = FontWeight.SemiBold)
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hit.date.format(dateFormat), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                Text(moods.getOrNull(hit.moodScore).orEmpty())
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            hit.source?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.primary) }
            if (hit.excerpt.isNotEmpty()) {
                val preview = buildAnnotatedString {
                    append(hit.excerpt)
                    if (hit.matchStart >= 0) addStyle(highlight, hit.matchStart, hit.matchStart + hit.matchLength)
                }
                Text(preview, style = MaterialTheme.typography.bodyMedium, maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
