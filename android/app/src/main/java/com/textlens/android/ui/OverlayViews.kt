package com.textlens.android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import com.textlens.android.core.ScreenArea
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SelectionOverlayView(
    context: Context,
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
    private val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 247, 240, 220)
        textSize = 22f
        textAlign = Paint.Align.CENTER
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
            selectionRect.right - 132f,
            selectionRect.bottom + 18f,
            selectionRect.right,
            selectionRect.bottom + 72f,
        )
        cancelRect.set(
            selectionRect.left,
            selectionRect.bottom + 18f,
            selectionRect.left + 116f,
            selectionRect.bottom + 72f,
        )
        keepActionButtonsOnScreen()
        drawActionButton(canvas, confirmRect, "ترجمه")
        drawActionButton(canvas, cancelRect, "لغو")

        canvas.drawText(
            "باکس را جابه‌جا کن یا از گوشه پایین راست تغییر اندازه بده",
            width / 2f,
            (selectionRect.top - 24f).coerceAtLeast(44f),
            hintPaint,
        )
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
        val rectHeight = height * 0.28f
        val left = (width - rectWidth) / 2f
        val top = (height - rectHeight) / 2.8f
        selectionRect.set(left, top, left + rectWidth, top + rectHeight)
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
        if (selectionRect.top < 72f) selectionRect.offset(0f, 72f - selectionRect.top)
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
        val area = ScreenArea(
            left = selectionRect.left.toInt(),
            top = selectionRect.top.toInt(),
            width = selectionRect.width().toInt(),
            height = selectionRect.height().toInt(),
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
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(bounds, 34f, 34f, bgPaint)
        canvas.drawRoundRect(bounds.insetCopy(2f), 34f, 34f, borderPaint)

        canvas.drawText("TextLens", 32f, 54f, textPaint)
        canvas.drawText("ترجمه", width - 34f, 54f, textPaint.apply { textAlign = Paint.Align.RIGHT })
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
        canvas.drawCircle(width / 2f, height / 2f, 34f, borderPaint)
        canvas.drawCircle(width / 2f + 18f, height / 2f - 18f, 10f, goldPaint)
        canvas.drawText("در حال خواندن و ترجمه...", width - 40f, height / 2f + 74f, mutedPaint.apply { textAlign = Paint.Align.RIGHT })
        mutedPaint.textAlign = Paint.Align.LEFT
        drawButton(canvas, closeRect.setAndReturn(width - 156f, height - 66f, width - 30f, height - 20f), "لغو")
    }

    private fun drawResult(canvas: Canvas, result: State.Result) {
        val textArea = Rect(34, 108, width - 34, height - 104)
        val layout = StaticLayout.Builder.obtain(result.text, 0, result.text.length, textPaint.apply {
            textSize = 28f
            textAlign = Paint.Align.RIGHT
        }, textArea.width())
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setLineSpacing(7f, 1f)
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

        val cost = result.costToman?.let { "هزینه: $it تومان" } ?: "هزینه: نامشخص"
        canvas.drawText(cost, 38f, height - 38f, mutedPaint)
        drawButton(canvas, copyRect.setAndReturn(width - 292f, height - 72f, width - 164f, height - 22f), "کپی")
        drawButton(canvas, closeRect.setAndReturn(width - 148f, height - 72f, width - 28f, height - 22f), "بستن")
    }

    private fun drawError(canvas: Canvas, message: String) {
        textPaint.color = Color.rgb(255, 95, 95)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("!", width / 2f, 142f, textPaint.apply { textSize = 48f })
        val layout = StaticLayout.Builder.obtain(message, 0, message.length, textPaint.apply { textSize = 26f }, width - 80)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(6f, 1f)
            .build()
        canvas.save()
        canvas.translate(40f, 174f)
        layout.draw(canvas)
        canvas.restore()
        textPaint.color = Color.rgb(247, 240, 220)
        textPaint.textAlign = Paint.Align.LEFT
        drawButton(canvas, modelRect.setAndReturn(width - 420f, height - 72f, width - 300f, height - 22f), "Model")
        drawButton(canvas, retryRect.setAndReturn(width - 292f, height - 72f, width - 164f, height - 22f), "Retry")
        drawButton(canvas, closeRect.setAndReturn(width - 148f, height - 72f, width - 28f, height - 22f), "بستن")
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

private fun persianTypeface(context: Context): Typeface =
    runCatching { Typeface.createFromAsset(context.assets, "fonts/Vazirmatn.ttf") }
        .getOrDefault(Typeface.DEFAULT)
