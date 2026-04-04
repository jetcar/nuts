package jetcar.nuts

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding

class MainActivity : AppCompatActivity() {
    private val engine = NutsGameEngine()

    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var statusView: TextView
    private lateinit var boardView: NutsBoardView
    private lateinit var actionButton: Button
    private lateinit var levelOneButton: Button
    private lateinit var levelTwoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#16100B"))
            setPadding(dp(16))
        }

        titleView = TextView(this).apply {
            setTextColor(Color.parseColor("#FFF3E7"))
            textSize = 24f
        }
        subtitleView = TextView(this).apply {
            setTextColor(Color.parseColor("#D4BFAE"))
            textSize = 15f
            setPadding(0, dp(4), 0, dp(8))
        }
        statusView = TextView(this).apply {
            setTextColor(Color.parseColor("#F8E4D2"))
            textSize = 15f
            setPadding(0, dp(4), 0, dp(12))
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val restartButton = buildButton("Restart") {
            updateUi(engine.restartLevel())
        }
        levelOneButton = buildButton("Level 1") {
            updateUi(engine.loadLevel(0))
        }
        levelTwoButton = buildButton("Level 2") {
            updateUi(engine.loadLevel(1))
        }
        actionButton = buildButton("Next") {
            updateUi(engine.advanceLevel())
        }

        controls.addView(restartButton, buttonParams())
        controls.addView(levelOneButton, buttonParams())
        controls.addView(levelTwoButton, buttonParams())
        controls.addView(actionButton, buttonParams())

        boardView = NutsBoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                topMargin = dp(12)
            }
            onAnchorTap = { anchorIndex ->
                updateUi(engine.handleAnchorTap(anchorIndex))
            }
        }

        val footerView = TextView(this).apply {
            setTextColor(Color.parseColor("#D4BFAE"))
            textSize = 13f
            gravity = Gravity.CENTER_HORIZONTAL
            text = "Tap a bolt, then tap any empty socket to move it. Clear every plank to win."
            setPadding(0, dp(12), 0, 0)
        }

        root.addView(titleView)
        root.addView(subtitleView)
        root.addView(statusView)
        root.addView(controls)
        root.addView(boardView)
        root.addView(footerView)

        setContentView(root)
        updateUi(engine.getLevel().instruction)
    }

    private fun updateUi(status: String) {
        titleView.text = engine.getLevel().name
        subtitleView.text = "Moves: ${engine.getMoveCount()} · Planks left: ${engine.getLevel().planks.size - engine.getRemovedPlanks().size}"
        statusView.text = status
        boardView.render(engine)

        levelOneButton.isEnabled = engine.getCurrentLevelNumber() != 1
        levelTwoButton.isEnabled = engine.getCurrentLevelNumber() != 2

        actionButton.visibility = if (engine.isLevelComplete()) View.VISIBLE else View.GONE
        actionButton.text = if (engine.getCurrentLevelNumber() < engine.getLevelCount()) "Next" else "Replay"
    }

    private fun buildButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        setOnClickListener { action() }
        setBackgroundColor(Color.parseColor("#574031"))
        setTextColor(Color.parseColor("#FFF8F2"))
        isAllCaps = false
        minHeight = dp(44)
    }

    private fun buttonParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f,
    ).apply {
        marginStart = dp(4)
        marginEnd = dp(4)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
