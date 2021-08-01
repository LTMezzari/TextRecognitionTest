package mezzari.torres.lucas.text_recognition_test

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

/**
 * @author Lucas T. Mezzari
 * @since 01/08/2021
 */
class AndroidCamera private constructor() {
    private lateinit var camera: Camera

    private var cameraLens: Int = CameraSelector.LENS_FACING_BACK
    private var analysisStrategy: Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    private var executor: Executor? = null
    private var analysisExecutor: Executor? = null
    private var analysisCallback: ((ImageProxy) -> Unit)? = null

    private var _preview: Preview? = null
    private var _cameraSelector: CameraSelector? = null
    private var _imageAnalysis: ImageAnalysis? = null

    private fun setupCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProvider = ProcessCameraProvider.getInstance(context)
        val executor = executor ?: return
        cameraProvider.addListener({
            val provider = cameraProvider.get()
            val preview = getPreview(previewView)
            val selector = getSelector()
            val analysis = getImageAnalyser()
            if (analysis != null) {
                camera = provider.bindToLifecycle(lifecycleOwner, selector, analysis, preview)
                return@addListener
            }
            provider.bindToLifecycle(lifecycleOwner, selector, preview)
        }, executor)
    }

    private fun getPreview(previewView: PreviewView): Preview {
        this._preview?.run {
            return this
        }

        return Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    private fun getSelector(): CameraSelector {
        this._cameraSelector?.run {
            return this
        }
        return CameraSelector
            .Builder()
            .requireLensFacing(cameraLens)
            .build()
    }

    private fun getImageAnalyser(): ImageAnalysis? {
        this._imageAnalysis?.run {
            return this
        }
        val executor = analysisExecutor ?: return null
        val callback = analysisCallback ?: return null
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(analysisStrategy)
            .build().apply {
                setAnalyzer(executor, callback)
            }
    }

    class Builder {

        private val instance = AndroidCamera()

        fun setExecutor(executor: Executor): Builder {
            instance.executor = executor
            return this
        }

        fun setPreview(preview: Preview): Builder {
            instance._preview = preview
            return this
        }

        fun setCameraSelector(cameraSelector: CameraSelector): Builder {
            instance._cameraSelector = cameraSelector
            return this
        }

        fun setImageAnalysis(imageAnalysis: ImageAnalysis): Builder {
            instance._imageAnalysis = imageAnalysis
            return this
        }

        fun setCameraLens(cameraLens: Int): Builder {
            instance.cameraLens = cameraLens
            return this
        }

        fun setAnalysisStrategy(analysisStrategy: Int): Builder {
            instance.analysisStrategy = analysisStrategy
            return this
        }

        fun setAnalysis(executor: Executor, callback: (ImageProxy) -> Unit): Builder {
            instance.analysisExecutor = executor
            instance.analysisCallback = callback
            return this
        }

        fun build(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView): AndroidCamera {
            instance.setupCamera(context, lifecycleOwner, previewView)
            return instance
        }

        fun build(lifecycleOwner: LifecycleOwner, previewView: PreviewView): AndroidCamera {
            instance.setupCamera(previewView.context, lifecycleOwner, previewView)
            return instance
        }

        fun build(previewView: PreviewView): AndroidCamera {
            instance.setupCamera(previewView.context, previewView.context as LifecycleOwner, previewView)
            return instance
        }
    }
}