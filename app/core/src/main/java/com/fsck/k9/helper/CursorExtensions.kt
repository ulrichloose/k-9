package com.fsck.k9.helper

import android.database.Cursor

fun <T> Cursor.map(block: (Cursor) -> T): List<T> {
    return List(count) { index ->
        moveToPosition(index)
        block(this)
    }
}

fun Cursor.getStringOrNull(columnName: String): String? {
    val columnIndex = getColumnIndex(columnName)
    return if (isNull(columnIndex)) null else getString(columnIndex)
}

fun Cursor.getIntOrNull(columnName: String): Int? {
    val columnIndex = getColumnIndex(columnName)
    return if (isNull(columnIndex)) null else getInt(columnIndex)
}

fun Cursor.getLongOrNull(columnName: String): Long? {
    val columnIndex = getColumnIndex(columnName)
    return if (isNull(columnIndex)) null else getLong(columnIndex)
}

fun Cursor.getStringOrThrow(columnName: String): String {
    return getStringOrNull(columnName) ?: error("Column $columnName must not be null")
}

fun Cursor.getIntOrThrow(columnName: String): Int {
    return getIntOrNull(columnName) ?: error("Column $columnName must not be null")
}

fun Cursor.getLongOrThrow(columnName: String): Long {
    return getLongOrNull(columnName) ?: error("Column $columnName must not be null")
}
