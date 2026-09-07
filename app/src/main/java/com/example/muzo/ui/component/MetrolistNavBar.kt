package com.example.muzo.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muzo.theme.GoogleSansFlex
import com.example.muzo.ui.utils.bounceClick

/**
 * Metrolist Floating Pill Navigation Bar.
 * Perfectly mirrors Screenshot 2 with animated pill indicator for the active tab.
 */
@Composable
fun MetrolistNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                title = "Home",
                icon = Icons.Default.Home,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            NavBarItem(
                title = "Search",
                icon = Icons.Default.Search,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )

            NavBarItem(
                title = "Library",
                icon = Icons.Default.LibraryMusic,
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )


        }
    }
}

@Composable
private fun NavBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_item_scale"
    )

    val pillBackground by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "pill_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pill_color"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(pillBackground)
            .bounceClick(scaleDown = 0.94f, onClick = onClick)
            .padding(horizontal = if (isSelected) 16.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = contentColor
                )
            }
        }
    }
}
