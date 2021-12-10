package com.bytedance.camera.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TakePictureActivity : AppCompatActivity() {
    private var mTakePhoto: Button? = null
    private var mImageView: ImageView? = null
    private var mCurrentPhotoPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_picture)
        mTakePhoto = findViewById(R.id.take_picture)
        mImageView = findViewById(R.id.image_view)
        mTakePhoto?.setOnClickListener(View.OnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this@TakePictureActivity,
                    Manifest.permission.CAMERA
                )
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    this@TakePictureActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@TakePictureActivity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
            } else {
                dispatchTakePictureIntent()
            }
        })
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            // error
        }
        if (photoFile != null) {
            // 获取存储图片的URI
            val photoURI = FileProvider.getUriForFile(this,
                "com.bytedance.camera.demo.fileprovider", photoFile
            )
            /**
             *   补充完整缺失代码 A2
             */
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // 获取当前时间作为文件名
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"

        // 获取应用文件存储路径 Android/data/com.bytedance.camera.demo/files/Pictures
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, ".jpeg", storageDir)

        // 保存文件路径
        mCurrentPhotoPath = image.absolutePath
        return image
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            /**
//             *   补充完整缺失代码 A1
//             */
//            val extras = data?.extras
//            val imageBitmaps = extras?.get("data") as Bitmap
//            mImageView?.setImageBitmap(imageBitmaps)
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            /**
             *   补充完整缺失代码 A1
             */

            val targetW = mImageView!!.width
            val targetH = mImageView!!.height

            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true

            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight
            var inSampleSize = 1
            if (photoH > targetH || photoW > targetW){
                val halfHeight = photoH / 2
                val halfWidth = photoW / 2

                while (halfHeight / inSampleSize >= targetH || halfWidth / inSampleSize >= targetW){
                    inSampleSize *= 2
                }
            }
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = inSampleSize
            bmOptions.inPurgeable = true
            val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
            mImageView!!.setImageBitmap(bitmap)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        }
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_CAMERA_PERMISSION = 2
    }

    fun rotateImage(bitmap: Bitmap, path: String) : Bitmap {
        val srcExif = ExifInterface(path)
        val matrix = Matrix()

        var angle = 0
        var orientation = srcExif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 ->
                angle = 90
            ExifInterface.ORIENTATION_ROTATE_180 ->
                angle = 180
            ExifInterface.ORIENTATION_ROTATE_270 ->
                angle = 270
            else ->
                angle = 0
        }
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
            matrix, true)
    }

}