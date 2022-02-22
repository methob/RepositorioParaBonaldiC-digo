package com.example.myapplication

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Size
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import java.util.*
import kotlin.math.max
import kotlin.math.min

object Utils {

    fun getMaximumOutputSize(
        context: Context,
        display: Display
    ): Size? {
        val config = getCameraCharacteristics(context)?.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        // If image format is provided, use it to determine supported sizes; else use target class
        val allSizes = config?.getOutputSizes(StreamConfigurationMap::class.java) ?: config?.getOutputSizes(ImageFormat.JPEG)
        val screenSize = getDisplaySmartSize(display)
        val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
        val maxSize = if (hdScreen) SIZE_1080P else screenSize
        // Get available sizes and sort them by area from largest to smallest
        val validSizes = allSizes
            ?.sortedWith(compareBy { it.height * it.width })
            ?.map { SmartSize(it.width, it.height) }?.reversed()

        // Then, get the largest output size that is smaller or equal than our max size
        return validSizes?.first { it.long <= maxSize.long && it.short <= maxSize.short }?.size
    }

    fun getCameraCharacteristics(
        context: Context, lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ): CameraCharacteristics? {
        val cameraManager = context.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
        try {
            val cameraList = Arrays.asList(*cameraManager.cameraIdList)
            for (availableCameraId in cameraList) {
                val availableCameraCharacteristics = cameraManager.getCameraCharacteristics(
                    availableCameraId!!
                )
                val availableLensFacing =
                    availableCameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        ?: continue
                if (availableLensFacing == lensFacing) {
                    return availableCameraCharacteristics
                }
            }
        } catch (e: CameraAccessException) {
            // Accessing camera ID info got error
        }
        return null
    }

    /** Helper class used to pre-compute shortest and longest sides of a [Size] */
    class SmartSize(width: Int, height: Int) {
        var size = Size(width, height)
        var long = max(size.width, size.height)
        var short = min(size.width, size.height)
        override fun toString() = "SmartSize(${long}x${short})"
    }

    /** Standard High Definition size for pictures and video */
    val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

    /** Returns a [SmartSize] object for the given [Display] */
    fun getDisplaySmartSize(display: Display): SmartSize {
        val outPoint = Point()
        display.getRealSize(outPoint)
        return SmartSize(outPoint.x, outPoint.y)
    }

    /**
     * Returns the largest available PREVIEW size. For more information, see:
     * https://d.android.com/reference/android/hardware/camera2/CameraDevice
     */
    fun <T>getPreviewOutputSize(
        display: Display,
        characteristics: CameraCharacteristics,
        targetClass: Class<T>,
        format: Int? = null
    ): Size {

        // Find which is smaller: screen or 1080p
        val screenSize = getDisplaySmartSize(display)
        val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
        val maxSize = if (hdScreen) SIZE_1080P else screenSize

        // If image format is provided, use it to determine supported sizes; else use target class
        val config = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        if (format == null)
            assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
        else
            assert(config.isOutputSupportedFor(format))
        val allSizes = if (format == null)
            config.getOutputSizes(targetClass) else config.getOutputSizes(format)

        // Get available sizes and sort them by area from largest to smallest
        val validSizes = allSizes
            .sortedWith(compareBy { it.height * it.width })
            .map { SmartSize(it.width, it.height) }.reversed()

        // Then, get the largest output size that is smaller or equal than our max size
        return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
    }

    fun AppCompatActivity.getDisplayInfo(): Display? {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.display
        }
        else {
            windowManager.defaultDisplay
        }
    }
}