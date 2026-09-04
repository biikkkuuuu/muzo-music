package com.example.muzo.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-fidelity Equalizer Controller utilizing Android hardware audiofx effects:
 * - 5-Band Audio Equalizer
 * - Bass Boost
 * - 3D Soundstage Virtualizer
 */
class EqualizerController(private val context: Context) {

    private val prefs = context.getSharedPreferences("muzi_equalizer_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    private val _isEnabled = MutableStateFlow(prefs.getBoolean("eq_enabled", false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentPreset = MutableStateFlow(prefs.getString("eq_preset", "Flat") ?: "Flat")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(prefs.getInt("eq_bass_boost", 0))
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(prefs.getInt("eq_virtualizer", 0))
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    // 5 Bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz in millibels (-1200 to +1200 mB)
    private val defaultBandLevels = listOf(0, 0, 0, 0, 0)
    private val _bandLevels = MutableStateFlow(loadSavedBandLevels())
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    val presetMap = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(700, 450, 100, 0, -150),
        "Rock" to listOf(500, 250, -150, 250, 500),
        "Pop" to listOf(-150, 150, 450, 200, -100),
        "Electronic" to listOf(450, 200, 0, 250, 450),
        "Vocal" to listOf(-200, 200, 500, 300, 0),
        "Custom" to defaultBandLevels
    )

    fun attachAudioSession(sessionId: Int) {
        if (sessionId == 0 || sessionId == currentSessionId) return
        release()
        currentSessionId = sessionId

        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _isEnabled.value
            }

            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_bassBoostStrength.value.toShort())
                }
            }

            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = _isEnabled.value
                if (strengthSupported) {
                    setStrength(_virtualizerStrength.value.toShort())
                }
            }

            applyCurrentBands()
            Log.d("EqualizerController", "Attached audio effects to session $sessionId successfully")
        } catch (e: Exception) {
            Log.e("EqualizerController", "Failed to initialize AudioEffect: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs.edit().putBoolean("eq_enabled", enabled).apply()

        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            if (enabled) {
                applyCurrentBands()
                bassBoost?.setStrength(_bassBoostStrength.value.toShort())
                virtualizer?.setStrength(_virtualizerStrength.value.toShort())
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error toggling equalizer: ${e.message}")
        }
    }

    fun setPreset(presetName: String) {
        _currentPreset.value = presetName
        prefs.edit().putString("eq_preset", presetName).apply()

        val levels = presetMap[presetName] ?: defaultBandLevels
        if (presetName != "Custom") {
            _bandLevels.value = levels
            saveBandLevels(levels)
            applyCurrentBands()

            // When Bass Boost preset is chosen, also set bass boost slider to 50%
            if (presetName == "Bass Boost" && _bassBoostStrength.value < 400) {
                setBassBoost(500)
            }
        }
    }

    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val current = _bandLevels.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = levelMilliBels
            _bandLevels.value = current
            _currentPreset.value = "Custom"
            prefs.edit().putString("eq_preset", "Custom").apply()
            saveBandLevels(current)

            try {
                if (equalizer != null && bandIndex < equalizer!!.numberOfBands) {
                    val range = equalizer!!.bandLevelRange
                    val clamped = levelMilliBels.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                    equalizer?.setBandLevel(bandIndex.toShort(), clamped)
                }
            } catch (e: Exception) {
                Log.w("EqualizerController", "Error setting band level: ${e.message}")
            }
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        prefs.edit().putInt("eq_bass_boost", clamped).apply()

        try {
            if (_isEnabled.value && bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error setting bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _virtualizerStrength.value = clamped
        prefs.edit().putInt("eq_virtualizer", clamped).apply()

        try {
            if (_isEnabled.value && virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error setting virtualizer: ${e.message}")
        }
    }

    private fun applyCurrentBands() {
        val eq = equalizer ?: return
        val levels = _bandLevels.value
        try {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            for (i in 0 until minOf(numBands, levels.size)) {
                val clamped = levels[i].coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                eq.setBandLevel(i.toShort(), clamped)
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error applying band levels: ${e.message}")
        }
    }

    private fun loadSavedBandLevels(): List<Int> {
        val savedStr = prefs.getString("eq_band_levels", null) ?: return defaultBandLevels
        return try {
            savedStr.split(",").map { it.trim().toInt() }
        } catch (_: Exception) {
            defaultBandLevels
        }
    }

    private fun saveBandLevels(levels: List<Int>) {
        val str = levels.joinToString(",")
        prefs.edit().putString("eq_band_levels", str).apply()
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
    }
}
