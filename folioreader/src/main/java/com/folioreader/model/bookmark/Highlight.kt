package com.folioreader.model.bookmark

data class Highlight(
    val id: Int,
    val bookName: String,
    val chapterName: String,
    val pageNumber: Int,
    val highlightText: String
)