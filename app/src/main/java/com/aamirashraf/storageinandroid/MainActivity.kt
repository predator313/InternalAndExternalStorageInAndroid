package com.aamirashraf.storageinandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aamirashraf.storageinandroid.adapter.InternalStoragePhotoAdapter
import com.aamirashraf.storageinandroid.adapter.SharedStoragePhotoAdapter
import com.aamirashraf.storageinandroid.databinding.ActivityMainBinding
import com.aamirashraf.storageinandroid.model.InternalStoragePhoto
import com.aamirashraf.storageinandroid.model.SharedStoragePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var internalStoragePhotoAdapter: InternalStoragePhotoAdapter
    private lateinit var externalStoragePhotoAdapter: SharedStoragePhotoAdapter
    private var readPermissionGranted:Boolean=false
    private var writePermissionGranted:Boolean=false

    //now we use the launcher for permission launcher
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
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
        externalStoragePhotoAdapter=SharedStoragePhotoAdapter {

        }
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permission->
            readPermissionGranted=permission[Manifest.permission.READ_EXTERNAL_STORAGE]?:readPermissionGranted
            writePermissionGranted=permission[Manifest.permission.WRITE_EXTERNAL_STORAGE]?:writePermissionGranted

        }
        updateAndRequestPermission()

        //since janitri phone is android 13 we need to request some
        //more permission regarding android tiramisu


        //takePhoto is contract
        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            val isPrivate = binding.switchPrivate.isChecked

                val isSavedSuccessfully =
                    when{
                        isPrivate->savePhotoToInternalStorage(UUID.randomUUID().toString(), it!!)
                        writePermissionGranted->savePhotoToExternalStorage(UUID.randomUUID().toString(),it!!)
                        else ->false
                    }
            if(isPrivate)loadPhotoFromInternalStorageIntoRecyclerView()

//                    savePhotoToInternalStorage(UUID.randomUUID().toString(), it!!)
                if (isSavedSuccessfully) {
                    loadPhotoFromInternalStorageIntoRecyclerView()
                    Toast.makeText(this@MainActivity, "Saved Successfully", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this@MainActivity, "Photo not saved", Toast.LENGTH_SHORT).show()
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

    //now work with the permission for the external storage
    private fun updateAndRequestPermission(){
        val hasReadPermission=ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )==PackageManager.PERMISSION_GRANTED

        val hasWritePermission=ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )==PackageManager.PERMISSION_GRANTED

        //for api level 29 we doesn't require the write permission
        val minSdk29=Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q
        readPermissionGranted=hasReadPermission
        writePermissionGranted=hasWritePermission || minSdk29

        //now all permission we need to request
        //in thing to remember that in android we takes all the
        //permission in the form of the string
        val permissionToRequest= mutableListOf<String>()
        if(!readPermissionGranted)permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if(!writePermissionGranted)permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //now we need to request the permission
        if(permissionToRequest.isNotEmpty()){
            //since the request list is not empty we need to request the permission
            permissionLauncher.launch(permissionToRequest.toTypedArray())
        }
    }
    private fun savePhotoToExternalStorage(displayName:String,bmp:Bitmap):Boolean{
       //media store is the huge database
        //with all kind of media data of the android
        val imageCollection= sdk29OrUP {
            //here we got the lambda function
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }?:MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValue=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE,"image/jpg")
            put(MediaStore.Images.Media.HEIGHT,bmp.height)
            put(MediaStore.Images.Media.WIDTH,bmp.width)
        }
        //saving of the things is always done in try and catch block
        return try {
            //to use somethings in external storage we
            //need a content resolver
            contentResolver.insert(imageCollection,contentValue)?.also {
                contentResolver.openOutputStream(it).use { outputStream->
                    if(!bmp.compress(Bitmap.CompressFormat.JPEG,96,outputStream!!)){
                        throw IOException("Can't able to save the image")
                    }

                }
            }?:throw IOException("can't abe to save the image")
            true
        }catch (e:IOException){
            e.printStackTrace()
            false
        }
    }


    private fun setUpRecyclerViewInternalStorage() = binding.rvPrivatePhotos.apply {
        adapter = internalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }
}