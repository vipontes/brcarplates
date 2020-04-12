package br.net.easify.brcarplates

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
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


class MainActivity : AppCompatActivity() {

    private lateinit var detector: FirebaseVisionTextRecognizer
    private val cameraRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        selectImage.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if ( callCameraIntent.resolveActivity(packageManager) != null ) {
                startActivityForResult(callCameraIntent, cameraRequestCode)
            }
        }

        startRecognizing.setOnClickListener {
            if (imageView.drawable != null) {
                editText.setText("")
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
                        editText.setText("Failed")
                    }
            } else {
                Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCamera() {
        detector = FirebaseVision.getInstance().onDeviceTextRecognizer
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            cameraRequestCode -> {
                if ( resultCode == Activity.RESULT_OK && data != null) {
                    imageView.setImageBitmap(data.extras!!.get("data") as Bitmap)
                }
            }
        }
    }

    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            editText.setText("No Text Found")
            return
        }
        for (block in resultText.textBlocks) {
            val blockText = block.text
            editText.append(blockText + "\n")
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