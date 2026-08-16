package com.widget.smartwidgets.widgets.calendar

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

/**
 * Repository to query the Android Calendar Provider.
 * Minimal abstraction focusing on battery efficiency.
 */
object CalendarRepository {

    fun getUpcomingEvents(context: Context, limit: Int = 5): List<CalendarEvent> {
        // Double check permission (even though the widget UI checks it)
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()
        
        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )

        // Query events from now onwards
        val now = System.currentTimeMillis()
        val selection = "${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DELETED} != 1"
        val selectionArgs = arrayOf(now.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT $limit"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val dtStartIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val dtEndIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

                do {
                    val id = cursor.getLong(idIdx)
                    val title = cursor.getString(titleIdx) ?: "No Title"
                    val dtStart = cursor.getLong(dtStartIdx)
                    val dtEnd = cursor.getLong(dtEndIdx)
                    val allDay = cursor.getInt(allDayIdx) == 1

                    events.add(
                        CalendarEvent(
                            id = id,
                            title = title,
                            startTime = dtStart,
                            endTime = dtEnd,
                            isAllDay = allDay
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: SecurityException) {
            // Permission was revoked
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return events
    }
}
