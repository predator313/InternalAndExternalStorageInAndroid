package com.aamirashraf.storageinandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aamirashraf.storageinandroid.databinding.RvItemsBinding
import com.aamirashraf.storageinandroid.model.SharedStoragePhoto

class SharedStoragePhotoAdapter(
    private val onPhotoClick: (SharedStoragePhoto) -> Unit
) : ListAdapter<SharedStoragePhoto, SharedStoragePhotoAdapter.PhotoViewHolder>(Companion) {

    inner class PhotoViewHolder(val binding: RvItemsBinding): RecyclerView.ViewHolder(binding.root)

    companion object : DiffUtil.ItemCallback<SharedStoragePhoto>() {
        override fun areItemsTheSame(oldItem: SharedStoragePhoto, newItem: SharedStoragePhoto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SharedStoragePhoto, newItem: SharedStoragePhoto): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            RvItemsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = currentList[position]
        holder.binding.apply {
            ivPhoto.setImageURI(photo.contentUri)

            val aspectRatio = photo.width.toFloat() / photo.height.toFloat()
            ConstraintSet().apply {
                clone(root)
                setDimensionRatio(ivPhoto.id, aspectRatio.toString())
                applyTo(root)
            }

            ivPhoto.setOnLongClickListener {
                onPhotoClick(photo)
                true
            }
        }
    }
}