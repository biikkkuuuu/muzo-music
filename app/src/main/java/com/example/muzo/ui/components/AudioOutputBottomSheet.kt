package com.example.muzo.ui.components

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

data class DiscoveredAudioDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isActive: Boolean,
    val isConnected: Boolean,
    val batteryPercent: Int? = null,
    val audioDeviceInfo: AudioDeviceInfo? = null
)

enum class DeviceType {
    PHONE_SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET,
    USB_AUDIO,
    CAST,
    OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var devices by remember { mutableStateOf<List<DiscoveredAudioDevice>>(emptyList()) }
    var currentVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var isDraggingVolume by remember { mutableStateOf(false) }

    fun refreshAudioDevices() {
        val list = mutableListOf<DiscoveredAudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var hasActiveBluetooth = false
            var hasActiveWired = false
            var hasActiveUsb = false

            for (dev in audioOutputs) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                        hasActiveBluetooth = true
                        list.add(
                            DiscoveredAudioDevice(
                                id = "bt_${dev.id}",
                                name = dev.productName?.toString()?.ifBlank { null } ?: "Bluetooth Audio",
                                type = DeviceType.BLUETOOTH,
                                isActive = true,
                                isConnected = true,
                                audioDeviceInfo = dev
                            )
                        )
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        hasActiveWired = true
                        list.add(
                            DiscoveredAudioDevice(
                                id = "wired_${dev.id}",
                                name = dev.productName?.toString()?.ifBlank { null } ?: "Wired Headphones",
                                type = DeviceType.WIRED_HEADSET,
                                isActive = true,
                                isConnected = true,
                                audioDeviceInfo = dev
                            )
                        )
                    }
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        hasActiveUsb = true
                        list.add(
                            DiscoveredAudioDevice(
                                id = "usb_${dev.id}",
                                name = dev.productName?.toString()?.ifBlank { null } ?: "USB DAC / Headset",
                                type = DeviceType.USB_AUDIO,
                                isActive = true,
                                isConnected = true,
                                audioDeviceInfo = dev
                            )
                        )
                    }
                }
            }

            val speakerActive = !hasActiveBluetooth && !hasActiveWired && !hasActiveUsb
            list.add(
                DiscoveredAudioDevice(
                    id = "builtin_speaker",
                    name = "This Device (Phone Speaker)",
                    type = DeviceType.PHONE_SPEAKER,
                    isActive = speakerActive,
                    isConnected = true
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val isBt = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
            @Suppress("DEPRECATION")
            val isWired = audioManager.isWiredHeadsetOn
            if (isBt) {
                list.add(DiscoveredAudioDevice("bt_legacy", "Bluetooth Audio", DeviceType.BLUETOOTH, isActive = true, isConnected = true))
            }
            if (isWired) {
                list.add(DiscoveredAudioDevice("wired_legacy", "Wired Headphones", DeviceType.WIRED_HEADSET, isActive = !isBt, isConnected = true))
            }
            list.add(DiscoveredAudioDevice("speaker_legacy", "This Device", DeviceType.PHONE_SPEAKER, isActive = !isBt && !isWired, isConnected = true))
        }

        // Bluetooth bonded devices if permission granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBtPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (hasBtPerm) {
                try {
                    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val bonded = btManager?.adapter?.bondedDevices.orEmpty()
                    for (bDev in bonded) {
                        if (list.none { it.name == bDev.name }) {
                            list.add(
                                DiscoveredAudioDevice(
                                    id = "bonded_${bDev.address}",
                                    name = bDev.name ?: "Bluetooth Device",
                                    type = DeviceType.BLUETOOTH,
                                    isActive = false,
                                    isConnected = false
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        devices = list
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshAudioDevices()
    }

    // Hardware volume listener and audio device listener
    DisposableEffect(Unit) {
        refreshAudioDevices()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    if (!isDraggingVolume) {
                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                }
            }
        }

        val deviceReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                refreshAudioDevices()
            }
        }

        try {
            context.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
            val dFilter = IntentFilter().apply {
                addAction(AudioManager.ACTION_HEADSET_PLUG)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            context.registerReceiver(deviceReceiver, dFilter)
        } catch (_: Exception) {}

        val handler = Handler(Looper.getMainLooper())
        val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    refreshAudioDevices()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    refreshAudioDevices()
                }
            }
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        }

        onDispose {
            try {
                context.unregisterReceiver(volumeReceiver)
                context.unregisterReceiver(deviceReceiver)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
                    audioManager.unregisterAudioDeviceCallback(deviceCallback)
                }
            } catch (_: Exception) {}
        }
    }

    val activeDevice = devices.firstOrNull { it.isActive } ?: devices.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14131C),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // AirPlay-style Header Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated Pulse Ring
                    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
                    val waveScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "waveScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF4A80F0),
                                        Color(0xFF263352)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = waveScale
                                    scaleY = waveScale
                                }
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                        Icon(
                            imageVector = when (activeDevice?.type) {
                                DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
                                DeviceType.WIRED_HEADSET -> Icons.Default.Headphones
                                DeviceType.USB_AUDIO -> Icons.Default.Usb
                                else -> Icons.Default.Speaker
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Playing Audio On",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4ADE80))
                            )
                        }
                        Text(
                            text = activeDevice?.name ?: "Speaker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Media Volume Drag Control
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Output Volume",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${((currentVolume / maxVolume) * 100).roundToInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B9DFE)
                        )
                    }

                    Slider(
                        value = currentVolume,
                        onValueChange = {
                            isDraggingVolume = true
                            currentVolume = it
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0)
                        },
                        onValueChangeFinished = {
                            isDraggingVolume = false
                        },
                        valueRange = 0f..maxVolume.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF4A80F0),
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            // Available Output Devices List
            Text(
                text = "Available Routes",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devices.forEach { device ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (device.isActive) Color(0xFF232D48) else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.audioDeviceInfo != null) {
                                    try {
                                        audioManager.setCommunicationDevice(device.audioDeviceInfo)
                                    } catch (_: Exception) {}
                                }
                                refreshAudioDevices()
                            }
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (device.isActive) Color(0xFF4A80F0).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val icon: ImageVector = when (device.type) {
                                DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
                                DeviceType.WIRED_HEADSET -> Icons.Default.Headphones
                                DeviceType.USB_AUDIO -> Icons.Default.Usb
                                else -> Icons.Default.PhoneAndroid
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (device.isActive) Color(0xFF4A80F0).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (device.isActive) Color(0xFF6B9DFE) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (device.isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (device.isActive) "Active Route" else if (device.isConnected) "Connected" else "Paired",
                                    fontSize = 12.sp,
                                    color = if (device.isActive) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.45f)
                                )
                            }

                            if (device.isActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF4A80F0),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bluetooth Settings Shortcut Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                        }
                    }
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsBluetooth,
                            contentDescription = null,
                            tint = Color(0xFF6B9DFE),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Pair New Bluetooth Device",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
