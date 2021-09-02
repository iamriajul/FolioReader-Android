package com.folioreader.ui.fragment.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.folioreader.databinding.SimpleHighlightItemBinding
import com.folioreader.model.bookmark.Highlight
import com.folioreader.ui.base.BaseAdapter

class HighlightAdapter : BaseAdapter<Highlight, SimpleHighlightItemBinding>() {
    override fun initializeViewBinding(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ) = SimpleHighlightItemBinding.inflate(layoutInflater, parent, false)

    override fun initializeDiffItemCallback() = object : DiffUtil.ItemCallback<Highlight>() {
        override fun areItemsTheSame(oldItem: Highlight, newItem: Highlight) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Highlight, newItem: Highlight) = oldItem == newItem

    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<SimpleHighlightItemBinding>,
        position: Int
    ) {
        val highlight = differ.currentList[position]
        holder.binding.apply {
            val bookAndChapterName = "${highlight.bookName}-${highlight.chapterName}"
            tvBookName.text = bookAndChapterName
            tvHighlightText.text = highlight.highlightText
        }
    }


}