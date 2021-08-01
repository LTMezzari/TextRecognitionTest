package mezzari.torres.lucas.text_recognition_test

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions
import mezzari.torres.lucas.text_recognition_test.databinding.ActivityMainBinding
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private val executor: Executor get() = ContextCompat.getMainExecutor(this@MainActivity)

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var camera: AndroidCamera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setupCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION
            )
        }
    }

    private fun setupCamera() {
        if (camera != null) return
        camera = AndroidCamera
            .Builder()
            .setExecutor(executor)
            .setAnalysis(executor, this::analyzeImage)
            .build(binding.previewView)
    }

    @ExperimentalGetImage
    private fun generateInputImage(imageProxy: ImageProxy?): InputImage? {
        val mediaImage = imageProxy?.image ?: return null
        return InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeImage(imageProxy: ImageProxy?) {
        val image = generateInputImage(imageProxy) ?: return
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image).addOnSuccessListener { visionText ->
            val message = createMessage(visionText.textBlocks)
            if (message.isEmpty() || message.isBlank()) {
                imageProxy?.close()
                return@addOnSuccessListener
            }
            AlertDialog.Builder(this).apply {
                setTitle("Reconhecido")
                setMessage(createMessage(visionText.textBlocks))
                setCancelable(false)
                setNeutralButton("Ok") { _, _ ->
                    imageProxy?.close()
                }
            }.show()
        }.addOnFailureListener {
            imageProxy?.close()
            Toast.makeText(this, it.message ?: "Erro", Toast.LENGTH_LONG).show()
        }
    }

    private fun createMessage(blocks: List<Text.TextBlock>): String {
        if (blocks.isEmpty()) {
            return ""
        }
        var message = ""
        for (block in blocks) {
            message += "Captured block: ${block.text} {\n"
            for (line in block.lines) {
                message += "Captured line: ${line.text} {\n"
                for (element in line.elements) {
                    message += "Captured element: ${element.text}\n"
                }
                message += "}\n"
            }
            message += "}\n"
        }
        return message
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val CAMERA_PERMISSION: Int = 20
    }
}