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
    private fun loadSampleBookPage() {
        val width = 800
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw Page Background
        canvas.drawColor(Color.WHITE)

        // Draw Page Margins/Border
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)

        // Draw Document Content
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

        // Overlay Severe Dark Shadow (Linear Gradient from Bottom-Right towards Center)
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

        setOriginalBitmap(bitmap)
        Toast.makeText(this, "Sample Book Page Loaded!", Toast.LENGTH_SHORT).show()
    }

    private fun loadSampleReceipt() {
        val width = 600
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Cream background for receipt
        canvas.drawColor(Color.rgb(250, 247, 238))

        val paint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            isAntiAlias = true
        }

        // Dotted side lines
        val linePaint = Paint().apply {
            color = Color.rgb(220, 215, 200)
            strokeWidth = 3f
        }
        canvas.drawLine(15f, 0f, 15f, height.toFloat(), linePaint)
        canvas.drawLine(width - 15f, 0f, width - 15f, height.toFloat(), linePaint)

        // Store details
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("OPENCV SMART SUPERMARKET", 40f, 90f, paint)

        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("123 OpenCV Boulevard, Tech City", 45f, 130f, paint)
        canvas.drawText("TEL: (0123) 456-789", 45f, 160f, paint)

        paint.color = Color.rgb(120, 120, 120)
        canvas.drawText("-------------------------------------", 30f, 200f, paint)

        // Receipt items
        paint.color = Color.rgb(30, 30, 30)
        var yPos = 250f
        val items = listOf(
            "1x OpenCV Integration   $49.00",
            "2x Shadow Filter SDK   $20.00",
            "1x Premium UI Layout   $15.00",
            "3x Android Dev Guide   $30.00",
            "5x Kotlin Coding Mug   $25.00"
        )
        for (item in items) {
            canvas.drawText(item, 45f, yPos, paint)
            yPos += 45f
        }

        paint.color = Color.rgb(120, 120, 120)
        canvas.drawText("-------------------------------------", 30f, yPos, paint)
        yPos += 50f

        // Total
        paint.color = Color.rgb(10, 10, 10)
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("TOTAL AMOUNT:        $139.00", 40f, yPos, paint)
        yPos += 60f

        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
        canvas.drawText("Thank you for shopping with us!", 60f, yPos, paint)
        yPos += 35f
        canvas.drawText("Powered by Mobile Lab 6 Engine", 65f, yPos, paint)

        // Spotlight circular shadow overlay (RadialGradient)
        val shadowPaint = Paint().apply { isAntiAlias = true }
        val center = RadialGradient(
            width * 0.5f, height * 0.3f, // Spotlight center
            width * 0.75f,               // Spotlight radius
            intArrayOf(Color.TRANSPARENT, Color.argb(130, 30, 30, 30), Color.argb(230, 5, 5, 5)),
            floatArrayOf(0.0f, 0.6f, 1.0f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = center
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadowPaint)

        setOriginalBitmap(bitmap)
        Toast.makeText(this, "Sample Thermal Receipt Loaded!", Toast.LENGTH_SHORT).show()
    }

    private fun loadSampleGridNote() {
        val width = 800
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Yellow legal pad background
        canvas.drawColor(Color.rgb(255, 253, 208))

        // Draw horizontal grid lines
        val linePaint = Paint().apply {
            color = Color.rgb(173, 216, 230) // light blue
            strokeWidth = 2f
        }
        var lineY = 60f
        while (lineY < height) {
            canvas.drawLine(0f, lineY, width.toFloat(), lineY, linePaint)
            lineY += 40f
        }

        // Left vertical margin line in red
        val marginPaint = Paint().apply {
            color = Color.rgb(255, 182, 193) // light pinkish-red
            strokeWidth = 4f
        }
        canvas.drawLine(100f, 0f, 100f, height.toFloat(), marginPaint)

        // Write handwritten text
        val textPaint = Paint().apply {
            color = Color.rgb(0, 0, 128) // blue ink
            isAntiAlias = true
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }

        // Title
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("MEMO: OPENCV SHADOW STUDY", 120f, 100f, textPaint)

        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        
        val lines = listOf(
            "1. Test Otsu binarization performance on yellow paper.",
            "2. Validate adaptive threshold block size limits.",
            "3. Measure execution speed for high resolution scans.",
            "4. Check for native memory leakage of OpenCV Mats.",
            "5. Re-stretch contrast to get clean white backgrounds.",
            "",
            "NOTES ON ADAPTIVE THRESHOLDING:",
            "- Block size must be odd (e.g., 15, 25, 45).",
            "- Constant C subtracts offset from weighted mean.",
            "- Helps isolate handwriting from non-uniform shadows."
        )

        var yPos = 180f
        for (line in lines) {
            canvas.drawText(line, 120f, yPos, textPaint)
            yPos += 40f
        }

        // Complex uneven shadow: overlapping linear gradient shadows
        val shadowPaint1 = Paint().apply { isAntiAlias = true }
        val linearGrad1 = LinearGradient(
            0f, 0f, width * 0.8f, height * 0.8f,
            intArrayOf(Color.argb(120, 20, 20, 20), Color.TRANSPARENT),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        shadowPaint1.shader = linearGrad1
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadowPaint1)

        val shadowPaint2 = Paint().apply { isAntiAlias = true }
        val linearGrad2 = LinearGradient(
            width.toFloat(), height.toFloat(), width * 0.3f, height * 0.3f,
            intArrayOf(Color.argb(160, 10, 10, 10), Color.TRANSPARENT),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        shadowPaint2.shader = linearGrad2
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadowPaint2)

        setOriginalBitmap(bitmap)
        Toast.makeText(this, "Sample Grid Note Loaded!", Toast.LENGTH_SHORT).show()
    }

    private fun setOriginalBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
        processedBitmap = null
        isShowingOriginal = false
        
        binding.ivOriginal.setImageBitmap(bitmap)
        binding.ivOriginal.clearColorFilter()
        binding.tvOriginalPlaceholder.visibility = View.GONE

        // Clear previous processed image preview
        binding.ivFiltered.setImageBitmap(null)
        binding.tvFilteredPlaceholder.visibility = View.VISIBLE
        binding.tvFilteredPlaceholder.text = "Run filter to see result"

        // Hide performance statistics view until processed
        binding.statsContainer.visibility = View.GONE
    }


    private fun setupPresets() {
        binding.presetChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            when (checkedIds.first()) {
                R.id.chipPresetBook -> {
                    // Update Sliders
                    binding.sbDilation.progress = 7
                    binding.sbMedianBlur.progress = 10 // (10 * 2) + 1 = 21
                    binding.filterModeChipGroup.check(R.id.chipFilterColor)
                    
                    // Hide adaptive sliders
                    binding.layoutBlockSize.visibility = View.GONE
                    binding.layoutConstantC.visibility = View.GONE
                    
                    // Generate Book Page Image
                    loadSampleBookPage()
                }
                R.id.chipPresetReceipt -> {
                    // Update Sliders
                    binding.sbDilation.progress = 5
                    binding.sbMedianBlur.progress = 5 // (5 * 2) + 1 = 11
                    binding.filterModeChipGroup.check(R.id.chipFilterOtsu)
                    
                    // Hide adaptive sliders
                    binding.layoutBlockSize.visibility = View.GONE
                    binding.layoutConstantC.visibility = View.GONE
                    
                    // Generate Receipt Image
                    loadSampleReceipt()
                }
                R.id.chipPresetNote -> {
                    // Update Sliders
                    binding.sbDilation.progress = 9
                    binding.sbMedianBlur.progress = 15 // (15 * 2) + 1 = 31
                    binding.filterModeChipGroup.check(R.id.chipFilterAdaptive)
                    
                    // Show adaptive sliders
                    binding.sbBlockSize.progress = 11 // (11 * 2) + 3 = 25
                    binding.sbConstantC.progress = 32 // 32 - 20 = 12.0
                    binding.layoutBlockSize.visibility = View.VISIBLE
                    binding.layoutConstantC.visibility = View.VISIBLE
                    
                    // Generate Note Image
                    loadSampleGridNote()
                }
                R.id.chipPresetCustom -> {
                    // Custom does not regenerate sample image, it just gives manual control
                }
            }
        }

        binding.filterModeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            // Toggle visibility of adaptive threshold parameters
            if (checkedIds.first() == R.id.chipFilterAdaptive) {
                binding.layoutBlockSize.visibility = View.VISIBLE
                binding.layoutConstantC.visibility = View.VISIBLE
            } else {
                binding.layoutBlockSize.visibility = View.GONE
                binding.layoutConstantC.visibility = View.GONE
            }
            binding.presetChipGroup.check(R.id.chipPresetCustom)
        }
    }

    private fun setupSeekBars() {
        // Dilation SeekBar
        binding.sbDilation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val safeProgress = progress.coerceAtLeast(1)
                binding.tvDilationLabel.text = getString(R.string.lbl_dilation_kernel, safeProgress)
                if (fromUser) {
                    binding.presetChipGroup.check(R.id.chipPresetCustom)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Median Blur SeekBar
        binding.sbMedianBlur.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actual = (progress * 2) + 1
                binding.tvMedianBlurLabel.text = getString(R.string.lbl_median_blur, actual)
                if (fromUser) {
                    binding.presetChipGroup.check(R.id.chipPresetCustom)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Block Size SeekBar
        binding.sbBlockSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actual = (progress * 2) + 3
                binding.tvBlockSizeLabel.text = getString(R.string.lbl_block_size, actual)
                if (fromUser) {
                    binding.presetChipGroup.check(R.id.chipPresetCustom)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Constant C SeekBar
        binding.sbConstantC.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actual = (progress - 20).toDouble()
                binding.tvConstantCLabel.text = getString(R.string.lbl_constant_c, actual)
                if (fromUser) {
                    binding.presetChipGroup.check(R.id.chipPresetCustom)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun getDilationSize() = binding.sbDilation.progress.coerceAtLeast(1)
    
    private fun getMedianBlurSize() = (binding.sbMedianBlur.progress * 2) + 1
    
    private fun getBlockSize() = (binding.sbBlockSize.progress * 2) + 3
    
    private fun getConstantC() = (binding.sbConstantC.progress - 20).toDouble()
    
    private fun getFilterType(): String {
        return when (binding.filterModeChipGroup.checkedChipId) {
            R.id.chipFilterColor -> "color"
            R.id.chipFilterGray -> "grayscale"
            R.id.chipFilterOtsu -> "otsu"
            R.id.chipFilterAdaptive -> "adaptive"
            else -> "color"
        }
    }

    /**
     * Applies the OpenCV shadow removal process on the currently loaded image.
     */
    private fun applyShadowRemoval() {
        val bitmap = originalBitmap
        if (bitmap == null) {
            Toast.makeText(this, "Please load a sample or select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show Progress Bar and hide button text
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRemoveShadow.text = ""
        binding.btnRemoveShadow.isEnabled = false

        val dSize = getDilationSize()
        val mSize = getMedianBlurSize()
        val filter = getFilterType()
        val bSize = getBlockSize()
        val constC = getConstantC()

        ShadowRemovalFilter.getShadowFilteredImage(
            bitmap = bitmap,
            dilationSize = dSize,
            blurSize = mSize,
            filterType = filter,
            blockSize = bSize,
            constantC = constC,
            callback = object : ShadowRemovalFilter.Callback {
                override fun onComplete(resultBitmap: Bitmap, executionTimeMs: Long, width: Int, height: Int) {
                    processedBitmap = resultBitmap
                    isShowingOriginal = false

                    // Render Processed Image
                    binding.ivFiltered.setImageBitmap(resultBitmap)
                    binding.tvFilteredPlaceholder.visibility = View.GONE

                    // Update Stats Container
                    binding.tvResolution.text = getString(R.string.lbl_resolution, width, height)
                    binding.tvExecutionTime.text = getString(R.string.lbl_time_taken, executionTimeMs)
                    binding.statsContainer.visibility = View.VISIBLE

                    // Hide Progress Bar and restore button
                    binding.progressBar.visibility = View.GONE
                    binding.btnRemoveShadow.text = getString(R.string.btn_apply_filter)
                    binding.btnRemoveShadow.isEnabled = true

                    Toast.makeText(this@MainActivity, "Shadow Removed Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

