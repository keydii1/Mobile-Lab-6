package com.example.lab6

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.ArrayList
import java.util.concurrent.Executors

object ShadowRemovalFilter {
    interface Callback {
        fun onComplete(bitmap: Bitmap, executionTimeMs: Long, width: Int, height: Int)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())


    fun getShadowFilteredImage(
        bitmap: Bitmap,
        dilationSize: Int = 7,
        blurSize: Int = 21,
        filterType: String = "color",
        blockSize: Int = 15,
        constantC: Double = 10.0,
        callback: Callback
    ) {
        executor.execute {
            try {
                val startTime = System.currentTimeMillis()
                
                // 1. Convert Bitmap to Mat (typically RGBA)
                val srcArray = Mat()
                Utils.bitmapToMat(bitmap, srcArray)

                val width = bitmap.width
                val height = bitmap.height

                // 2. Convert from RGB to HSV (since bitmapToMat brings RGB/RGBA)
                val hsvMat = Mat()
                Imgproc.cvtColor(srcArray, hsvMat, Imgproc.COLOR_RGB2HSV)

                // 3. Split HSV channels
                val hsvPlanes = ArrayList<Mat>()
                Core.split(hsvMat, hsvPlanes)

                // hsvPlanes[0] -> H, hsvPlanes[1] -> S, hsvPlanes[2] -> V
                val vChannel = hsvPlanes[2]

                // 4. Perform Shadow Removal on V channel
                val dilatedImg = Mat()
                val dSize = if (dilationSize < 1) 1 else dilationSize
                val kernel = Mat.ones(dSize, dSize, CvType.CV_32F)
                Imgproc.dilate(vChannel, dilatedImg, kernel)
                
                var bSize = if (blurSize % 2 == 0) blurSize + 1 else blurSize
                if (bSize < 3) bSize = 3
                Imgproc.medianBlur(dilatedImg, dilatedImg, bSize)

                val diffMat = Mat()
                Core.absdiff(vChannel, dilatedImg, diffMat)
                Core.bitwise_not(diffMat, diffMat)

                val normMat = diffMat.clone()
                Core.normalize(diffMat, normMat, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)

                val resultRgb = Mat()

                // Apply selected filter mode
                when (filterType) {
                    "color" -> {
                        val resultPlanes = ArrayList<Mat>()
                        resultPlanes.add(hsvPlanes[0]) // H
                        resultPlanes.add(hsvPlanes[1]) // S
                        resultPlanes.add(normMat)      // Processed V

                        val resultHsv = Mat()
                        Core.merge(resultPlanes, resultHsv)
                        Imgproc.cvtColor(resultHsv, resultRgb, Imgproc.COLOR_HSV2RGB)
                        resultHsv.release()
                        for (m in resultPlanes) {
                            m.release()
                        }
                    }
                    "grayscale" -> {
                        Imgproc.cvtColor(normMat, resultRgb, Imgproc.COLOR_GRAY2RGB)
                    }
                    "otsu" -> {
                        val binaryMat = Mat()
                        Imgproc.threshold(normMat, binaryMat, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
                        Imgproc.cvtColor(binaryMat, resultRgb, Imgproc.COLOR_GRAY2RGB)
                        binaryMat.release()
                    }
                    "adaptive" -> {
                        val binaryMat = Mat()
                        var adBlockSize = if (blockSize % 2 == 0) blockSize + 1 else blockSize
                        if (adBlockSize < 3) adBlockSize = 3
                        Imgproc.adaptiveThreshold(
                            normMat,
                            binaryMat,
                            255.0,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            adBlockSize,
                            constantC
                        )
                        Imgproc.cvtColor(binaryMat, resultRgb, Imgproc.COLOR_GRAY2RGB)
                        binaryMat.release()
                    }
                    else -> {
                        val resultPlanes = ArrayList<Mat>()
                        resultPlanes.add(hsvPlanes[0])
                        resultPlanes.add(hsvPlanes[1])
                        resultPlanes.add(normMat)

                        val resultHsv = Mat()
                        Core.merge(resultPlanes, resultHsv)
                        Imgproc.cvtColor(resultHsv, resultRgb, Imgproc.COLOR_HSV2RGB)
                        resultHsv.release()
                        for (m in resultPlanes) {
                            m.release()
                        }
                    }
                }

                // 5. Convert Mat back to Bitmap
                val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(resultRgb, resultBitmap)

                // Release native Mat memory
                srcArray.release()
                hsvMat.release()
                for (m in hsvPlanes) {
                    m.release()
                }
                dilatedImg.release()
                kernel.release()
                diffMat.release()
                normMat.release()
                resultRgb.release()

                val executionTime = System.currentTimeMillis() - startTime

                // 6. Return result on UI Thread
                handler.post {
                    callback.onComplete(resultBitmap, executionTime, width, height)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

