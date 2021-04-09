package com.divyanshu.androiddraw

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.divyanshu.draw.result.contract.CreateDrawingActivityResultContract
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var adapter: DrawAdapter

    private val createDrawingActivityResultLauncher = registerForActivityResult(CreateDrawingActivityResultContract) {
        if (it != null) {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            showSaveDialog(bitmap)
        }
    }

    private val requestPermissionActivityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            adapter = DrawAdapter(this, getFilesPath())
            recycler_view.adapter = adapter
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val writeExternalStoragePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, writeExternalStoragePermission)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionActivityResultLauncher.launch(writeExternalStoragePermission)
        } else {
            adapter = DrawAdapter(this, getFilesPath())
            recycler_view.adapter = adapter
        }

        fab_add_draw.setOnClickListener {
            CreateDrawingActivityResultContract.launch(createDrawingActivityResultLauncher)
        }
    }

    private fun getFilesPath(): ArrayList<String> {
        val resultList = ArrayList<String>()
        val imageDir = "${Environment.DIRECTORY_PICTURES}/Android Draw/"
        val path = Environment.getExternalStoragePublicDirectory(imageDir)
        path.mkdirs()
        val imageList = path.listFiles()
        for (imagePath in imageList) {
            resultList.add(imagePath.absolutePath)
        }

        return resultList
    }

    private fun showSaveDialog(bitmap: Bitmap) {
        val alertDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_save, null)
        alertDialog.setView(dialogView)
        val fileNameEditText: EditText = dialogView.findViewById(R.id.editText_file_name)
        val filename = UUID.randomUUID().toString()
        fileNameEditText.setSelectAllOnFocus(true)
        fileNameEditText.setText(filename)
        alertDialog.setTitle("Save Drawing")
                .setPositiveButton("ok") { _, _ -> saveImage(bitmap, fileNameEditText.text.toString()) }
                .setNegativeButton("Cancel") { _, _ -> }

        val dialog = alertDialog.create()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun saveImage(bitmap: Bitmap, fileName: String) {
        val imageDir = "${Environment.DIRECTORY_PICTURES}/Android Draw/"
        val path = Environment.getExternalStoragePublicDirectory(imageDir)
        Log.e("path", path.toString())
        val file = File(path, "$fileName.png")
        path.mkdirs()
        file.createNewFile()
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        updateRecyclerView(Uri.fromFile(file))
    }

    private fun updateRecyclerView(uri: Uri) {
        adapter.addItem(uri)
    }
}