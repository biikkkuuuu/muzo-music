

package com.biikkkuuuu.muzi.models

import com.music.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
