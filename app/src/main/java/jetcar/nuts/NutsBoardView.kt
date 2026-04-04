package jetcar.nuts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class NutsBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val boardRect = RectF()
    private var level: LevelDefinition = LevelCatalog.levels.first()
    private var bolts: Set<Int> = emptySet()
    private var removedPlanks: Set<String> = emptySet()
    private var selectedAnchor: Int? = null

    var onAnchorTap: ((Int) -> Unit)? = null

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B2A1F")
    }
    private val boardEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = Color.parseColor("#6D4F3A")
    }
    private val plankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(60, 255, 248, 220)
    }
    private val holeRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B664C")
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20150F")
    }
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8D3CF")
    }
    private val boltShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7E7A77")
    }
    private val boltSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#5B5855")
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FFD166")
    }

    fun render(engine: NutsGameEngine) {
        level = engine.getLevel()
        bolts = engine.getBolts()
        removedPlanks = engine.getRemovedPlanks()
        selectedAnchor = engine.getSelectedAnchor()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = dp(18f)
        boardRect.set(pad, pad, width - pad, height - pad)
        val corner = dp(28f)
        canvas.drawRoundRect(boardRect, corner, corner, boardPaint)
        canvas.drawRoundRect(boardRect, corner, corner, boardEdgePaint)

        val plankWidth = min(boardRect.width(), boardRect.height()) * 0.10f
        plankPaint.strokeWidth = plankWidth
        grainPaint.strokeWidth = plankWidth * 0.12f
        boltSlotPaint.strokeWidth = plankWidth * 0.08f
        selectedPaint.strokeWidth = plankWidth * 0.11f

        level.planks.filter { it.id !in removedPlanks }.forEach { plank ->
            val start = anchorPoint(plank.startAnchor)
            val end = anchorPoint(plank.endAnchor)
            plankPaint.color = plankColor(plank.tone)
            canvas.drawLine(start.first, start.second, end.first, end.second, plankPaint)

            val dx = end.first - start.first
            val dy = end.second - start.second
            val length = max(1f, hypot(dx, dy))
            val offsetX = -dy / length * plankWidth * 0.18f
            val offsetY = dx / length * plankWidth * 0.18f
            canvas.drawLine(
                start.first + offsetX,
                start.second + offsetY,
                end.first + offsetX,
                end.second + offsetY,
                grainPaint,
            )
            canvas.drawLine(
                start.first - offsetX,
                start.second - offsetY,
                end.first - offsetX,
                end.second - offsetY,
                grainPaint,
            )
        }

        level.anchors.indices.forEach { anchorIndex ->
            val point = anchorPoint(anchorIndex)
            val holeRadius = plankWidth * 0.21f
            canvas.drawCircle(point.first, point.second, holeRadius * 1.35f, holeRimPaint)
            canvas.drawCircle(point.first, point.second, holeRadius, holePaint)

            if (anchorIndex in bolts) {
                val boltRadius = holeRadius * 1.28f
                canvas.drawCircle(point.first, point.second + holeRadius * 0.10f, boltRadius, boltShadowPaint)
                canvas.drawCircle(point.first, point.second, boltRadius, boltPaint)
                canvas.drawLine(
                    point.first - boltRadius * 0.58f,
                    point.second,
                    point.first + boltRadius * 0.58f,
                    point.second,
                    boltSlotPaint,
                )
            }

            if (anchorIndex == selectedAnchor) {
                canvas.drawCircle(point.first, point.second, holeRadius * 1.9f, selectedPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            nearestAnchor(event.x, event.y)?.let {
                onAnchorTap?.invoke(it)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun nearestAnchor(x: Float, y: Float): Int? {
        val threshold = min(boardRect.width(), boardRect.height()) * 0.12f
        return level.anchors.indices
            .map { it to anchorPoint(it) }
            .map { (index, point) -> index to hypot(point.first - x, point.second - y) }
            .filter { (_, distance) -> distance <= threshold }
            .minByOrNull { it.second }
            ?.first
    }

    private fun anchorPoint(index: Int): Pair<Float, Float> {
        val anchor = level.anchors[index]
        return Pair(
            boardRect.left + anchor.x * boardRect.width(),
            boardRect.top + anchor.y * boardRect.height(),
        )
    }

    private fun plankColor(tone: WoodTone): Int = when (tone) {
        WoodTone.Oak -> Color.parseColor("#C58A4D")
        WoodTone.Cedar -> Color.parseColor("#B66E4E")
        WoodTone.Walnut -> Color.parseColor("#8E613F")
        WoodTone.Maple -> Color.parseColor("#D7A56B")
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
