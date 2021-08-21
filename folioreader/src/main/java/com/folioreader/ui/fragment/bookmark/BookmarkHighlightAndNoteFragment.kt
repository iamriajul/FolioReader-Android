package com.folioreader.ui.fragment.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.folioreader.R
import com.folioreader.databinding.FragmentBookmarkHighlightAndNoteBinding
import com.folioreader.model.bookmark.Bookmark
import com.folioreader.model.bookmark.Note
import com.folioreader.ui.base.BaseDialogFragment
import com.folioreader.ui.base.BaseFragment
import com.folioreader.ui.view.hide
import com.folioreader.ui.view.show

class BookmarkHighlightAndNoteFragment :
    BaseDialogFragment<FragmentBookmarkHighlightAndNoteBinding>() {

    override val fullscreen = true

    override val transparent = false

    private lateinit var bookmarkAdapter: BookmarkAdapter
    private lateinit var noteAdapter: NoteAdapter

    companion object {
        const val TAG = "BookmarkHighlightAndNot"
    }

    override fun initializeViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): FragmentBookmarkHighlightAndNoteBinding {
        val binding = FragmentBookmarkHighlightAndNoteBinding.inflate(inflater, parent, false)
        binding.lifecycleOwner = this
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupRecyclerViews()
    }

    private fun setupRecyclerViews() {
        bookmarkAdapter = BookmarkAdapter()
        noteAdapter = NoteAdapter()
        binding.rvBookmark.adapter = bookmarkAdapter

        bookmarkAdapter.differ.submitList(
            listOf(
                Bookmark(1, "Book 01", "Chapter 02", 9),
                Bookmark(2, "Book 01", "Chapter 02", 12),
                Bookmark(3, "Book 01", "Chapter 03", 34),
                Bookmark(4, "Book 01", "Chapter 04", 56),
            )
        )
        noteAdapter.differ.submitList(
            listOf(
                Note(1, "Book 01", "Chapter 01", 3),
                Note(3, "Book 01", "Chapter 03", 26),
                Note(4, "Book 01", "Chapter 04", 53)
            )
        )
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            dismiss()
        }

        binding.buttonGroup.addOnButtonCheckedListener { _, checkedId, _ ->
            var title = getString(R.string.bookmark)
            when (checkedId) {
                R.id.btnBookmark -> {
                    title = getString(R.string.bookmark)
                    binding.rvBookmark.show()
                    binding.rvBookmark.adapter = bookmarkAdapter
                }
                R.id.btnHighLight -> {
                    title = getString(R.string.highlight)
                    binding.rvBookmark.hide()
                }
                R.id.btnNote -> {
                    title = getString(R.string.note)
                    binding.rvBookmark.show()
                    binding.rvBookmark.adapter = noteAdapter
                }
            }
            binding.tvTitle.text = title
        }
    }


}