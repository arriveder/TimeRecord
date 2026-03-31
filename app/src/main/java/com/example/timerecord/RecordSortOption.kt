package com.example.timerecord

enum class RecordSortOption(val title: String) {
    TIME_DESC("时间最新"),
    TIME_ASC("时间最早"),
    NOTE_ASC("备注 A-Z"),
    NOTE_DESC("备注 Z-A")
}
