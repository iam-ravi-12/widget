package com.widget.smartwidgets.widgets.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widget.smartwidgets.MainActivity
import com.widget.smartwidgets.data.model.Note
import com.widget.smartwidgets.data.repository.NoteRepository
import com.widget.smartwidgets.widgets.common.WidgetTheme

class QuickNotesWidget : GlanceAppWidget() {

    companion object {
        private val SMALL = DpSize(120.dp, 120.dp)
        private val MEDIUM = DpSize(200.dp, 160.dp)
        private val LARGE = DpSize(280.dp, 280.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = NoteRepository(context)
        
        // Render 1 note for small, 3 for medium, 6 for large
        // We will fetch up to 6 recent notes
        val notes = repository.getRecentNotes(6)

        provideContent {
            GlanceTheme {
                QuickNotesContent(notes)
            }
        }
    }
}

@Composable
private fun QuickNotesContent(notes: List<Note>) {
    val size = LocalSize.current
    val context = LocalContext.current
    
    val isSmall = size.width < 180.dp || size.height < 150.dp
    val isLarge = size.height >= 250.dp
    
    val maxNotes = when {
        isSmall -> 1
        isLarge -> 5
        else -> 3
    }

    val createIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("smartwidgets://notes/create")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(WidgetTheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Notes",
                style = TextStyle(
                    color = WidgetTheme.accent,
                    fontSize = if (isSmall) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            Text(
                text = "+",
                style = TextStyle(
                    color = WidgetTheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.clickable(actionStartActivity(createIntent))
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        if (notes.isEmpty()) {
            Text(
                text = "No notes yet\nTap + to create one",
                style = TextStyle(
                    color = WidgetTheme.textSecondary,
                    fontSize = 13.sp
                )
            )
        } else {
            notes.take(maxNotes).forEach { note ->
                NoteItem(note = note, isSmall = isSmall)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoteItem(note: Note, isSmall: Boolean) {
    val context = LocalContext.current
    val editIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("smartwidgets://notes/edit/${note.id}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(editIntent)),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = TextStyle(
                color = WidgetTheme.accent,
                fontSize = if (isSmall) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(end = 6.dp)
        )
        Text(
            text = note.content,
            style = TextStyle(
                color = WidgetTheme.textPrimary,
                fontSize = if (isSmall) 12.sp else 14.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = if (isSmall) 2 else 1
        )
    }
}
