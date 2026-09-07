package com.example.muzo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.theme.GoogleSansFlex

/**
 * Data model for an Echo Music 1:1 Material 3 settings row.
 */
data class Material3SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val customIcon: (@Composable () -> Unit)? = null,
    val isCircleIcon: Boolean = false,
    val badge: String? = null,
    val subtitleColor: Color? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit
)

/**
 * Material3SettingsGroup: Echo Music 1:1 Rounded Card Grouping
 * Top item has 24dp top corners, bottom item has 24dp bottom corners,
 * middle items have 4dp corners, with 2dp spacing inside a surfaceContainerHigh card.
 */
@Composable
fun Material3SettingsGroup(
    items: List<Material3SettingsItem>,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GoogleSansFlex,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    index == items.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(4.dp)
                }

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .clickable { item.onClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon in squircle container
                        if (item.customIcon != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                item.customIcon.invoke()
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        } else if (item.icon != null) {
                            val bgShape = if (item.isCircleIcon) CircleShape else RoundedCornerShape(12.dp)
                            val bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(bgShape)
                                    .background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // Title and Subtitle with GoogleSansFlex
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                item.badge?.let { badgeText ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = GoogleSansFlex,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            item.subtitle?.let { sub ->
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = GoogleSansFlex,
                                    color = item.subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Trailing Content
                        item.trailingContent?.let { trailing ->
                            Spacer(modifier = Modifier.width(8.dp))
                            trailing()
                        }
                    }
                }
            }
        }
    }
}
