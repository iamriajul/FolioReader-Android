package com.folioreader.model.bookmark

data class Note(
    val id: Int,
    val bookName: String,
    val chapterName: String,
    val pageNumber: Int
)