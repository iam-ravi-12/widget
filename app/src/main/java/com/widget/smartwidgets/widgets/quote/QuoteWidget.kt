package com.widget.smartwidgets.widgets.quote

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.widgets.common.GlanceWidgetCard
import java.util.Calendar
import androidx.compose.ui.unit.sp

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}

class QuoteWidget : GlanceAppWidget() {
    
    private val quotes = listOf(
        "Stay focused and never give up." to "Unknown",
        "The only way to do great work is to love what you do." to "Steve Jobs",
        "It always seems impossible until it's done." to "Nelson Mandela",
        "Believe you can and you're halfway there." to "Theodore Roosevelt",
        "Act as if what you do makes a difference. It does." to "William James",
        "Success is not final, failure is not fatal: it is the courage to continue that counts." to "Winston Churchill",
        "What you get by achieving your goals is not as important as what you become by achieving your goals." to "Zig Ziglar",
        "You are never too old to set another goal or to dream a new dream." to "C.S. Lewis",
        "The future belongs to those who believe in the beauty of their dreams." to "Eleanor Roosevelt",
        "Don't watch the clock; do what it does. Keep going." to "Sam Levenson"
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val quoteIndex = dayOfYear % quotes.size
        val quote = quotes[quoteIndex]

        provideContent {
            GlanceTheme {
                GlanceWidgetCard(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\"${quote.first}\"",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "— ${quote.second}",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
        }
    }
}
