package com.garemat.moonstone_companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garemat.moonstone_companion.AppTheme
import com.garemat.moonstone_companion.ui.theme.LocalAppTheme

@Composable
fun NullSymbol(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val appTheme = LocalAppTheme.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (appTheme == AppTheme.MOONSTONE) Color.Transparent else Color.LightGray.copy(alpha = 0.5f))
            .padding(if (size < 20.dp) 1.dp else 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (size < 20.dp) 1.2.dp.toPx() else 1.5.dp.toPx()
            // Outer circle line
            drawCircle(
                color = Color.Black,
                radius = this.size.minDimension / 2.2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            // Diagonal slash
            drawLine(
                color = Color.Black,
                start = Offset(this.size.width * 0.25f, this.size.height * 0.75f),
                end = Offset(this.size.width * 0.75f, this.size.height * 0.25f),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun getMoonstoneInlineContent() = mapOf(
    "nullSymbol" to InlineTextContent(
        Placeholder(
            width = 14.sp,
            height = 14.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        NullSymbol(size = 14.dp, modifier = Modifier.padding(horizontal = 2.dp))
    }
)

fun highlightText(text: String, searchQuery: String): AnnotatedString = buildAnnotatedString {
    appendWithHighlight(text, searchQuery)
}

fun AnnotatedString.Builder.appendWithHighlight(text: String, searchQuery: String) {
    if (searchQuery.isEmpty() || !text.contains(searchQuery, ignoreCase = true)) {
        append(text)
        return
    }

    val pattern = Regex.escape(searchQuery).toRegex(RegexOption.IGNORE_CASE)
    var lastIndex = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(lastIndex, match.range.first))
        withStyle(style = SpanStyle(
            background = Color.Yellow.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold
        )) {
            append(match.value)
        }
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}
