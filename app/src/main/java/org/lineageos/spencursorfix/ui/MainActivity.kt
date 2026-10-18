package org.lineageos.spencursorfix.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import java.io.File
import android.view.PointerIcon
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon as ComposePointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.lineageos.spencursorfix.DotIconGenerator
import org.lineageos.spencursorfix.Prefs
import org.lineageos.spencursorfix.ui.theme.SPenCursorFixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SPenCursorFixTheme {
                MainScreen()
            }
        }
    }
}

data class ColorOption(val name: String, val hex: String)

val PRESET_COLORS = listOf(
    ColorOption("OneUI Cyan", "#B6E5EA"),
    ColorOption("Pure White", "#FFFFFF"),
    ColorOption("Pure Black", "#000000"),
    ColorOption("Deep Blue", "#0072DE"),
    ColorOption("Teal", "#00BCD4"),
    ColorOption("Neon Purple", "#9C27B0"),
    ColorOption("Vibrant Amber", "#FF9800")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val sharedPrefs: SharedPreferences = remember {
        try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(Prefs.PREF_FILE, Context.MODE_WORLD_READABLE)
        } catch (e: Throwable) {
            context.getSharedPreferences(Prefs.PREF_FILE, Context.MODE_PRIVATE)
        }
    }

    var enabled by remember {
        mutableStateOf(sharedPrefs.getBoolean(Prefs.PREF_ENABLED, true))
    }
    var selectedColor by remember {
        mutableStateOf(sharedPrefs.getString(Prefs.PREF_DOT_COLOR, Prefs.DEFAULT_COLOR) ?: Prefs.DEFAULT_COLOR)
    }
    var selectedRadius by remember {
        val savedRadius = sharedPrefs.getString(Prefs.PREF_DOT_RADIUS, Prefs.DEFAULT_RADIUS.toString())
        mutableStateOf(savedRadius?.toFloatOrNull() ?: Prefs.DEFAULT_RADIUS)
    }
    var targetDeviceName by remember {
        mutableStateOf(sharedPrefs.getString(Prefs.PREF_TARGET_NAME, Prefs.DEFAULT_TARGET_NAME) ?: Prefs.DEFAULT_TARGET_NAME)
    }
    var verboseLogging by remember {
        mutableStateOf(sharedPrefs.getBoolean(Prefs.PREF_DEBUG_LOG, false))
    }

    var showDeviceDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    fun sendUpdateBroadcast(
        isEnabled: Boolean = enabled,
        color: String = selectedColor,
        radius: Float = selectedRadius,
        target: String = targetDeviceName,
        debug: Boolean = verboseLogging
    ) {
        try {
            val intent = Intent("org.lineageos.spencursorfix.UPDATE_PREFS").apply {
                setPackage("android")
                putExtra(Prefs.PREF_ENABLED, isEnabled)
                putExtra(Prefs.PREF_DOT_COLOR, color)
                putExtra(Prefs.PREF_DOT_RADIUS, radius)
                putExtra(Prefs.PREF_TARGET_NAME, target)
                putExtra(Prefs.PREF_DEBUG_LOG, debug)
            }
            context.sendBroadcast(intent)
        } catch (e: Throwable) {
            try {
                val intent = Intent("org.lineageos.spencursorfix.UPDATE_PREFS").apply {
                    putExtra(Prefs.PREF_ENABLED, isEnabled)
                    putExtra(Prefs.PREF_DOT_COLOR, color)
                    putExtra(Prefs.PREF_DOT_RADIUS, radius)
                    putExtra(Prefs.PREF_TARGET_NAME, target)
                    putExtra(Prefs.PREF_DEBUG_LOG, debug)
                }
                context.sendBroadcast(intent)
            } catch (ignored: Throwable) {}
        }
    }

    fun fixFilePermissions() {
        try {
            val dataDir = File(context.applicationInfo.dataDir)
            val prefsDir = File(dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "${Prefs.PREF_FILE}.xml")
            dataDir.setReadable(true, false)
            dataDir.setExecutable(true, false)
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false)
                prefsDir.setExecutable(true, false)
            }
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
            }
        } catch (ignored: Throwable) {}
    }

    LaunchedEffect(Unit) {
        fixFilePermissions()
        sendUpdateBroadcast()
    }

    fun saveBoolean(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).commit()
        fixFilePermissions()
        val curEnabled = if (key == Prefs.PREF_ENABLED) value else enabled
        val curDebug = if (key == Prefs.PREF_DEBUG_LOG) value else verboseLogging
        sendUpdateBroadcast(isEnabled = curEnabled, debug = curDebug)
    }

    fun saveString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).commit()
        fixFilePermissions()
        val curColor = if (key == Prefs.PREF_DOT_COLOR) value else selectedColor
        val curRadius = if (key == Prefs.PREF_DOT_RADIUS) (value.toFloatOrNull() ?: selectedRadius) else selectedRadius
        val curTarget = if (key == Prefs.PREF_TARGET_NAME) value else targetDeviceName
        sendUpdateBroadcast(color = curColor, radius = curRadius, target = curTarget)
    }

    val systemPointerIcon = remember(selectedColor, selectedRadius) {
        DotIconGenerator.getOrCreateDot(selectedColor, selectedRadius)
    }
    val composePointerIcon = remember(systemPointerIcon) {
        ComposePointerIcon(systemPointerIcon)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "S Pen Cursor Fix",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Material 3 Expressive Live Interactive Preview Card
            item {
                InteractivePreviewCard(
                    colorHex = selectedColor,
                    radiusDp = selectedRadius,
                    pointerIcon = composePointerIcon
                )
            }

            // 2. General Section (Master Toggle)
            item {
                SectionHeader("General")
            }
            item {
                ExpressiveCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable S Pen Cursor Fix",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Replaces mouse arrow with OneUI hover dot",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { isChecked ->
                                enabled = isChecked
                                saveBoolean(Prefs.PREF_ENABLED, isChecked)
                            },
                            thumbContent = if (enabled) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // 3. Appearance Section (Color & Size)
            item {
                SectionHeader("Appearance")
            }
            item {
                ExpressiveCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hover Dot Color
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hover Dot Color",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                val dynamicSystemColor = MaterialTheme.colorScheme.primary
                                val isDynamicSelected = selectedColor.equals("dynamic", ignoreCase = true)
                                Text(
                                    text = when {
                                        isDynamicSelected -> "System Material You Accent"
                                        PRESET_COLORS.any { it.hex.equals(selectedColor, ignoreCase = true) } ->
                                            PRESET_COLORS.find { it.hex.equals(selectedColor, ignoreCase = true) }?.name ?: selectedColor
                                        else -> "Custom ($selectedColor)"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Color selection chips / swatches (Color Picker is first option, then Material You, then presets)
                        val dynamicSystemColor = MaterialTheme.colorScheme.primary
                        val isDynamicSelected = selectedColor.equals("dynamic", ignoreCase = true)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Color Picker as first option
                            item {
                                val isCustomSelected = !isDynamicSelected && PRESET_COLORS.none { it.hex.equals(selectedColor, ignoreCase = true) }
                                ColorPickerChip(
                                    currentColorHex = if (isDynamicSelected) dynamicSystemColor.toHex() else selectedColor,
                                    isSelected = isCustomSelected,
                                    onClick = { showColorPickerDialog = true }
                                )
                            }

                            // 2. System Material You Dynamic Accent Option
                            item {
                                SystemAccentChip(
                                    accentColor = dynamicSystemColor,
                                    isSelected = isDynamicSelected,
                                    onClick = {
                                        selectedColor = "dynamic"
                                        saveString(Prefs.PREF_DOT_COLOR, "dynamic")
                                    }
                                )
                            }

                            // 3. Preset colors (Default Cyan is first preset, followed by others)
                            items(PRESET_COLORS) { option ->
                                ColorChip(
                                    option = option,
                                    isSelected = !isDynamicSelected && option.hex.equals(selectedColor, ignoreCase = true),
                                    onClick = {
                                        selectedColor = option.hex
                                        saveString(Prefs.PREF_DOT_COLOR, option.hex)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Dot Size Header with value and reset button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dot Size",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (selectedRadius == 4.0f) "4.0 dp (OneUI Default)" else String.format(Locale.US, "%.1f dp", selectedRadius),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (selectedRadius != 4.0f) {
                                TextButton(
                                    onClick = {
                                        selectedRadius = 4.0f
                                        saveString(Prefs.PREF_DOT_RADIUS, "4.0")
                                    }
                                ) {
                                    Text("Reset to 4.0 dp")
                                }
                            }
                        }

                        // Dot Size Slider with proportional min/mid/max markers
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Slider(
                                value = selectedRadius,
                                onValueChange = { newRadius ->
                                    val rounded = kotlin.math.round(newRadius * 2f) / 2f
                                    selectedRadius = rounded
                                    saveString(Prefs.PREF_DOT_RADIUS, String.format(Locale.US, "%.1f", rounded))
                                },
                                valueRange = 2.0f..10.0f,
                                steps = 15,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "2.0 dp (Min)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "6.0 dp (Mid)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "10.0 dp (Max)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Advanced Section
            item {
                SectionHeader("Advanced")
            }
            item {
                ExpressiveCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Target Device Name Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeviceDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target Device Name",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = targetDeviceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showDeviceDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit target device name",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Verbose Logging Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verbose Logging",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Output device resolution logs to logcat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = verboseLogging,
                                onCheckedChange = { isChecked ->
                                    verboseLogging = isChecked
                                    saveBoolean(Prefs.PREF_DEBUG_LOG, isChecked)
                                },
                                thumbContent = if (verboseLogging) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Material 3 Expressive Target Device Input Dialog
    if (showDeviceDialog) {
        TargetDeviceDialog(
            currentName = targetDeviceName,
            onDismiss = { showDeviceDialog = false },
            onSave = { newName ->
                targetDeviceName = newName
                saveString(Prefs.PREF_TARGET_NAME, newName)
                showDeviceDialog = false
            }
        )
    }

    if (showColorPickerDialog) {
        ColorPickerDialog(
            initialColorHex = selectedColor,
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { newHex ->
                selectedColor = newHex
                saveString(Prefs.PREF_DOT_COLOR, newHex)
                showColorPickerDialog = false
            }
        )
    }
}

@Composable
fun InteractivePreviewCard(
    colorHex: String,
    radiusDp: Float,
    pointerIcon: ComposePointerIcon
) {
    val dynamicPrimary = MaterialTheme.colorScheme.primary
    val parsedColor = remember(colorHex, dynamicPrimary) {
        if ("dynamic".equals(colorHex, ignoreCase = true) || "system".equals(colorHex, ignoreCase = true)) {
            dynamicPrimary
        } else {
            try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                Color(0xFFB6E5EA)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(pointerIcon), // Active live pen hover updates in real time!
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic canvas drawing the exact S Pen OneUI hover ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)
                        val ratio = radiusDp / 4.0f
                        val outerBorderOuterR = 4.001f.dp.toPx() * ratio
                        val outerBorderInnerR = 3.250f.dp.toPx() * ratio
                        val innerRingOuterR = 3.611f.dp.toPx() * ratio
                        val innerRingInnerR = 3.100f.dp.toPx() * ratio

                        val borderCenterR = (outerBorderOuterR + outerBorderInnerR) / 2f
                        val borderStrokeWidth = kotlin.math.max(1f, outerBorderOuterR - outerBorderInnerR)

                        val ringCenterR = (innerRingOuterR + innerRingInnerR) / 2f
                        val ringStrokeWidth = kotlin.math.max(1f, innerRingOuterR - innerRingInnerR)

                        // 1. Subtle drop halo
                        drawCircle(
                            color = Color(0x28000000),
                            radius = outerBorderOuterR,
                            center = centerOffset,
                            style = Stroke(width = kotlin.math.max(1f, 0.75f.dp.toPx()))
                        )
                        // 2. Outer grey boundary (#9EA0A0)
                        drawCircle(
                            color = Color(0xFF9EA0A0),
                            radius = borderCenterR,
                            center = centerOffset,
                            style = Stroke(width = borderStrokeWidth)
                        )
                        // 3. Inner accent ring (live selected color)
                        drawCircle(
                            color = parsedColor,
                            radius = ringCenterR,
                            center = centerOffset,
                            style = Stroke(width = ringStrokeWidth)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "S Pen Cursor Preview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ColorChip(
    option: ColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = remember(option.hex) {
        try {
            Color(android.graphics.Color.parseColor(option.hex))
        } catch (e: Exception) {
            Color(0xFFB6E5EA)
        }
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "borderColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.5.dp, animatedBorderColor, CircleShape)
                .border(1.dp, Color(0x33000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isColorLight(color)) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = option.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun isColorLight(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.6f
}

@Composable
fun ColorPickerChip(
    currentColorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val parsedColor = remember(currentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(currentColorHex))
        } catch (e: Exception) {
            Color(0xFFB6E5EA)
        }
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "pickerBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) parsedColor else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(2.5.dp, animatedBorderColor, CircleShape)
                .border(1.dp, Color(0x33000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Custom Color Picker",
                tint = if (isSelected) {
                    if (isColorLight(parsedColor)) Color.Black else Color.White
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Custom",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun Color.toHex(): String {
    val r = (this.red * 255f).toInt().coerceIn(0, 255)
    val g = (this.green * 255f).toInt().coerceIn(0, 255)
    val b = (this.blue * 255f).toInt().coerceIn(0, 255)
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}

@Composable
fun SystemAccentChip(
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "systemAccentBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(2.5.dp, animatedBorderColor, CircleShape)
                .border(1.dp, Color(0x33000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isColorLight(accentColor)) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "System Material You Accent",
                    tint = if (isColorLight(accentColor)) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Material You",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val dynamicPrimary = MaterialTheme.colorScheme.primary
    val parsedInitial = remember(initialColorHex, dynamicPrimary) {
        if ("dynamic".equals(initialColorHex, ignoreCase = true) || "system".equals(initialColorHex, ignoreCase = true)) {
            android.graphics.Color.parseColor(dynamicPrimary.toHex())
        } else {
            try {
                android.graphics.Color.parseColor(initialColorHex)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#B6E5EA")
            }
        }
    }

    var red by remember { mutableFloatStateOf(android.graphics.Color.red(parsedInitial).toFloat()) }
    var green by remember { mutableFloatStateOf(android.graphics.Color.green(parsedInitial).toFloat()) }
    var blue by remember { mutableFloatStateOf(android.graphics.Color.blue(parsedInitial).toFloat()) }
    var hexInput by remember {
        mutableStateOf(String.format(Locale.US, "#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt()))
    }

    fun updateHex(r: Int, g: Int, b: Int) {
        hexInput = String.format(Locale.US, "#%02X%02X%02X", r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    val currentColor = Color(red.toInt().coerceIn(0, 255), green.toInt().coerceIn(0, 255), blue.toInt().coerceIn(0, 255))

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Custom Hover Color",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Color preview banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(1.dp, Color(0x33000000), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hexInput,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isColorLight(currentColor)) Color.Black else Color.White
                    )
                }

                // Red Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Red",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "${red.toInt().coerceIn(0, 255)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Slider(
                        value = red,
                        onValueChange = {
                            red = it
                            updateHex(it.toInt(), green.toInt(), blue.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE53935),
                            activeTrackColor = Color(0xFFE53935)
                        )
                    )
                }

                // Green Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Green",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF43A047)
                        )
                        Text(
                            text = "${green.toInt().coerceIn(0, 255)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Slider(
                        value = green,
                        onValueChange = {
                            green = it
                            updateHex(red.toInt(), it.toInt(), blue.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF43A047),
                            activeTrackColor = Color(0xFF43A047)
                        )
                    )
                }

                // Blue Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Blue",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1E88E5)
                        )
                        Text(
                            text = "${blue.toInt().coerceIn(0, 255)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Slider(
                        value = blue,
                        onValueChange = {
                            blue = it
                            updateHex(red.toInt(), green.toInt(), it.toInt())
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1E88E5),
                            activeTrackColor = Color(0xFF1E88E5)
                        )
                    )
                }

                // Hex input field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val clean = raw.trim()
                        hexInput = clean
                        try {
                            val parseTarget = if (clean.startsWith("#")) clean else "#$clean"
                            if (parseTarget.length == 7 || parseTarget.length == 9) {
                                val parsed = android.graphics.Color.parseColor(parseTarget)
                                red = android.graphics.Color.red(parsed).toFloat()
                                green = android.graphics.Color.green(parsed).toFloat()
                                blue = android.graphics.Color.blue(parsed).toFloat()
                            }
                        } catch (_: Exception) {
                            // In progress typing
                        }
                    },
                    label = { Text("Hex Code") },
                    placeholder = { Text("#B6E5EA") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        val defaultColor = android.graphics.Color.parseColor("#B6E5EA")
                        red = android.graphics.Color.red(defaultColor).toFloat()
                        green = android.graphics.Color.green(defaultColor).toFloat()
                        blue = android.graphics.Color.blue(defaultColor).toFloat()
                        updateHex(red.toInt(), green.toInt(), blue.toInt())
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset to default (Cyan)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = String.format(
                        Locale.US,
                        "#%02X%02X%02X",
                        red.toInt().coerceIn(0, 255),
                        green.toInt().coerceIn(0, 255),
                        blue.toInt().coerceIn(0, 255)
                    )
                    onColorSelected(finalHex)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun ExpressiveCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        )
    ) {
        content()
    }
}

@Composable
fun TargetDeviceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Target Device Name",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Specify the input device name to match for S Pen cursor replacement:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Device Name") },
                    placeholder = { Text("sec_e-pen") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { text = Prefs.DEFAULT_TARGET_NAME },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset to default (sec_e-pen)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text.trim()) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
