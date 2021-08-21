package com.folioreader.ui.fragment.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.folioreader.databinding.SimpleBookmarkItemBinding
import com.folioreader.model.bookmark.Bookmark
import com.folioreader.ui.base.BaseAdapter

class BookmarkAdapter : BaseAdapter<Bookmark, SimpleBookmarkItemBinding>() {
    override fun initializeViewBinding(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ) = SimpleBookmarkItemBinding.inflate(layoutInflater, parent, false)

    override fun initializeDiffItemCallback() = object : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark) = oldItem == newItem

    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<SimpleBookmarkItemBinding>,
        position: Int
    ) {
        val bookmark = differ.currentList[position]
        holder.binding.apply {
            val bookAndChapterName = "${bookmark.bookName} ${bookmark.chapterName}"
            val pageNumber = "Page ${bookmark.pageNumber}"
            tvBookName.text = bookAndChapterName
            tvPageNumber.text = pageNumber
        }
    }
}