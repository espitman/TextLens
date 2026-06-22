package com.textlens.android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import android.view.MotionEvent
import android.view.View
import com.textlens.android.core.ScreenArea
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SelectionOverlayView(
    context: Context,
    private val initialCenterX: Float? = null,
    private val initialCenterY: Float? = null,
    private val onComplete: (ScreenArea?) -> Unit,
) : View(context) {
    private enum class DragMode {
        None,
        Move,
        Resize,
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(42, 245, 200, 74)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(24, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.FILL
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.FILL
    }
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(7, 7, 7)
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = persianTypeface(context)
    }
    private val selectionRect = RectF()
    private val confirmRect = RectF()
    private val cancelRect = RectF()
    private val resizeHandleRect = RectF()
    private var dragMode = DragMode.None
    private var lastX = 0f
    private var lastY = 0f
    private val minSizePx = 90f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        ensureInitialRect()

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        canvas.drawRect(selectionRect, fillPaint)
        canvas.drawRect(selectionRect, borderPaint)
        canvas.drawRect(
            selectionRect.left + 5f,
            selectionRect.top + 5f,
            selectionRect.right - 5f,
            selectionRect.bottom - 5f,
            innerPaint,
        )

        resizeHandleRect.set(
            selectionRect.right - 34f,
            selectionRect.bottom - 34f,
            selectionRect.right + 10f,
            selectionRect.bottom + 10f,
        )
        canvas.drawRoundRect(resizeHandleRect, 12f, 12f, handlePaint)

        confirmRect.set(
            selectionRect.left,
            selectionRect.bottom + 18f,
            selectionRect.left + 132f,
            selectionRect.bottom + 72f,
        )
        cancelRect.set(
            selectionRect.right - 116f,
            selectionRect.bottom + 18f,
            selectionRect.right,
            selectionRect.bottom + 72f,
        )
        keepActionButtonsOnScreen()
        drawActionButton(canvas, confirmRect, "Translate")
        drawActionButton(canvas, cancelRect, "Cancel")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        ensureInitialRect()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragMode = when {
                    confirmRect.contains(event.x, event.y) -> {
                        completeSelection()
                        DragMode.None
                    }
                    cancelRect.contains(event.x, event.y) -> {
                        onComplete(null)
                        DragMode.None
                    }
                    resizeHandleRect.contains(event.x, event.y) -> DragMode.Resize
                    selectionRect.contains(event.x, event.y) -> DragMode.Move
                    else -> DragMode.None
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                when (dragMode) {
                    DragMode.Move -> moveSelection(dx, dy)
                    DragMode.Resize -> resizeSelection(dx, dy)
                    DragMode.None -> Unit
                }
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                dragMode = DragMode.None
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragMode = DragMode.None
                onComplete(null)
                return true
            }
        }
        return true
    }

    private fun ensureInitialRect() {
        if (!selectionRect.isEmpty || width == 0 || height == 0) return
        val rectWidth = width * 0.72f
        val rectHeight = height * 0.24f
        val centerX = initialCenterX ?: (width / 2f)
        val centerY = initialCenterY ?: (height / 2.8f + rectHeight / 2f)
        val left = centerX - rectWidth / 2f
        val top = centerY - rectHeight / 2f
        selectionRect.set(left, top, left + rectWidth, top + rectHeight)
        clampSelection()
    }

    private fun moveSelection(dx: Float, dy: Float) {
        selectionRect.offset(dx, dy)
        clampSelection()
    }

    private fun resizeSelection(dx: Float, dy: Float) {
        selectionRect.right = (selectionRect.right + dx).coerceAtMost(width - 16f)
        selectionRect.bottom = (selectionRect.bottom + dy).coerceAtMost(height - 92f)
        if (selectionRect.width() < minSizePx) selectionRect.right = selectionRect.left + minSizePx
        if (selectionRect.height() < minSizePx) selectionRect.bottom = selectionRect.top + minSizePx
        clampSelection()
    }

    private fun clampSelection() {
        val maxBottom = height - 92f
        if (selectionRect.left < 16f) selectionRect.offset(16f - selectionRect.left, 0f)
        if (selectionRect.top < 16f) selectionRect.offset(0f, 16f - selectionRect.top)
        if (selectionRect.right > width - 16f) selectionRect.offset(width - 16f - selectionRect.right, 0f)
        if (selectionRect.bottom > maxBottom) selectionRect.offset(0f, maxBottom - selectionRect.bottom)
    }

    private fun keepActionButtonsOnScreen() {
        if (confirmRect.right > width - 16f) confirmRect.offset(width - 16f - confirmRect.right, 0f)
        if (cancelRect.left < 16f) cancelRect.offset(16f - cancelRect.left, 0f)
        if (confirmRect.bottom > height - 16f) confirmRect.offset(0f, height - 16f - confirmRect.bottom)
        if (cancelRect.bottom > height - 16f) cancelRect.offset(0f, height - 16f - cancelRect.bottom)
    }

    private fun completeSelection() {
        val screenLocation = IntArray(2)
        getLocationOnScreen(screenLocation)
        val captureRect = RectF(
            screenLocation[0] + selectionRect.left,
            screenLocation[1] + selectionRect.top,
            screenLocation[0] + selectionRect.right,
            screenLocation[1] + selectionRect.bottom,
        ).apply {
            left = left.coerceAtLeast(0f)
            top = top.coerceAtLeast(0f)
            right = right.coerceAtMost(resources.displayMetrics.widthPixels.toFloat())
            bottom = bottom.coerceAtMost(resources.displayMetrics.heightPixels.toFloat())
        }
        val area = ScreenArea(
            left = captureRect.left.toInt(),
            top = captureRect.top.toInt(),
            width = captureRect.width().toInt(),
            height = captureRect.height().toInt(),
        )
        onComplete(area.takeIf { it.isMeaningful })
    }

    private fun drawActionButton(canvas: Canvas, rect: RectF, label: String) {
        canvas.drawRoundRect(rect, 18f, 18f, buttonPaint)
        canvas.drawText(label, rect.centerX(), rect.centerY() + 9f, buttonTextPaint)
    }
}

class TranslationPopupView(context: Context) : View(context) {
    sealed class State {
        data object Loading : State()
        data class Result(val text: String, val model: String, val costToman: Int?) : State()
        data class Error(val message: String) : State()
    }

    var state: State = State.Loading
        set(value) {
            field = value
            invalidate()
        }

    var onClose: (() -> Unit)? = null
    var onRetry: (() -> Unit)? = null
    var onCopy: (() -> Unit)? = null
    var onSwitchModel: (() -> Unit)? = null
    var onDragBy: ((dx: Int, dy: Int) -> Unit)? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(9, 9, 9)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.FILL
    }
    private val loaderTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(54, 245, 200, 74)
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
    }
    private val loaderArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(247, 240, 220)
        textSize = 34f
        typeface = persianTypeface(context)
    }
    private val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 247, 240, 220)
        textSize = 22f
        typeface = persianTypeface(context)
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 200, 74)
        style = Paint.Style.FILL
    }
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(7, 7, 7)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        typeface = persianTypeface(context)
    }
    private val closeRect = RectF()
    private val retryRect = RectF()
    private val copyRect = RectF()
    private val modelRect = RectF()
    private var downX = 0f
    private var downY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var scrollOffset = 0f
    private var resultContentHeight = 0f

    override fun onDraw(canvas: Canvas) {
        resetMainTextPaint()
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, 34f, 34f, bgPaint)
        canvas.drawRoundRect(bounds.insetCopy(2f), 34f, 34f, borderPaint)

        canvas.drawText("TextLens", 32f, 54f, textPaint)
        canvas.drawText("Translation", width - 34f, 54f, textPaint.apply { textAlign = Paint.Align.RIGHT })
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawLine(28f, 82f, width - 28f, 82f, borderPaint.apply { alpha = 90 })
        borderPaint.alpha = 255

        when (val current = state) {
            State.Loading -> drawLoading(canvas)
            is State.Result -> drawResult(canvas, current)
            is State.Error -> drawError(canvas, current.message)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX).toInt()
                val dy = (event.rawY - lastRawY).toInt()
                lastRawX = event.rawX
                lastRawY = event.rawY
                if (downY <= 90f) {
                    onDragBy?.invoke(dx, dy)
                } else if (state is State.Result) {
                    val maxScroll = (resultContentHeight - (height - 220f)).coerceAtLeast(0f)
                    scrollOffset = (scrollOffset - dy).coerceIn(0f, maxScroll)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.x - downX) > 12 || abs(event.y - downY) > 12) return false
                when {
                    closeRect.contains(event.x, event.y) -> onClose?.invoke()
                    retryRect.contains(event.x, event.y) -> onRetry?.invoke()
                    copyRect.contains(event.x, event.y) -> onCopy?.invoke()
                    modelRect.contains(event.x, event.y) -> onSwitchModel?.invoke()
                }
                return true
            }
        }
        return true
    }

    private fun drawLoading(canvas: Canvas) {
        val radius = 38f
        val centerX = width / 2f
        val centerY = height / 2f
        val spinnerBounds = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        val angle = ((SystemClock.uptimeMillis() / 5L) % 360L).toFloat()
        canvas.drawArc(spinnerBounds, 0f, 360f, false, loaderTrackPaint)
        canvas.drawArc(spinnerBounds, angle, 94f, false, loaderArcPaint)
        postInvalidateDelayed(16L)
        drawButton(canvas, closeRect.setAndReturn(width - 156f, height - 66f, width - 30f, height - 20f), "Cancel")
    }

    private fun drawResult(canvas: Canvas, result: State.Result) {
        val textArea = Rect(34, 108, width - 34, height - 104)
        val displayText = result.text.normalizedForPersianDisplay()
        val layout = StaticLayout.Builder.obtain(displayText, 0, displayText.length, textPaint.apply {
            textSize = 48f
            textAlign = Paint.Align.LEFT
        }, textArea.width())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(14f, 1f)
            .setIncludePad(false)
            .build()
        resultContentHeight = layout.height.toFloat()
        canvas.save()
        canvas.clipRect(textArea)
        canvas.translate(textArea.left.toFloat(), textArea.top.toFloat())
        canvas.translate(0f, -scrollOffset)
        layout.draw(canvas)
        canvas.restore()
        textPaint.textAlign = Paint.Align.LEFT

        val cost = when (result.costToman) {
            0 -> "Cost: Free"
            null -> "Cost: Unknown"
            else -> "Cost: ${result.costToman} toman"
        }
        canvas.drawText(cost, 38f, height - 38f, mutedPaint)
        drawButton(canvas, copyRect.setAndReturn(width - 292f, height - 72f, width - 164f, height - 22f), "Copy")
        drawButton(canvas, closeRect.setAndReturn(width - 148f, height - 72f, width - 28f, height - 22f), "Close")
    }

    private fun resetMainTextPaint() {
        textPaint.color = Color.rgb(247, 240, 220)
        textPaint.textSize = 34f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = persianTypeface(context)
    }

    private fun drawError(canvas: Canvas, message: String) {
        textPaint.color = Color.rgb(255, 95, 95)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("!", width / 2f, 142f, textPaint.apply { textSize = 48f })
        val safeMessage = message.ifBlank { "An unknown error occurred." }
        val contentWidth = (width - 72).coerceAtLeast(160)
        val layout = StaticLayout.Builder.obtain(safeMessage, 0, safeMessage.length, textPaint.apply { textSize = 22f }, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(6f, 1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(((width - contentWidth) / 2f), 174f)
        layout.draw(canvas)
        canvas.restore()
        textPaint.color = Color.rgb(247, 240, 220)
        textPaint.textAlign = Paint.Align.LEFT
        drawButton(canvas, modelRect.setAndReturn(width - 420f, height - 72f, width - 300f, height - 22f), "Model")
        drawButton(canvas, retryRect.setAndReturn(width - 292f, height - 72f, width - 164f, height - 22f), "Retry")
        drawButton(canvas, closeRect.setAndReturn(width - 148f, height - 72f, width - 28f, height - 22f), "Close")
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String) {
        canvas.drawRoundRect(rect, 18f, 18f, buttonPaint)
        canvas.drawText(label, rect.centerX(), rect.centerY() + 9f, buttonTextPaint)
    }
}

private fun RectF.insetCopy(amount: Float): RectF =
    RectF(left + amount, top + amount, right - amount, bottom - amount)

private fun RectF.setAndReturn(left: Float, top: Float, right: Float, bottom: Float): RectF {
    set(left, top, right, bottom)
    return this
}

private fun String.normalizedForPersianDisplay(): String =
    replace('\u00A0', ' ')
        .replace("\t", " ")
        .lines()
        .joinToString("\n") { line ->
            line.trim().replace(Regex(" {2,}"), " ")
        }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

private fun persianTypeface(context: Context): Typeface =
    runCatching { Typeface.createFromAsset(context.assets, "fonts/Vazirmatn.ttf") }
        .getOrDefault(Typeface.DEFAULT)
