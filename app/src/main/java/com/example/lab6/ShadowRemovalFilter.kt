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
        fun onComplete(bitmap: Bitmap)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    fun getShadowFilteredImage(bitmap: Bitmap, callback: Callback) {
        executor.execute {
            try {
                // 1. Convert Bitmap to Mat (typically RGBA)
                val srcArray = Mat()
                Utils.bitmapToMat(bitmap, srcArray)

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
                val kernel = Mat.ones(7, 7, CvType.CV_32F)
                Imgproc.dilate(vChannel, dilatedImg, kernel)
                Imgproc.medianBlur(dilatedImg, dilatedImg, 21)

                val diffMat = Mat()
                Core.absdiff(vChannel, dilatedImg, diffMat)
                Core.bitwise_not(diffMat, diffMat)

                val normMat = diffMat.clone()
                Core.normalize(diffMat, normMat, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)

                // 5. Merge channels back
                val resultPlanes = ArrayList<Mat>()
                resultPlanes.add(hsvPlanes[0]) // H
                resultPlanes.add(hsvPlanes[1]) // S
                resultPlanes.add(normMat)      // Processed V

                val resultHsv = Mat()
                Core.merge(resultPlanes, resultHsv)

                // 6. Convert HSV back to RGB
                val resultRgb = Mat()
                Imgproc.cvtColor(resultHsv, resultRgb, Imgproc.COLOR_HSV2RGB)

                // 7. Convert Mat back to Bitmap
                val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
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
                for (m in resultPlanes) {
                    m.release()
                }
                resultHsv.release()
                resultRgb.release()

                // 8. Return result on UI Thread
                handler.post {
                    callback.onComplete(resultBitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
