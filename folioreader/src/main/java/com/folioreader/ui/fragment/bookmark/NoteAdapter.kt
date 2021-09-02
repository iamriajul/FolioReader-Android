package com.folioreader.ui.fragment.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.folioreader.databinding.SimpleNoteItemBinding
import com.folioreader.model.bookmark.Note
import com.folioreader.ui.base.BaseAdapter

class NoteAdapter : BaseAdapter<Note, SimpleNoteItemBinding>() {
    override fun initializeViewBinding(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ) = SimpleNoteItemBinding.inflate(layoutInflater, parent, false)

    override fun initializeDiffItemCallback() = object : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem

    }

    override fun onBindViewHolder(holder: BaseViewHolder<SimpleNoteItemBinding>, position: Int) {
        val note = differ.currentList[position]
        holder.binding.apply {
            val bookAndChapterName = "${note.bookName} ${note.chapterName}"
            val pageNumber = "Page ${note.pageNumber}"
            tvBookName.text = bookAndChapterName
            tvPageNumber.text = pageNumber
        }
    }

}