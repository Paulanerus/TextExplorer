package dev.paulee.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import dev.paulee.api.data.Change
import dev.paulee.api.plugin.Tag

fun java.awt.Color.toComposeColor() = Color(red, green, blue, alpha)

@Composable
fun MarkedText(
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
    textAlign: TextAlign = TextAlign.Start,
    text: String,
    highlights: Map<String, Tag>,
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0

        highlights.forEach { (highlightedWord, tagAndColor) ->
            val (tag, color) = tagAndColor
            val startIndex = text.indexOf(highlightedWord, currentIndex)

            val composeColor = color.toComposeColor()

            if (startIndex != -1) {
                append(text.substring(currentIndex, startIndex))

                withStyle(style = SpanStyle(background = composeColor.copy(alpha = 0.3f))) {
                    append(highlightedWord)
                }

                if (tag.isNotEmpty()) {
                    withStyle(
                        style = SpanStyle(
                            fontSize = 13.sp,
                            background = composeColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("  $tag ")
                    }
                }

                currentIndex = startIndex + highlightedWord.length
            }
        }

        if (currentIndex < text.length) append(text.substring(currentIndex))
    }
    Text(text = annotatedString, modifier = modifier, textDecoration = textDecoration, textAlign = textAlign)
}

@Composable
fun HeatmapText(
    change: Change?,
    fallback: String,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
    textAlign: TextAlign = TextAlign.Center,
) {
    val text = change?.str ?: fallback

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0

        change?.tokens.orEmpty().forEach { (token, range) ->
            val startIndex = text.indexOf(token, currentIndex)

            val composeColor = if (token.startsWith("**")) Color.Green else Color.Red

            if (startIndex != -1) {
                append(text.substring(currentIndex, startIndex))

                withStyle(style = SpanStyle(background = composeColor.copy(alpha = 0.3f))) {
                    append(token)
                }

                currentIndex = startIndex + token.length
            }
        }

        if (currentIndex < text.length) append(text.substring(currentIndex))
    }
    Text(text = annotatedString, modifier = modifier, textDecoration = textDecoration, textAlign = textAlign)
}
