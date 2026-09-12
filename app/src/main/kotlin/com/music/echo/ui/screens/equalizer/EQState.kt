package com.biikkkuuuu.muzi.ui.screens.equalizer

import com.biikkkuuuu.muzi.eq.data.SavedEQProfile


data class EQState(
    val profiles: List<SavedEQProfile> = emptyList(),
    val activeProfileId: String? = null,
    val importStatus: String? = null,
    val error: String? = null
)