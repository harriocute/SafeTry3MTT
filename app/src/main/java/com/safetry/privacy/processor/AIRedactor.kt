package com.safetry.privacy.processor

import android.content.Context
import android.graphics.*
import com.safetry.privacy.model.DetectionResult
import com.safetry.privacy.model.DetectionCategory
import kotlin.math.max
import kotlin.math.min

class AIRedactor(private val context: Context) {

    private var objectDetector: org.tensorflow.lite.task.vision.detector.ObjectDetector? = null
    private var isInitialized = false

    private val vehicleLabels = setOf("car", "truck", "bus", "motorcycle", "vehicle", "automobile")
    private val signLabels = setOf("stop sign", "traffic light", "street sign", "sign")
    private val personLabels = setOf("person")
    private val documentLabels = setOf("book", "notebook", "document", "paper", "cell phone")

    init {
        initializeDetectors()
    }

    private fun initializeDetectors() {
        try {
            val options = org.tensorflow.lite.task.vision.detector.ObjectDetector
                .ObjectDetectorOptions.builder()
                .setMaxResults(20)
                .setScoreThreshold(0.3f)
                .build()

            val assetFiles = context.assets.list("models") ?: emptyArray()
            if (assetFiles.contains("detect.tflite")) {
                objectDetector = org.tensorflow.lite.task.vision.detector.ObjectDetector
                    .createFromFileAndOptions(context, "models/detect.tflite", options)
                isInitialized = true
            }
        } catch (e: Exception) {
            isInitialized = false
        }
    }

    fun detectSensitiveContent(bitmap: Bitmap, blurFaces: Boolean): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()

        if (isInitialized && objectDetector != null) {
            detections.addAll(runTFLiteDetection(bitmap, blurFaces))
        } else if (blurFaces) {
            detections.addAll(runFallbackFaceDetection(bitmap))
        }

        return detections
    }

    private fun runTFLiteDetection(bitmap: Bitmap, blurFaces: Boolean): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        try {
            val tensorImage = org.tensorflow.lite.support.image.TensorImage.fromBitmap(bitmap)
            val detections = objectDetector?.detect(tensorImage) ?: return results

            for (detection in detections) {
                val label = detection.categories.firstOrNull()?.label?.lowercase() ?: continue
                val score = detection.categories.firstOrNull()?.score ?: 0f
                val box = detection.boundingBox

                val category = when {
                    vehicleLabels.any { label.contains(it) } -> DetectionCategory.LICENSE_PLATE
                    signLabels.any { label.contains(it) } -> DetectionCategory.STREET_SIGN
                    personLabels.any { label.contains(it) } && blurFaces -> DetectionCategory.FACE
                    documentLabels.any { label.contains(it) } -> DetectionCategory.TEXT_DOCUMENT
                    label.contains("badge") || label.contains("card") -> DetectionCategory.ID_BADGE
                    else -> continue
                }

                val humanLabel = when (category) {
                    DetectionCategory.LICENSE_PLATE -> "License Plate"
                    DetectionCategory.STREET_SIGN -> "Street Sign"
                    DetectionCategory.FACE -> "Face"
                    DetectionCategory.TEXT_DOCUMENT -> "Text Document"
                    DetectionCategory.ID_BADGE -> "ID Badge"
                }

                results.add(DetectionResult(
                    label = humanLabel,
                    category = category,
                    confidence = score,
                    boundingBox = box
                ))
            }
        } catch (e: Exception) {
            // silently fail
        }
        return results
    }

    private fun runFallbackFaceDetection(bitmap: Bitmap): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        try {
            val width = bitmap.width
            val height = bitmap.height
            val detector = android.media.FaceDetector(width, height, 5)
            val faces = arrayOfNulls<android.media.FaceDetector.Face>(5)
            val rgb565 = bitmap.copy(Bitmap.Config.RGB_565, false)
            val faceCount = detector.findFaces(rgb565, faces)
            rgb565.recycle()

            for (i in 0 until faceCount) {
                val face = faces[i] ?: continue
                val midpoint = PointF()
                face.getMidPoint(midpoint)
                val eyeDistance = face.eyesDistance()
                val faceWidth = eyeDistance * 3f
                val faceHeight = eyeDistance * 4f

                val box = RectF(
                    max(0f, midpoint.x - faceWidth / 2),
                    max(0f, midpoint.y - faceHeight / 2),
                    min(width.toFloat(), midpoint.x + faceWidth / 2),
                    min(height.toFloat(), midpoint.y + faceHeight / 2)
                )

                results.add(DetectionResult(
                    label = "Face",
                    category = DetectionCategory.FACE,
                    confidence = face.confidence(),
                    boundingBox = box
                ))
            }
        } catch (e: Exception) {
            // face detection not available
        }
        return results
    }

    fun applyRedactions(bitmap: Bitmap, detections: List<DetectionResult>): Bitmap {
        if (detections.isEmpty()) return bitmap

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint()

        for (detection in detections) {
            val box = detection.boundingBox
            val left = max(0, box.left.toInt())
            val top = max(0, box.top.toInt())
            val right = min(mutableBitmap.width, box.right.toInt())
            val bottom = min(mutableBitmap.height, box.bottom.toInt())

            if (right <= left || bottom <= top) continue

            val region = Bitmap.createBitmap(mutableBitmap, left, top, right - left, bottom - top)
            val blurred = applyMosaicBlur(region, 12)
            canvas.drawBitmap(blurred, left.toFloat(), top.toFloat(), paint)
            region.recycle()
            blurred.recycle()
        }

        return mutableBitmap
    }

    private fun applyMosaicBlur(bitmap: Bitmap, blockSize: Int): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply { isAntiAlias = false }
        val width = bitmap.width
        val height = bitmap.height

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val blockW = min(blockSize, width - x)
                val blockH = min(blockSize, height - y)
                val centerX = min(x + blockW / 2, width - 1)
                val centerY = min(y + blockH / 2, height - 1)
                paint.color = bitmap.getPixel(centerX, centerY)
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + blockW).toFloat(), (y + blockH).toFloat(), paint)
                x += blockSize
            }
            y += blockSize
        }
        return result
    }

    fun close() {
        objectDetector?.close()
    }
}
