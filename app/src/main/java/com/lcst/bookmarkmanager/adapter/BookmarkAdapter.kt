package com.lcst.bookmarkmanager.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lcst.bookmarkmanager.data.Bookmark
import com.lcst.bookmarkmanager.databinding.ItemBookmarkBinding

class BookmarkAdapter(
    private val onEditClick: (Bookmark) -> Unit,
    private val onDeleteClick: (Bookmark) -> Unit,
    private val onItemClick: (Bookmark) -> Unit
) : ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookmarkViewHolder(
        private val binding: ItemBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark) {
            binding.titleTextView.text = bookmark.title
            binding.urlTextView.text = bookmark.url

            binding.editButton.setOnClickListener {
                onEditClick(bookmark)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(bookmark)
            }

            binding.root.setOnClickListener {
                onItemClick(bookmark)
            }
        }
    }

    class BookmarkDiffCallback : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            // Se entrambi hanno id = 0, usa title+url come identificatore univoco
            return if (oldItem.id == 0L && newItem.id == 0L) {
                oldItem.title == newItem.title && oldItem.url == newItem.url
            } else {
                oldItem.id == newItem.id && oldItem.id != 0L
            }
        }

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem == newItem
        }
    }
}

