package com.bytedance.camera.demo

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PictureCallback
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.hardware.Camera.getCameraInfo
import android.media.MediaMetadataRetriever
import android.view.*
import android.widget.*
import java.nio.file.Path
import android.graphics.Bitmap
import android.graphics.Matrix

import android.media.ExifInterface





class VideoRecordActivity : AppCompatActivity() {
    private var mTakePhoto: Button? = null
    private var mRecordVideo: Button? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mIsRecording = false

    private var mFileName : EditText? = null

    private var mIntroText : TextView? = null
    private var mChangeButton : ImageButton? = null
    private var mRecordButton : ImageButton? = null
    private var mViewbutton : ImageView? =null

    private var iFontCameraIndex = 0
    private var iBackCameraIndex = 0
    private var bBack = true
    private var touchBeginTime : Long = 0L
    private var isRecording : Boolean = false
    private var lastMediaPath : String? = null



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)

        mSurfaceView = findViewById(R.id.surface_view)

        mIntroText = findViewById(R.id.intro_text)
        mFileName = findViewById(R.id.set_name)
        mChangeButton = findViewById(R.id.change_button)
        mRecordButton = findViewById(R.id.record_button)
        mViewbutton = findViewById(R.id.view_button)

        startCamera()

        mChangeButton?.setOnClickListener(View.OnClickListener {
            changeCamera()
        })

        mRecordButton?.setOnTouchListener{_, me ->
            if (me?.action == MotionEvent.ACTION_DOWN) {
                touchBeginTime = System.currentTimeMillis()
                isRecording = false
            }
            else if (me.action == MotionEvent.ACTION_MOVE){
                if(!isRecording && System.currentTimeMillis() - touchBeginTime > 150){
                    if (prepareVideoRecorder()) {
                        mMediaRecorder!!.start()
                        isRecording = true
                    } else {
                        releaseMediaRecorder()
                    }
                }
            }
            else if (me.action == MotionEvent.ACTION_UP){
                if(System.currentTimeMillis() - touchBeginTime <= 150){
                    takePicture()
                }else if(isRecording){
                    mMediaRecorder!!.stop()
                    releaseMediaRecorder()
                    mCamera!!.lock()
                    mIsRecording = false
                }
            }
            return@setOnTouchListener true
        }
    }


        private fun startCamera() {
            try {
                mCamera = Camera.open(CameraInfo.CAMERA_FACING_BACK)
                setCameraDisplayOrientation()
            } catch (e: Exception) {
                // error
            }
            mSurfaceHolder = mSurfaceView!!.holder
            mSurfaceHolder?.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {
                        mCamera?.setPreviewDisplay(holder)
                        mCamera?.startPreview()
                    } catch (e: IOException) {
                        // error
                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
                    try {
                        mCamera!!.stopPreview()
                    } catch (e: Exception) {
                        // error
                    }
                    try {
                        mCamera!!.setPreviewDisplay(holder)
                        mCamera!!.startPreview()
                    } catch (e: Exception) {
                        //error
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {}
            })
        }

    private fun setCameraDisplayOrientation() {
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val info = CameraInfo()
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info)
        val result = (info.orientation - degrees + 360) % 360
        mCamera!!.setDisplayOrientation(result)
    }

    private fun takePicture() {
        mCamera!!.takePicture(null, null, PictureCallback { bytes, camera ->
            val pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: return@PictureCallback
            try {
                showLastImg(pictureFile?.absolutePath)
                val fos = FileOutputStream(pictureFile)
                fos.write(bytes)
                fos.close()
            } catch (e: FileNotFoundException) {
                //error
            } catch (e: IOException) {
                //error
            }
            mCamera!!.startPreview()
        })
    }


    fun rotaingBitmap(angle: Int, bitmap: Bitmap): Bitmap? {
        //旋转图片 动作
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun showLastImg(path : String?){
        if (path == null) return

        val targetW = mViewbutton!!.width
        val targetH = mViewbutton!!.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true

        BitmapFactory.decodeFile(path, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight
        var inSampleSize = 1
        if (photoH > targetH && photoW > targetW){
            val halfHeight = photoH / 2
            val halfWidth = photoW / 2

            while (halfHeight / inSampleSize >= targetH && halfWidth / inSampleSize >= targetW){
                inSampleSize *= 2
            }
        }
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(path, bmOptions)
        val rotatedBitmap = rotaingBitmap(90,bitmap)
        mViewbutton!!.setImageBitmap(rotatedBitmap)

    }

    private fun showLastVideo(path: String?){
//        if (path == null) return
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val bitmap = retriever.getFrameAtTime(1,MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        if (bitmap != null ){
            val rotatedBitmap = rotaingBitmap(90, bitmap)
            mViewbutton!!.setImageBitmap(rotatedBitmap)
        }
        retriever.release()
    }

    private fun getOutputMediaFile(type: Int): File? {
        var setFileName = mFileName?.text.toString()
        var introText : String? = null
        // Android/data/com.bytedance.camera.demo/files/Pictures
        val mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!mediaStorageDir!!.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        if (setFileName == null) {
            setFileName = timeStamp
        }

        val mediaFile: File? = if (type == MEDIA_TYPE_IMAGE) {
            introText = "FileName:" + setFileName + mediaStorageDir.path + File.separator + "IMG_" + timeStamp + ".jpg"
//            lastMediaPath = mediaStorageDir.path + File.separator + "IMG_" + setFileName + ".jpg"
            File(mediaStorageDir.path + File.separator + "IMG_" + setFileName + ".jpg")
        } else if (type == MEDIA_TYPE_VIDEO) {
            introText = "FileName:" + setFileName + mediaStorageDir.path + File.separator + "VID_" + timeStamp + ".mp4"
//            lastMediaPath = mediaStorageDir.path + File.separator + "VID_" + setFileName + ".mp4"
            File(mediaStorageDir.path + File.separator + "VID_" + setFileName + ".mp4")
        } else {
            return null
        }
        mIntroText!!.text = introText + "\n存储路径:" + mediaFile?.absolutePath
        return mediaFile
    }

    private fun prepareVideoRecorder(): Boolean {
        mMediaRecorder = MediaRecorder()
        mCamera!!.unlock()
        mMediaRecorder!!.setCamera(mCamera)
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mMediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
        mMediaRecorder!!.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString())
        showLastVideo(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString())
        mMediaRecorder!!.setPreviewDisplay(mSurfaceHolder!!.surface)
        try {
            mMediaRecorder!!.prepare()
        } catch (e: IllegalStateException) {
            releaseMediaRecorder()
            return false
        } catch (e: IOException) {
            releaseMediaRecorder()
            return false
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun recordVideo() {
        if (mIsRecording) {
            mMediaRecorder!!.stop()
            releaseMediaRecorder()
            mCamera!!.lock()
            mIsRecording = false
            mRecordVideo!!.text = "Start Recording"
        } else {
            if (prepareVideoRecorder()) {
                mMediaRecorder!!.start()
                mIsRecording = true
                mRecordVideo!!.text = "Stop Recording"
            } else {
                releaseMediaRecorder()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaRecorder()
        releaseCamera()
    }

    private fun releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
            mMediaRecorder = null
            mCamera!!.lock()
        }
    }

    private fun releaseCamera() {
        if (mCamera != null) {
            mCamera!!.release()
            mCamera = null
        }
    }

    companion object {
        private const val MEDIA_TYPE_IMAGE = 1
        private const val MEDIA_TYPE_VIDEO = 2
    }

    fun getCameraInfo() {
        val cameraInfo = CameraInfo()
        val iCameraCnt = Camera.getNumberOfCameras()
        for (i in 0 until iCameraCnt) {
            getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                iFontCameraIndex = i
            } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                iBackCameraIndex = i
            }
        }
    }

    fun surfaceCreated(holder: SurfaceHolder?) {
        getCameraInfo()
        mCamera = Camera.open(iBackCameraIndex)
        bBack = true
        try {
            setCameraDisplayOrientation()
            mCamera!!.setPreviewDisplay(mSurfaceHolder)
            mCamera!!.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun changeCamera() {
        getCameraInfo()
        if (mCamera != null) {
            mCamera!!.stopPreview()
            releaseCamera()
        }
        if (bBack) {
            mCamera = Camera.open(iFontCameraIndex)
            setCameraDisplayOrientation()
            try {
                mCamera!!.setPreviewDisplay(mSurfaceHolder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mCamera!!.startPreview()
            bBack = false
        } else {
            mCamera = Camera.open(iBackCameraIndex)
            setCameraDisplayOrientation()
            try {
                mCamera!!.setPreviewDisplay(mSurfaceHolder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mCamera!!.startPreview()
            bBack = true
        }
    }

}