package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarRatingWidget(
    currentRating: Int,
    maxStars: Int = 5,
    onRatingSelected: ((Int) -> Unit)? = null,
    starSize: Dp = 28.dp,
    readOnly: Boolean = false,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        for (starIndex in 1..maxStars) {
            val isSelected = starIndex <= currentRating
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = 0.5f)
            )
            val starColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale)
                    .then(
                        if (!readOnly && onRatingSelected != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onRatingSelected(starIndex)
                            }
                        } else Modifier
                    )
                    .padding(2.dp)
                    .testTag("star_rating_${starIndex}")
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "$starIndex Stars",
                    tint = starColor,
                    modifier = Modifier.size(starSize)
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (currentRating > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Text(
                    text = if (currentRating > 0) "$currentRating / $maxStars" else "Not Rated",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentRating > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
