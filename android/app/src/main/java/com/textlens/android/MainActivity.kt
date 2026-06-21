package com.textlens.android

import android.os.Bundle
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.textlens.android.data.AndroidSettings
import com.textlens.android.data.ProviderSettings
import com.textlens.android.data.SettingsStore
import com.textlens.android.data.TranslationProvider
import com.textlens.android.data.displayName
import com.textlens.android.data.modelOptions
import com.textlens.android.core.AndroidPermissionController
import com.textlens.android.core.AndroidPermissionState
import com.textlens.android.core.FloatingBubbleService
import com.textlens.android.core.MediaProjectionSession
import com.textlens.android.core.ScreenCaptureGrant
import com.textlens.android.core.screenCaptureGrantOrNull
import com.textlens.android.ui.TextLensAndroidTheme
import kotlinx.coroutines.launch

private val AppBlack = Color(0xFF070707)
private val Panel = Color(0xFF11100D)
private val Gold = Color(0xFFF5C84A)
private val TextPrimary = Color(0xFFF7F0DC)
private val TextMuted = Color(0xA6F7F0DC)

class MainActivity : ComponentActivity() {
    private lateinit var permissionController: AndroidPermissionController
    private var screenCaptureGrant: ScreenCaptureGrant? = null
    private var screenCaptureMessage = "Screen capture permission has not been requested yet."
    private var permissionState by mutableStateOf(AndroidPermissionState())

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        screenCaptureGrant = screenCaptureGrantOrNull(result.resultCode, result.data)
        MediaProjectionSession.update(result.resultCode, result.data)
        screenCaptureMessage = if (screenCaptureGrant != null) {
            "Screen capture permission is ready for this app session."
        } else {
            "Screen capture permission was denied or cancelled."
        }
        refreshPermissionState()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionController = AndroidPermissionController(this)
        refreshPermissionState()

        setContent {
            TextLensAndroidTheme {
                val context = LocalContext.current
                val settingsStore = remember { SettingsStore(context.applicationContext) }
                val settings by settingsStore.settings.collectAsState(initial = AndroidSettings())
                val scope = rememberCoroutineScope()

                TextLensSettingsApp(
                    settings = settings,
                    permissionState = permissionState,
                    onOpenOverlaySettings = {
                        startActivity(permissionController.overlaySettingsIntent())
                    },
                    onRequestScreenCapture = {
                        screenCaptureLauncher.launch(permissionController.screenCaptureIntent())
                    },
                    onRequestNotifications = {
                        permissionController.notificationPermission()?.let(notificationPermissionLauncher::launch)
                    },
                    onStartBubble = {
                        val intent = Intent(this, FloatingBubbleService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    },
                    onStopBubble = {
                        stopService(Intent(this, FloatingBubbleService::class.java))
                    },
                    onSave = { updatedSettings ->
                        scope.launch {
                            settingsStore.save(updatedSettings)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionController.isInitialized) {
            refreshPermissionState()
        }
    }

    private fun refreshPermissionState() {
        permissionState = permissionController.currentState(
            screenCaptureGrant = screenCaptureGrant,
            screenCaptureMessage = screenCaptureMessage,
        )
    }
}

@Composable
private fun TextLensSettingsApp(
    settings: AndroidSettings,
    permissionState: AndroidPermissionState,
    onOpenOverlaySettings: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onRequestNotifications: () -> Unit,
    onStartBubble: () -> Unit,
    onStopBubble: () -> Unit,
    onSave: (AndroidSettings) -> Unit,
) {
    var selectedProvider by remember { mutableStateOf(settings.provider) }
    var openRouterApiKey by remember { mutableStateOf(settings.openRouter.apiKey) }
    var openRouterBaseUrl by remember { mutableStateOf(settings.openRouter.baseUrl) }
    var openRouterModel by remember { mutableStateOf(settings.openRouter.model) }
    var liaraApiKey by remember { mutableStateOf(settings.liara.apiKey) }
    var liaraBaseUrl by remember { mutableStateOf(settings.liara.baseUrl) }
    var liaraModel by remember { mutableStateOf(settings.liara.model) }
    var targetLanguage by remember { mutableStateOf(settings.targetLanguage) }
    var savedMessage by remember { mutableStateOf("") }

    LaunchedEffect(settings) {
        selectedProvider = settings.provider
        openRouterApiKey = settings.openRouter.apiKey
        openRouterBaseUrl = settings.openRouter.baseUrl
        openRouterModel = settings.openRouter.model
        liaraApiKey = settings.liara.apiKey
        liaraBaseUrl = settings.liara.baseUrl
        liaraModel = settings.liara.model
        targetLanguage = settings.targetLanguage
    }

    val currentApiKey = when (selectedProvider) {
        TranslationProvider.OpenRouter -> openRouterApiKey
        TranslationProvider.Liara -> liaraApiKey
    }
    val currentBaseUrl = when (selectedProvider) {
        TranslationProvider.OpenRouter -> openRouterBaseUrl
        TranslationProvider.Liara -> liaraBaseUrl
    }
    val currentModel = when (selectedProvider) {
        TranslationProvider.OpenRouter -> openRouterModel
        TranslationProvider.Liara -> liaraModel
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBlack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF050505),
                            Color(0xFF151006),
                            Color(0xFF070707),
                        ),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header()
            FirstRunStatusCard(
                provider = selectedProvider,
                isReady = currentApiKey.isNotBlank(),
            )
            PermissionPanel(
                permissionState = permissionState,
                onOpenOverlaySettings = onOpenOverlaySettings,
                onRequestScreenCapture = onRequestScreenCapture,
                onRequestNotifications = onRequestNotifications,
                onStartBubble = onStartBubble,
                onStopBubble = onStopBubble,
            )
            SettingsPanel(
                selectedProvider = selectedProvider,
                apiKey = currentApiKey,
                baseUrl = currentBaseUrl,
                model = currentModel,
                targetLanguage = targetLanguage,
                savedMessage = savedMessage,
                onProviderChange = {
                    selectedProvider = it
                    savedMessage = ""
                },
                onApiKeyChange = {
                    when (selectedProvider) {
                        TranslationProvider.OpenRouter -> openRouterApiKey = it
                        TranslationProvider.Liara -> liaraApiKey = it
                    }
                    savedMessage = ""
                },
                onBaseUrlChange = {
                    when (selectedProvider) {
                        TranslationProvider.OpenRouter -> openRouterBaseUrl = it
                        TranslationProvider.Liara -> liaraBaseUrl = it
                    }
                    savedMessage = ""
                },
                onModelChange = {
                    when (selectedProvider) {
                        TranslationProvider.OpenRouter -> openRouterModel = it
                        TranslationProvider.Liara -> liaraModel = it
                    }
                    savedMessage = ""
                },
                onTargetLanguageChange = {
                    targetLanguage = it
                    savedMessage = ""
                },
                onSave = {
                    onSave(
                        AndroidSettings(
                            provider = selectedProvider,
                            openRouter = ProviderSettings(
                                apiKey = openRouterApiKey.trim(),
                                baseUrl = openRouterBaseUrl.trim(),
                                model = openRouterModel.trim(),
                            ),
                            liara = ProviderSettings(
                                apiKey = liaraApiKey.trim(),
                                baseUrl = liaraBaseUrl.trim(),
                                model = liaraModel.trim(),
                            ),
                            targetLanguage = targetLanguage.trim().ifBlank { "Persian" },
                        ),
                    )
                    savedMessage = "Settings saved"
                },
            )
        }
    }
}

@Composable
private fun Header() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextLensMark()
        Column {
            Text(
                text = "TextLens",
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Android floating bubble translator",
                color = TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun FirstRunStatusCard(
    provider: TranslationProvider,
    isReady: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (isReady) "Ready for ${provider.displayName}" else "Add an API key to start",
            color = if (isReady) Gold else TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Phase 1 sets up provider settings. The floating bubble, permissions, OCR, and capture flow arrive in the next phases.",
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun PermissionPanel(
    permissionState: AndroidPermissionState,
    onOpenOverlaySettings: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onRequestNotifications: () -> Unit,
    onStartBubble: () -> Unit,
    onStopBubble: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Panel.copy(alpha = 0.90f))
            .border(1.dp, Gold.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Permission Flow",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "TextLens needs overlay access for the floating bubble and screen capture access for selected-area OCR. Capture permission is kept only for the active app session.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

        PermissionRow(
            title = "Draw over other apps",
            description = "Required for the floating bubble and selection overlay.",
            granted = permissionState.overlayGranted,
            actionLabel = "Open Settings",
            onAction = onOpenOverlaySettings,
        )
        PermissionRow(
            title = "Screen capture",
            description = permissionState.screenCaptureMessage,
            granted = permissionState.screenCaptureGranted,
            actionLabel = "Request",
            onAction = onRequestScreenCapture,
        )
        PermissionRow(
            title = "Notifications",
            description = "Used only when the bubble service needs a foreground notification.",
            granted = permissionState.notificationsGranted,
            actionLabel = "Allow",
            onAction = onRequestNotifications,
        )

        if (permissionState.allReady) {
            Text(
                text = "All required permissions are ready.",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = AppBlack,
                ),
                shape = RoundedCornerShape(15.dp),
                onClick = onStartBubble,
            ) {
                Text("Start Bubble", fontWeight = FontWeight.Bold)
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.10f),
                    contentColor = TextPrimary,
                ),
                shape = RoundedCornerShape(15.dp),
                onClick = onStopBubble,
            ) {
                Text("Stop", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.26f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (granted) Gold.copy(alpha = 0.18f) else Color(0xFFFF4D4D).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (granted) "✓" else "!",
                color = if (granted) Gold else Color(0xFFFF6B6B),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }

        if (!granted) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onAction() }
                    .background(Gold)
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                text = actionLabel,
                color = AppBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    selectedProvider: TranslationProvider,
    apiKey: String,
    baseUrl: String,
    model: String,
    targetLanguage: String,
    savedMessage: String,
    onProviderChange: (TranslationProvider) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Panel.copy(alpha = 0.96f))
            .border(1.dp, Gold.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProviderSelector(
            selectedProvider = selectedProvider,
            onProviderChange = onProviderChange,
        )

        SettingsField(
            label = "API Key",
            value = apiKey,
            onValueChange = onApiKeyChange,
            isSecret = true,
        )
        SettingsField(
            label = "Base URL",
            value = baseUrl,
            onValueChange = onBaseUrlChange,
        )
        ModelField(
            provider = selectedProvider,
            model = model,
            onModelChange = onModelChange,
        )
        SettingsField(
            label = "Target Language",
            value = targetLanguage,
            onValueChange = onTargetLanguageChange,
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = AppBlack,
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = onSave,
        ) {
            Text(
                text = "Save Settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (savedMessage.isNotBlank()) {
            Text(
                text = savedMessage,
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProviderSelector(
    selectedProvider: TranslationProvider,
    onProviderChange: (TranslationProvider) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TranslationProvider.entries.forEach { provider ->
            val selected = provider == selectedProvider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Gold else Color.Transparent)
                    .clickable { onProviderChange(provider) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = provider.displayName,
                    color = if (selected) AppBlack else TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSecret: Boolean = false,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = fieldColors(),
        shape = RoundedCornerShape(15.dp),
    )
}

@Composable
private fun ModelField(
    provider: TranslationProvider,
    model: String,
    onModelChange: (String) -> Unit,
) {
    var expanded by remember(provider) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsField(
            label = "Model",
            value = model,
            onValueChange = onModelChange,
        )

        Box {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { expanded = true }
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                text = "Choose ${provider.displayName} model",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                provider.modelOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onModelChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold,
    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
    focusedLabelColor = Gold,
    unfocusedLabelColor = TextMuted,
    cursorColor = Gold,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)

@Composable
private fun TextLensMark() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Gold.copy(alpha = 0.38f), RoundedCornerShape(18.dp)),
    ) {
        Text(
            text = "T",
            modifier = Modifier.align(Alignment.Center),
            color = Gold,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(AppBlack),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Gold),
            )
        }
    }
}

@Preview
@Composable
private fun TextLensAppPreview() {
    TextLensAndroidTheme {
        TextLensSettingsApp(
            settings = AndroidSettings(),
            permissionState = AndroidPermissionState(),
            onOpenOverlaySettings = {},
            onRequestScreenCapture = {},
            onRequestNotifications = {},
            onStartBubble = {},
            onStopBubble = {},
            onSave = {},
        )
    }
}
