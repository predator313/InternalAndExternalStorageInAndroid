package com.aamirashraf.storageinandroid.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aamirashraf.storageinandroid.databinding.RvItemsBinding
import com.aamirashraf.storageinandroid.model.InternalStoragePhoto

class InternalStoragePhotoAdapter(
    private val onPhotoClick: (InternalStoragePhoto) -> Unit

) : ListAdapter<InternalStoragePhoto, InternalStoragePhotoAdapter.PhotoViewHolder>(Companion) {
    inner class PhotoViewHolder(val binding: RvItemsBinding): RecyclerView.ViewHolder(binding.root)
    private var onPhotoClick2:((InternalStoragePhoto)->Unit)?=null

    companion object : DiffUtil.ItemCallback<InternalStoragePhoto>() {
        override fun areItemsTheSame(oldItem: InternalStoragePhoto, newItem: InternalStoragePhoto): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: InternalStoragePhoto, newItem: InternalStoragePhoto): Boolean {
            return oldItem.name == newItem.name && oldItem.bmp.sameAs(newItem.bmp)
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
            ivPhoto.setImageBitmap(photo.bmp)

            val aspectRatio = photo.bmp.width.toFloat() / photo.bmp.height.toFloat()
            ConstraintSet().apply {
                clone(root)
                setDimensionRatio(ivPhoto.id, aspectRatio.toString())
                applyTo(root)
            }

            ivPhoto.setOnLongClickListener {
                onPhotoClick(photo)
                true
            }
            ivPhoto.setOnClickListener {
//                onPhotoClick2?.let {
//                    it(photo)
//                    Log.d("hello","clicked")
//                }
//                onPhotoClick2?.invoke(photo)
                onPhotoClick(photo)

            }
        }
    }
    fun setCallback(callback:(InternalStoragePhoto)->Unit){
        onPhotoClick2=callback

    }
}