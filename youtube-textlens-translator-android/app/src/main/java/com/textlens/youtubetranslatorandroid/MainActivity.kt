package com.textlens.youtubetranslatorandroid

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Gold = Color(0xFFFFCC18)
private val GoldSoft = Color(0xFFFFD95A)
private val Ink = Color(0xFF050505)
private val Panel = Color(0xFF10100D)
private val Muted = Color(0xFFA8A39A)

private data class SubtitleBlock(
    val number: String,
    val timing: String,
    val text: String
)

private data class TranslationConfig(
    val provider: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val targetLanguage: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TranslatorApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslatorApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("No SRT selected") }
    var translatedText by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf("Ready.") }
    var showLogs by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var provider by remember { mutableStateOf("OpenRouter") }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://openrouter.ai/api/v1") }
    var model by remember { mutableStateOf("google/gemma-4-31b-it:free") }
    var targetLanguage by remember { mutableStateOf("Persian") }
    var pendingSave by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf("youtube") }
    var youtubeUrl by remember { mutableStateOf("") }
    var subtitleToUrl by remember { mutableStateOf("") }
    var subtitleFetchStatus by remember { mutableStateOf("Paste a YouTube link and fetch the first SRT through subtitle.to.") }
    var isFetchingSubtitle by remember { mutableStateOf(false) }
    var subtitleWebView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    val openSrt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            sourceText = text
            sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "subtitle.srt"
            val count = parseSrt(text).size
            logs = "Loaded $sourceName\n$count subtitle cues detected."
            showLogs = false
            translatedText = ""
        }
    }
    val saveSrt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri: Uri? ->
        if (uri != null && pendingSave.isNotBlank()) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingSave) }
            logs = "Saved translated SRT successfully."
        }
    }

    MaterialTheme {
        Surface(color = Ink) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x553A2F00), Ink),
                            radius = 900f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Header(
                        isSettings = showSettings,
                        onSettingsClick = { showSettings = !showSettings }
                    )

                    if (showSettings) {
                        SettingsPanel(
                            provider = provider,
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            model = model,
                            targetLanguage = targetLanguage,
                            onProviderChange = { nextProvider ->
                                provider = nextProvider
                                if (nextProvider == "OpenRouter") {
                                    baseUrl = "https://openrouter.ai/api/v1"
                                    model = "google/gemma-4-31b-it:free"
                                } else {
                                    baseUrl = "https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1"
                                    model = "openai/gpt-5-nano"
                                }
                            },
                            onApiKeyChange = { apiKey = it },
                            onBaseUrlChange = { baseUrl = it },
                            onModelChange = { model = it },
                            onTargetLanguageChange = { targetLanguage = it }
                        )
                    } else {
                        Card {
                        Text("Subtitle source", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text("Choose one workflow, then translate the loaded SRT.", color = Muted, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        TabBar(
                            leftLabel = "YouTube Link",
                            rightLabel = "Local SRT File",
                            selected = inputMode,
                            onSelect = { inputMode = it }
                        )
                        Spacer(Modifier.height(8.dp))
                        if (inputMode == "youtube") {
                            AppTextField("YouTube video URL", youtubeUrl, { youtubeUrl = it })
                            Text(subtitleFetchStatus, color = Muted, fontSize = 13.sp)
                            GoldButton("Fetch SRT", enabled = youtubeUrl.isNotBlank() && !isFetchingSubtitle && !isTranslating) {
                                val normalized = normalizeYouTubeUrl(youtubeUrl)
                                val videoId = extractYouTubeId(normalized)
                                if (videoId.isBlank()) {
                                    subtitleFetchStatus = "Could not detect a YouTube video id."
                                } else {
                                    isFetchingSubtitle = true
                                    subtitleFetchStatus = "Loading subtitle.to in background..."
                                    subtitleToUrl = "https://subtitle.to/$normalized"
                                    sourceName = "$videoId-en.srt"
                                }
                            }
                        } else {
                            Text(sourceName, color = Muted, fontSize = 14.sp)
                            GoldButton("Choose SRT", enabled = !isTranslating) { openSrt.launch(arrayOf("application/x-subrip", "text/*", "*/*")) }
                        }
                        }

                        if (subtitleToUrl.isNotBlank()) {
                            SubtitleToWebView(
                                url = subtitleToUrl,
                                webView = subtitleWebView,
                                onWebView = { subtitleWebView = it },
                                onStatus = { subtitleFetchStatus = it },
                                onDownload = { downloadUrl, userAgent ->
                                    scope.launch {
                                        try {
                                            subtitleFetchStatus = "Downloading SRT..."
                                            val text = downloadSubtitle(downloadUrl, userAgent, subtitleToUrl)
                                            val count = parseSrt(text).size
                                            require(count > 0) { "Downloaded file did not contain SRT cues." }
                                            sourceText = text
                                            translatedText = ""
                                            pendingSave = ""
                                            logs = "Fetched $sourceName\n$count subtitle cues detected."
                                            showLogs = false
                                            subtitleFetchStatus = "SRT fetched successfully. Ready to translate."
                                        } catch (error: Exception) {
                                            subtitleFetchStatus = "Fetch failed: ${error.message}"
                                        } finally {
                                            isFetchingSubtitle = false
                                        }
                                    }
                                }
                            )
                        }

                    if (sourceText.isNotBlank()) {
                        Card {
                            Text("Translate and save", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("Current file: $sourceName", color = Muted, fontSize = 14.sp)
                            Text("Provider: $provider • Model: $model", color = Muted, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GoldButton("Translate", enabled = apiKey.isNotBlank() && !isTranslating) {
                                    scope.launch {
                                    isTranslating = true
                                    showLogs = true
                                    progress = 0f
                                    logs = "Preparing subtitle blocks..."
                                        val config = TranslationConfig(provider, apiKey, baseUrl, model, targetLanguage)
                                        try {
                                            translatedText = translateSrt(sourceText, config) { message, value, _ ->
                                                logs = message
                                                progress = value
                                            }
                                            pendingSave = translatedText
                                            logs = "Translation completed successfully."
                                        } catch (error: Exception) {
                                            logs = "Translation failed: ${error.message}"
                                        } finally {
                                            isTranslating = false
                                        }
                                    }
                                }
                                GoldButton("Save", enabled = translatedText.isNotBlank() && !isTranslating) {
                                    pendingSave = translatedText
                                    saveSrt.launch(defaultOutputName(sourceName))
                                }
                            }
                            if (isTranslating) {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(progress = { progress }, color = Gold, trackColor = Color(0x443A2F00), modifier = Modifier.size(36.dp))
                                    Text("${(progress * 100).toInt()}%", color = GoldSoft, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    if (showLogs) {
                        Card {
                            Text("Translation logs", color = Gold, fontWeight = FontWeight.Black)
                            Text(logs, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                    }
                }
            }
        }
    }

    LaunchedEffect(subtitleToUrl, isFetchingSubtitle) {
        if (subtitleToUrl.isBlank() || !isFetchingSubtitle) return@LaunchedEffect
        repeat(30) { attempt ->
            delay(1200)
            val view = subtitleWebView ?: return@repeat
            subtitleFetchStatus = "Searching for SRT... ${attempt + 1}/30"
            view.evaluateJavascript(FIRST_SRT_CLICK_SCRIPT) { result ->
                if (result.contains("clicked", ignoreCase = true)) {
                    subtitleFetchStatus = "Clicked SRT, waiting for download..."
                }
            }
        }
        if (isFetchingSubtitle) {
            subtitleFetchStatus = "SRT button not found. Use Choose downloaded SRT after downloading manually."
            isFetchingSubtitle = false
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SubtitleToWebView(
    url: String,
    webView: WebView?,
    onWebView: (WebView) -> Unit,
    onStatus: (String) -> Unit,
    onDownload: (String, String) -> Unit
) {
    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = false
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        onStatus("Page loaded. Waiting for SRT list...")
                    }
                }
                setDownloadListener { downloadUrl, userAgent, _, _, _ ->
                    onDownload(downloadUrl, userAgent.orEmpty())
                }
                onWebView(this)
                loadUrl(url)
            }
        },
        update = {
            if (webView?.url != url) {
                it.loadUrl(url)
            }
        }
    )
}

@Composable
private fun TabBar(
    leftLabel: String,
    rightLabel: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF211E14), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabItem(
            label = leftLabel,
            active = selected == "youtube",
            modifier = Modifier.weight(1f)
        ) { onSelect("youtube") }
        TabItem(
            label = rightLabel,
            active = selected == "file",
            modifier = Modifier.weight(1f)
        ) { onSelect("file") }
    }
}

@Composable
private fun TabItem(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Gold else Color.Transparent,
            contentColor = if (active) Color.Black else Color(0xFFE9E2CF)
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = modifier.height(48.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(label, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Header(isSettings: Boolean, onSettingsClick: () -> Unit) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Gold, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("T", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 30.sp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "YouTube TextLens",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 23.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Fetch, translate, and save Persian subtitles.",
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onSettingsClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29261B), contentColor = Gold),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text(if (isSettings) "‹" else "⚙︎", fontSize = if (isSettings) 32.sp else 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    provider: String,
    apiKey: String,
    baseUrl: String,
    model: String,
    targetLanguage: String,
    onProviderChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit
) {
    Card {
        Text("Settings", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("API provider, model, and translation target.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Toggle("OpenRouter", provider == "OpenRouter", modifier = Modifier.weight(1f)) {
                onProviderChange("OpenRouter")
            }
            Toggle("Liara", provider == "Liara", modifier = Modifier.weight(1f)) {
                onProviderChange("Liara")
            }
        }
        Spacer(Modifier.height(12.dp))
        AppTextField("API key", apiKey, onApiKeyChange, password = true)
        AppTextField("Base URL", baseUrl, onBaseUrlChange)
        ModelPicker(provider, model, onModelChange)
        AppTextField("Target language", targetLanguage, onTargetLanguageChange)
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Panel.copy(alpha = 0.94f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun GoldButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black, disabledContainerColor = Color(0xFF3A3520), disabledContentColor = Muted),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(label, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun Toggle(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (active) Gold else Color(0xFF29261B), contentColor = if (active) Color.Black else Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (active) Gold else Color(0x664D3D00)),
        modifier = modifier.height(52.dp)
    ) {
        Text(label, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AppTextField(label: String, value: String, onValueChange: (String) -> Unit, password: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Black,
            unfocusedContainerColor = Color.Black,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Gold,
            unfocusedIndicatorColor = Color(0x663A2F00),
            focusedLabelColor = Gold,
            unfocusedLabelColor = Muted
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(provider: String, model: String, onModelChange: (String) -> Unit) {
    val models = if (provider == "Liara") {
        listOf("openai/gpt-5-nano", "openai/gpt-4.1-mini", "google/gemma-3-27b-it", "google/gemini-2.0-flash-lite-001")
    } else {
        listOf("google/gemma-4-31b-it:free", "openai/gpt-4.1-mini", "google/gemini-2.0-flash-lite-001", "anthropic/claude-3.5-sonnet")
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Gold,
                unfocusedIndicatorColor = Color(0x663A2F00),
                focusedLabelColor = Gold,
                unfocusedLabelColor = Muted
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onModelChange(option)
                    expanded = false
                })
            }
        }
    }
}

private suspend fun translateSrt(
    source: String,
    config: TranslationConfig,
    onProgress: suspend (String, Float, String) -> Unit
): String {
    val blocks = parseSrt(source)
    require(blocks.isNotEmpty()) { "No SRT cues found." }

    val translated = mutableMapOf<Int, String>()
    val chunks = blocks.chunked(30)
    val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    chunks.forEachIndexed { chunkIndex, chunk ->
        val start = chunkIndex * 30 + 1
        val end = start + chunk.size - 1
        onProgress("Translating blocks $start-$end of ${blocks.size}...", chunkIndex.toFloat() / chunks.size, translatedPreview(blocks, translated))
        val response = callTranslationApi(client, config, chunk)
        response.forEach { (index, text) ->
            translated[index] = text
        }
        onProgress("Completed blocks $start-$end.", (chunkIndex + 1).toFloat() / chunks.size, translatedPreview(blocks, translated))
    }

    return blocks.mapIndexed { index, block ->
        val number = block.number.ifBlank { "${index + 1}" }
        val text = translated[index + 1]?.trim().takeUnless { it.isNullOrBlank() } ?: block.text
        "$number\n${block.timing}\n$text"
    }.joinToString("\n\n") + "\n"
}

private suspend fun callTranslationApi(
    client: OkHttpClient,
    config: TranslationConfig,
    blocks: List<SubtitleBlock>
): Map<Int, String> = withContext(Dispatchers.IO) {
    val items = JSONArray()
    blocks.forEachIndexed { offset, block ->
        items.put(JSONObject().put("index", offset + 1).put("text", block.text))
    }
    val prompt = """
        Translate every subtitle item to fluent, natural ${config.targetLanguage}.
        Preserve meaning, tone, and line breaks where useful.
        Return ONLY valid JSON in this exact shape: {"items":[{"index":1,"text":"..."}]}.
        Do not leave English text unless it is a proper name.
        Input:
        ${items}
    """.trimIndent()

    val bodyJson = JSONObject()
        .put("model", config.model)
        .put("temperature", 0.2)
        .put("messages", JSONArray()
            .put(JSONObject().put("role", "system").put("content", "You are a precise subtitle translator. Return JSON only."))
            .put(JSONObject().put("role", "user").put("content", prompt)))

    val requestBuilder = Request.Builder()
        .url(config.baseUrl.trimEnd('/') + "/chat/completions")
        .addHeader("Authorization", "Bearer ${config.apiKey}")
        .addHeader("Content-Type", "application/json")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))

    if (config.provider == "OpenRouter") {
        requestBuilder.addHeader("HTTP-Referer", "https://github.com/espitman/TextLens")
        requestBuilder.addHeader("X-Title", "TextLens Subtitle Translator Android")
    }

    client.newCall(requestBuilder.build()).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) error("API returned ${response.code}: ${raw.take(240)}")
        val content = JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        parseTranslatedItems(content)
    }
}

private fun parseTranslatedItems(content: String): Map<Int, String> {
    val json = JSONObject(content)
    val array = json.getJSONArray("items")
    return buildMap {
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            put(item.getInt("index"), item.getString("text"))
        }
    }
}

private fun parseSrt(source: String): List<SubtitleBlock> {
    return source.replace("\r\n", "\n")
        .trim()
        .split(Regex("\n{2,}"))
        .mapNotNull { raw ->
            val lines = raw.lines().filter { it.isNotBlank() }
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex < 0) return@mapNotNull null
            val number = lines.take(timingIndex).lastOrNull().orEmpty()
            val timing = lines[timingIndex]
            val text = lines.drop(timingIndex + 1).joinToString("\n").trim()
            if (text.isBlank()) null else SubtitleBlock(number, timing, text)
        }
}

private fun translatedPreview(blocks: List<SubtitleBlock>, translated: Map<Int, String>): String {
    return blocks.take(10).mapIndexed { index, block ->
        "${index + 1}: ${translated[index + 1] ?: block.text}"
    }.joinToString("\n\n")
}

private fun defaultOutputName(sourceName: String): String {
    val clean = sourceName.substringAfterLast('/').substringBeforeLast('.', sourceName)
    return "${clean}-fa.srt"
}

private fun normalizeYouTubeUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("youtu.be/") -> "https://$trimmed"
        trimmed.startsWith("youtube.com/") || trimmed.startsWith("www.youtube.com/") -> "https://$trimmed"
        else -> trimmed
    }
}

private fun extractYouTubeId(input: String): String {
    val patterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{6,})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{6,})"),
        Regex("/shorts/([A-Za-z0-9_-]{6,})"),
        Regex("/embed/([A-Za-z0-9_-]{6,})")
    )
    return patterns.firstNotNullOfOrNull { it.find(input)?.groupValues?.getOrNull(1) }.orEmpty()
}

private suspend fun downloadSubtitle(
    url: String,
    userAgent: String,
    referer: String
): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .addHeader("User-Agent", userAgent.ifBlank { "Mozilla/5.0" })
        .addHeader("Referer", referer)
        .get()
        .build()
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
        .newCall(request)
        .execute()
        .use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Download returned ${response.code}")
            body
        }
}

private const val FIRST_SRT_CLICK_SCRIPT = """
(function () {
  const isVisible = (el) => {
    const rect = el.getBoundingClientRect();
    const style = window.getComputedStyle(el);
    return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
  };
  const textOf = (el) => [
    el.innerText || '',
    el.textContent || '',
    el.getAttribute('aria-label') || '',
    el.getAttribute('title') || '',
    el.getAttribute('href') || '',
    el.getAttribute('download') || ''
  ].join(' ');
  const candidates = Array.from(document.querySelectorAll('a, button, [role="button"]'))
    .filter(isVisible)
    .map((el, index) => ({ el, index, text: textOf(el).replace(/\s+/g, ' ').trim() }))
    .filter((item) => /\bSRT\b/i.test(item.text));
  if (!candidates.length) {
    return 'not-found';
  }
  candidates[0].el.click();
  return 'clicked ' + candidates[0].text.slice(0, 80);
})();
"""
