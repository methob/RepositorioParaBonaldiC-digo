package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat.YUV_420_888
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.Utils.getCameraCharacteristics
import com.example.myapplication.Utils.getDisplayInfo
import com.example.myapplication.Utils.getPreviewOutputSize
import com.example.myapplication.VisionProcessorBase.Companion.TAG_FINAL
import com.google.mlkit.common.MlKitException

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val isPortraitMode: Boolean
        private get() = (applicationContext.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE)

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        previewView = findViewById(R.id.preview_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(
                this,
                Observer { provider: ProcessCameraProvider? ->
                    cameraProvider = provider
                    if (allPermissionsGranted()) {
                        bindAllCameraUseCases()
                    } else {
                        getRuntimePermissions()
                    }
                }
            )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bindAllCameraUseCases()
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), Companion.PERMISSION_REQUESTS
            )
        }
    }

    @JvmName("getRequiredPermissions1")
    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor =  BarcodeScannerProcessor(this)

        val builder = ImageAnalysis.Builder()
        getDisplayInfo()?.let { display ->
            getCameraCharacteristics(this)?.let { characteristics ->
                val previewSize = getPreviewOutputSize(display, characteristics, android.media.ImageReader::class.java, YUV_420_888)
                Log.d(TAG_FINAL, "AnalysisSize -> height: ${previewSize.height} -> width: ${previewSize.width} ")
                if(isPortraitMode){
                    builder.setTargetResolution(Size(previewSize.height, previewSize.width))
                }
                else {
                    builder.setTargetResolution(previewSize)
                }
            } ?: builder.setTargetResolution(Size(1920, 1080))
        }

        analysisUseCase = builder.build()
        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this@MainActivity)
        ) { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {

                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                Log.d(TAG_FINAL, "ImageAnalyzer: rotation: $rotationDegrees")
                Log.d(TAG_FINAL, "ImageAnalyzer: rotation: isFlipped: $isImageFlipped")
                if (!isPortraitMode) {
                    graphicOverlay?.setImageSourceInfo(
                        imageProxy.width,
                        imageProxy.height,
                        isImageFlipped
                    )
                } else {
                    graphicOverlay?.setImageSourceInfo(
                        imageProxy.height,
                        imageProxy.width,
                        isImageFlipped
                    )
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }
            try {
                imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector!!, analysisUseCase)
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        getDisplayInfo()?.let { display ->
            getCameraCharacteristics(this)?.let { characteristics ->
                val previewSize = getPreviewOutputSize(display, characteristics, android.media.MediaRecorder::class.java)
                Log.d(TAG_FINAL, "PreviewSize -> height: ${previewSize.height} -> width: ${previewSize.width} ")

                if(isPortraitMode){
                    builder.setTargetResolution(Size(previewSize.height, previewSize.width))
                }
                else {
                    builder.setTargetResolution(previewSize)
                }
            } ?: builder.setTargetResolution(Size(1920, 1080))
        }

        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView?.surfaceProvider)
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (allPermissionsGranted()) {
            bindAllCameraUseCases()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val requiredPermissions: Array<String?>
        get() =
            try {
                val info =
                    this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }

    private fun isPermissionGranted(context: Context, permission: String?): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission!!) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    companion object {
        private const val PERMISSION_REQUESTS = 1
    }
}