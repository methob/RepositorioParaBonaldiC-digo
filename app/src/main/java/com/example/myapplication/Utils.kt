package com.example.myapplication

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import java.util.*

object Utils {

    fun getMaximumOutputSize(
        context: Context
    ): Size? {
        val config = getCameraCharacteristics(context)?.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        // If image format is provided, use it to determine supported sizes; else use target class
        val allSizes = config?.getOutputSizes(StreamConfigurationMap::class.java) ?: config?.getOutputSizes(ImageFormat.JPEG)
        return allSizes?.maxByOrNull { it.height * it.width }
    }

    private fun getCameraCharacteristics(
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
}