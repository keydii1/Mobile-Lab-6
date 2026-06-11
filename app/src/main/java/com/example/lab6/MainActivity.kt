package com.example.lab6

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lab6.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var originalBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null
    private var isShowingOriginal = false


    // Register Activity Result Launcher for selecting image from Gallery
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                // Convert bitmap to ARGB_8888 to ensure compatibility with OpenCV
                val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                originalBitmap = argbBitmap
                processedBitmap = null
                isShowingOriginal = false
                
                binding.ivOriginal.setImageBitmap(argbBitmap)
                binding.ivOriginal.clearColorFilter()
                binding.tvOriginalPlaceholder.visibility = View.GONE
                
                // Clear previous result
                binding.ivFiltered.setImageBitmap(null)
                binding.tvFilteredPlaceholder.visibility = View.VISIBLE
                binding.tvFilteredPlaceholder.text = "Run filter to see result"
                
                // Hide stats view
                binding.statsContainer.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OpenCV
        if (OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV Loaded Successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "OpenCV Initialization Failed!", Toast.LENGTH_LONG).show()
        }

        // Set Click Listeners
        binding.btnLoadSample.setOnClickListener {
            loadSampleDocument()
        }

        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnRemoveShadow.setOnClickListener {
            applyShadowRemoval()
        }
    }

    /**
     * Generates a high-quality mock document page featuring dark gradient shadow overlays,
     * simulating a real-world shadowed book page.
     */
    private fun loadSampleDocument() {
        val width = 800
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 1. Draw Page Background
        canvas.drawColor(Color.WHITE)

        // 2. Draw Page Margins/Border
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)

        // 3. Draw Document Content
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        // Title
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MOBILE LAB 6: SHADOW REMOVAL", 60f, 100f, paint)

        // Subtitle
        paint.textSize = 24f
        paint.color = Color.GRAY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("OpenCV Document Image Processing", 60f, 140f, paint)

        // Horizontal Line
        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(60f, 160f, width - 60f, 160f, paint)

        // Reset Paint for text body
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 20f

        val lines = listOf(
            "Welcome to Mobile Lab 6 Exercise on OpenCV integration.",
            "This document simulates a book page captured under poor lighting,",
            "where severe shadows are cast by the camera or nearby obstacles.",
            "",
            "Shadow removal is a crucial pre-processing step for document",
            "digitization, Optical Character Recognition (OCR), and search.",
            "",
            "HOW THE OPENCV ALGORITHM WORKS:",
            "1. The RGB image is converted into the HSV color space.",
            "2. The V (Value/Brightness) channel is isolated for processing.",
            "3. Morphological dilation estimates the background illumination.",
            "4. A median blur filter smooths out high frequency text features.",
            "5. Core.absdiff calculates the difference between original and background.",
            "6. Core.normalize re-stretches the contrast back to [0, 255].",
            "7. The processed V channel is merged back with unmodified H & S.",
            "",
            "Notice how the dark shadow gradient overlay on the lower right area",
            "of this original image is perfectly compensated and cleaned by the",
            "algorithm, resulting in a crisp, readable black-and-white scan.",
            "",
            "--- Smart Software System Team (S3T), March 2026 ---"
        )

        var yPos = 210f
        for (line in lines) {
            if (line.startsWith("HOW THE OPENCV ALGORITHM WORKS:")) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.rgb(80, 80, 80)
            } else {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.BLACK
            }
            canvas.drawText(line, 60f, yPos, paint)
            yPos += 32f
        }

        // 4. Overlay Severe Dark Shadow (Linear Gradient from Bottom-Right towards Center)
        val shadowPaint = Paint().apply {
            isAntiAlias = true
        }
        val gradient = LinearGradient(
            width * 0.4f, height * 0.4f,  // Start coordinates (semi-transparent)
            width.toFloat(), height.toFloat(), // End coordinates (severe dark)
            intArrayOf(Color.TRANSPARENT, Color.argb(160, 20, 20, 20), Color.argb(220, 10, 10, 10)),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadowPaint)

        // Set as current bitmap
        currentBitmap = bitmap
        binding.ivOriginal.setImageBitmap(bitmap)
        binding.ivOriginal.clearColorFilter()
        binding.tvOriginalPlaceholder.visibility = View.GONE

        // Reset Filter result state
        binding.ivFiltered.setImageBitmap(null)
        binding.tvFilteredPlaceholder.visibility = View.VISIBLE
        binding.tvFilteredPlaceholder.text = "Run filter to see result"

        Toast.makeText(this, "Sample Document with Shadow Loaded!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Applies the OpenCV shadow removal process on the currently loaded image.
     */
    private fun applyShadowRemoval() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "Please load a sample or select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show Progress Bar and hide button text
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRemoveShadow.text = ""
        binding.btnRemoveShadow.isEnabled = false

        ShadowRemovalFilter.getShadowFilteredImage(bitmap, object : ShadowRemovalFilter.Callback {
            override fun onComplete(bitmap: Bitmap) {
                // Render Processed Image
                binding.ivFiltered.setImageBitmap(bitmap)
                binding.tvFilteredPlaceholder.visibility = View.GONE

                // Hide Progress Bar and restore button
                binding.progressBar.visibility = View.GONE
                binding.btnRemoveShadow.text = "Remove Shadow (OpenCV)"
                binding.btnRemoveShadow.isEnabled = true

                Toast.makeText(this@MainActivity, "Shadow Removed Successfully!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
