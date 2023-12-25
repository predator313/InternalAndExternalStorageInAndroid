package com.aamirashraf.storageinandroid

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aamirashraf.storageinandroid.adapter.InternalStoragePhotoAdapter
import com.aamirashraf.storageinandroid.databinding.ActivityMainBinding
import com.aamirashraf.storageinandroid.model.InternalStoragePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var internalStoragePhotoAdapter: InternalStoragePhotoAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        internalStoragePhotoAdapter = InternalStoragePhotoAdapter {
            val isDeletionSuccessful = deletePhotoFromInternalStorage(it.name)
            if (isDeletionSuccessful) {

                loadPhotoFromInternalStorageIntoRecyclerView()
                Toast.makeText(this@MainActivity, "Deleted Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Photo not Deleted", Toast.LENGTH_SHORT).show()
            }

        }
//        internalStoragePhotoAdapter.setCallback {
//            val isDeletionSuccessful=deletePhotoFromInternalStorage(it.name)
//            Log.d("hello",isDeletionSuccessful.toString())
//            if (isDeletionSuccessful) {
//
//                loadPhotoFromInternalStorageIntoRecyclerView()
//                Toast.makeText(this@MainActivity, "Deleted Successfully", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this@MainActivity, "Photo not Deleted", Toast.LENGTH_SHORT).show()
//            }
//        }
        //takePhoto is contract
        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            val isPrivate = binding.switchPrivate.isChecked
            if (isPrivate) {
                val isSavedSuccessfully =
                    savePhotoToInternalStorage(UUID.randomUUID().toString(), it!!)
                if (isSavedSuccessfully) {
                    loadPhotoFromInternalStorageIntoRecyclerView()
                    Toast.makeText(this@MainActivity, "Saved Successfully", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this@MainActivity, "Photo not saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
        //TakePicturePreview gives the bitmap of the photo taken from the camera
        binding.btnTakePhoto.setOnClickListener {
//            implementation "androidx.activity:activity-ktx:1.8.2"
//            implementation 'androidx.fragment:fragment-ktx:1.6.2'
            //to use launch we need to use these dependencies
            takePhoto.launch()
        }

        setUpRecyclerViewInternalStorage()
        loadPhotoFromInternalStorageIntoRecyclerView()


    }

    private fun savePhotoToInternalStorage(fileName: String, bmp: Bitmap): Boolean {
        //data related operation should be done in the try and catch block
        return try {
            openFileOutput("$fileName.jpg", MODE_PRIVATE).use { stream ->
                //MODE_PRIVATE means the file created here is only access by this application
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 96, stream)) {
                    throw IOException("can't abe to save bitmap")
                }
                return true
            }

        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun loadPhotoFromInternalStorage(): List<InternalStoragePhoto> {
        //here we loads all photo from internal storage so better to use the
        //suspend function and make all all inside the coroutines async
        return withContext(Dispatchers.IO) {
            //means withContext we switch from main thread to the background thread
            val allFiles = filesDir.listFiles()
            allFiles?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }
                ?.map {
                    val bytes = it.readBytes()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    InternalStoragePhoto(it.name, bmp)
                } ?: listOf()  ///if null return empty list
        }
    }

    private fun deletePhotoFromInternalStorage(fileName: String): Boolean {
        return try {
            deleteFile(fileName)  //this is the inbuilt function used to delete file

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }

    private fun loadPhotoFromInternalStorageIntoRecyclerView() {
        lifecycleScope.launch {
            //means that this coroutines is only active as it parent(activity/fragment)
            val allPhoto = loadPhotoFromInternalStorage()
            internalStoragePhotoAdapter.submitList(allPhoto)

        }

    }

    private fun setUpRecyclerViewInternalStorage() = binding.rvPrivatePhotos.apply {
        adapter = internalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }
}