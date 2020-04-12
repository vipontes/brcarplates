package br.net.easify.brcarplates

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var detector: FirebaseVisionTextRecognizer
    private val cameraRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Dexter.withActivity(this@MainActivity)
            .withPermissions(Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    setupCamera()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {

                }
            }).check()

        selectImage.setOnClickListener {
            hideData()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                openCamera()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Sorry you're version android is not support, Min Android 6.0 (Marsmallow)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }



        startRecognizing.setOnClickListener {
            if (imageView.drawable != null) {
                recognizedText.setText("")
                startRecognizing.isEnabled = false
                val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                val image = FirebaseVisionImage.fromBitmap(bitmap)

                detector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText ->
                        startRecognizing.isEnabled = true
                        processResultText(firebaseVisionText)
                    }
                    .addOnFailureListener {
                        startRecognizing.isEnabled = true
                        recognizedText.setText("Failed")
                    }
            } else {
                Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")

        // camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        startActivityForResult(cameraIntent, cameraRequestCode)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {

        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir("images", Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

    private fun setupCamera() {
        detector = FirebaseVision.getInstance().onDeviceTextRecognizer
    }

    private fun hideData() {
        startRecognizing.visibility = View.GONE
        recognizedText.setText("")
    }

    private fun showOption() {
        startRecognizing.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            cameraRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val bitmap = data.extras!!.get("data") as Bitmap
                    imageView.setImageBitmap(bitmap)
                    val uri: Uri = saveImageToInternalStorage(bitmap)
                    showOption()
                }
            }
        }
    }

    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            recognizedText.setText("No Text Found")
            return
        }
        for (block in resultText.textBlocks) {
            val blockText = block.text
            recognizedText.append(blockText + "\n")
        }
    }
}

/*
package br.net.easify.brcarplates

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.otaliastudios.cameraview.frame.Frame
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var detector: FirebaseVisionTextRecognizer
    private var isDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanAgain.setOnClickListener(View.OnClickListener {
            isDetected = false
            scanAgain.visibility = View.GONE
        })

        Dexter.withActivity(this@MainActivity)
            .withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    setupCamera()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {

                }
            }).check()
    }

    private fun setupCamera() {
        detector = FirebaseVision.getInstance().onDeviceTextRecognizer
        cameraView.setLifecycleOwner(this@MainActivity)
        cameraView.addFrameProcessor { frame ->
            processImage(getVisionImageFromFrame(frame))
        }
    }

    private fun getVisionImageFromFrame(frame: Frame): FirebaseVisionImage {
        val data = frame.data
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setHeight(frame.size.height)
            .setWidth(frame.size.width)
            .build()

        return FirebaseVisionImage.fromByteArray(data, metadata)
    }

    fun processImage(image: FirebaseVisionImage) {

        if (!isDetected) {
            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    processResultText(firebaseVisionText)
                }
                .addOnFailureListener {
                    editText.setText("Failed")
                }
        }
    }

    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            editText.setText("")
            scanAgain.visibility = View.VISIBLE
            isDetected = false
            return
        }

        isDetected = true

        for (block in resultText.textBlocks) {
            val blockText = block.text
            editText.append(blockText + "\n")
        }
    }
}
 */