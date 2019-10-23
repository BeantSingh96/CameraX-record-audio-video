package com.knickglobal.videorecord

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import android.content.Context
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private val tag = MainActivity::class.java.simpleName

    private val permissionsCode = 10

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private lateinit var textureView: TextureView
    private lateinit var videoCapture: VideoCapture
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var orientationEventListener: OrientationEventListener


    private lateinit var file: File
    private var lensFacing = CameraX.LensFacing.BACK
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var captureButton: ImageButton
    private lateinit var switchButton: ImageButton
    private lateinit var fetchAudios: ImageButton
    private lateinit var audioRC: RecyclerView
    private lateinit var audioName: TextView

    private var prepared: Boolean? = false

    private var mediaSpeed: Float? = 1.5f



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inits()

    }


    /**
     * initialization of widgets
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("RestrictedApi", "ClickableViewAccessibility")
    private fun inits() {

        textureView = findViewById(R.id.view_finder)
        captureButton = findViewById(R.id.capture_button)
        audioRC = findViewById(R.id.audioRC)
        audioName = findViewById(R.id.audioName)
        audioName.visibility = GONE

        linearLayoutManager = LinearLayoutManager(this)
        audioRC.layoutManager = linearLayoutManager

        switchButton = findViewById(R.id.switch_button)
        switchButton.setOnClickListener {
            lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
                CameraX.LensFacing.BACK
            } else {
                CameraX.LensFacing.FRONT
            }
            try {
                // Only bind use cases if we can query a camera with this orientation
//                CameraX.getCameraWithLensFacing(lensFacing)

                startCamera()
            } catch (exc: Exception) {
                // Do nothing
            }
        }

        fetchAudios = findViewById(R.id.fetchAudios)
        fetchAudios.setOnClickListener {
            val list = getAllAudioFromDevice(this)
            if (list.isNotEmpty()) {
                fetchAudios.visibility = GONE
                audioName.visibility = GONE
                audioRC.visibility = VISIBLE
            }

            audioRC.adapter = AudioAdapter(this, list, object : AudioAdapter.OnItemClickListener {
                override fun onItemClick(pathAudio: String, nameAudio: String) {

                    audioName.text = nameAudio
                    audioName.visibility = VISIBLE
                    audioRC.visibility = GONE
                    fetchAudios.visibility = VISIBLE

                    val file = File(pathAudio)
                    mediaPlayer = MediaPlayer.create(this@MainActivity, Uri.fromFile(file))
                    prepareAudio()
                }
            })
        }

        // capture video button
        captureButton.setOnTouchListener { _, event ->
            file = File(
                externalMediaDirs.first(),
                "My video ${System.currentTimeMillis()}.mp4"
            )
            if (event.action == MotionEvent.ACTION_DOWN) {
                mediaPlayer?.start()

                captureButton.setBackgroundColor(Color.GREEN)
                captureButton.setPadding(45, 45, 45, 45)

                mediaPlayer?.playbackParams?.speed ?: mediaSpeed

                videoCapture.startRecording(file,object : VideoCapture.OnVideoSavedListener {
                    override fun onVideoSaved(file: File) {
                        Log.i(tag, "Video File : $file")
                    }

                    override fun onError(
                        videoCaptureError: VideoCapture.VideoCaptureError,
                        message: String,
                        cause: Throwable?
                    ) {
                        Log.i(tag, "Video Error: $message")
                    }
                })


            } else if (event.action == MotionEvent.ACTION_UP) {
                mediaPlayer?.stop()
                audioName.visibility = GONE

                captureButton.setPadding(25, 25, 25, 25)
                captureButton.setBackgroundColor(Color.RED)
                videoCapture.stopRecording()

                Log.i(tag, "Video File stopped")
            }
            false
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            textureView.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, requiredPermissions, permissionsCode
            )
        }
    }

    /**
     * start camera set preview
     */
    @SuppressLint("RestrictedApi")
    private fun startCamera() {

        CameraX.unbindAll()

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
        }.build()
        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {previewOutput->
            updateTransform()

            val parent = textureView.parent as ViewGroup
            parent.removeView(textureView)
            textureView.surfaceTexture = previewOutput.surfaceTexture
            parent.addView(textureView, 0)


            updateTransform()

//            textureView.surfaceTexture = it.surfaceTexture
        }

        // Create a configuration object for the video use case
        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetRotation(textureView.display.rotation)
        }.build()
        videoCapture = VideoCapture(videoCaptureConfig)

        // Bind use cases to lifecycle
        CameraX.bindToLifecycle(
            this@MainActivity,
            videoCapture,
            preview
        )
    }

    /**
     * Update camera transformation
     */
    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = textureView.width / 2f
        val centerY = textureView.height / 2f

        val rotationDegrees = when(textureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }

    /**
     * Fetch audios from device
     */
    private fun getAllAudioFromDevice(context: Context): List<AudioModel> {
        val tempAudioList = arrayListOf<AudioModel>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver
            .query(
                uri, null, null, null, null
            )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val audioModel = AudioModel()
                val path = cursor.getString(1)
                val name = cursor.getString(8)
                val album = cursor.getString(2)
                val artist = cursor.getString(25)

                audioModel.setaName(name)
                audioModel.setaAlbum(album)
                audioModel.setaArtist(artist)
                audioModel.setaPath(path)

                Log.e("Name :$name", " Album :$album")
                Log.e("Path :$path", " Artist :$artist")

                tempAudioList.add(audioModel)
            }
            cursor.close()
        } else {
            Log.e("Name :", " null")
        }

        return tempAudioList
    }

    /**
     * prepare audio player
     */
    private fun prepareAudio() {
        mediaPlayer?.setOnPreparedListener {
            prepared = true
        }
    }


    /**
     * check permissions are granted or not
     */
    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * permissions result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == permissionsCode) {
            if (allPermissionsGranted()) {
                textureView.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

}
