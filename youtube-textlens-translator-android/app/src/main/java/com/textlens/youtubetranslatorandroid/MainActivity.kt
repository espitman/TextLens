package com.textlens.youtubetranslatorandroid

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
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
    var sourceText by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("No SRT selected") }
    var translatedText by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf("Persian preview will appear here.") }
    var logs by remember { mutableStateOf("Ready.") }
    var isTranslating by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var provider by remember { mutableStateOf("OpenRouter") }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://openrouter.ai/api/v1") }
    var model by remember { mutableStateOf("google/gemma-4-31b-it:free") }
    var targetLanguage by remember { mutableStateOf("Persian") }
    var pendingSave by remember { mutableStateOf("") }

    val openSrt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            sourceText = text
            sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "subtitle.srt"
            val count = parseSrt(text).size
            logs = "Loaded $sourceName\n$count subtitle cues detected."
            preview = text.lines().take(18).joinToString("\n").ifBlank { "Empty subtitle file." }
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
                    Header()

                    Card {
                        Text("Load and translate SRT", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text(sourceName, color = Muted, fontSize = 14.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoldButton("Choose SRT", enabled = !isTranslating) { openSrt.launch(arrayOf("application/x-subrip", "text/*", "*/*")) }
                            GoldButton("Translate", enabled = sourceText.isNotBlank() && apiKey.isNotBlank() && !isTranslating) {
                                scope.launch {
                                    isTranslating = true
                                    progress = 0f
                                    logs = "Preparing subtitle blocks..."
                                    val config = TranslationConfig(provider, apiKey, baseUrl, model, targetLanguage)
                                    try {
                                        translatedText = translateSrt(sourceText, config) { message, value, livePreview ->
                                            logs = message
                                            progress = value
                                            if (livePreview.isNotBlank()) preview = livePreview
                                        }
                                        pendingSave = translatedText
                                        preview = translatedText.lines().takeLast(24).joinToString("\n")
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
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(progress = { progress }, color = Gold, trackColor = Color(0x443A2F00), modifier = Modifier.size(36.dp))
                                Text("${(progress * 100).toInt()}%", color = GoldSoft, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Card {
                        Text("API provider", color = Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Toggle("OpenRouter", provider == "OpenRouter") {
                                provider = "OpenRouter"
                                baseUrl = "https://openrouter.ai/api/v1"
                                model = "google/gemma-4-31b-it:free"
                            }
                            Toggle("Liara", provider == "Liara") {
                                provider = "Liara"
                                baseUrl = "https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1"
                                model = "openai/gpt-5-nano"
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        AppTextField("API key", apiKey, { apiKey = it }, password = true)
                        AppTextField("Base URL", baseUrl, { baseUrl = it })
                        ModelPicker(provider, model) { model = it }
                        AppTextField("Target language", targetLanguage, { targetLanguage = it })
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(modifier = Modifier.weight(1f)) {
                            Text("Translation logs", color = Gold, fontWeight = FontWeight.Black)
                            Text(logs, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Text("Persian preview", color = Gold, fontWeight = FontWeight.Black)
                            Text(preview, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Right, lineHeight = 30.sp, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Gold, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("T", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 36.sp)
            }
            Column {
                Text("YouTube TextLens Translator", color = Color.White, fontWeight = FontWeight.Black, fontSize = 27.sp)
                Text("Offline SRT loading. AI Persian translation. Android edition.", color = Muted, fontWeight = FontWeight.Bold)
            }
        }
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
private fun Toggle(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (active) Gold else Color(0xFF29261B), contentColor = if (active) Color.Black else Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (active) Gold else Color(0x664D3D00))
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
