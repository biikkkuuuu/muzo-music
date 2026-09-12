

package com.biikkkuuuu.muzi.models

import com.music.innertube.models.YTItem
import com.biikkkuuuu.muzi.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
