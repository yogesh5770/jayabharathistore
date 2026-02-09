package com.jayabharathistore.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jayabharathistore.app.ui.theme.Neutral100
import com.jayabharathistore.app.ui.theme.Neutral200

@Composable
fun AnimateInEffect(
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + 
                slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { 30 }
                ),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun ShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Neutral100,
        Neutral200,
        Neutral100,
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun ShimmerItem(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(ShimmerBrush())
    )
}
