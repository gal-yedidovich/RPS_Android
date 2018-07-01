package com.example.gal.rps.android.activities

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import com.example.gal.rps.android.activities.gameStates.GameOverState
import com.example.gal.rps.android.activities.gameStates.GameState
import com.example.gal.rps.android.activities.gameStates.SelectFlagState
import com.example.gal.rps.android.util.HttpClient
import com.example.gal.rps.android.util.Shared
import com.example.gal.rps.android.util.SocketClient
import com.example.gal.rps_android.R
import kotlinx.android.synthetic.main.activity_game.*
import org.json.JSONObject

class GameActivity : AppCompatActivity() {
	companion object {
		const val BOARD_SIZE = 7
	}

	internal val squares = Array<Array<Square?>>(BOARD_SIZE) { Array(BOARD_SIZE) { null } }
	val gameId by lazy {
		val gi = intent.getIntExtra("game_id", -1)
		if (gi == -1) throw Exception()
		return@lazy gi
	}

	var state: GameState = SelectFlagState(this)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_game)

		SocketClient.Game.onDataReceived += ::onDataReceived
		initBoard()
	}

	private fun initBoard() {

		//create board
		val board = object : GridLayout(this) { //grid with height that equals width
			override fun onMeasure(width: Int, heightSpec: Int) = super.onMeasure(width, width) //use only width
		}
		board.apply {
			layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)//height is irrelevant
			columnCount = BOARD_SIZE
			rowCount = BOARD_SIZE
			useDefaultMargins = true
		}

		//insert squares
		for (row in 0 until BOARD_SIZE) {
			for (col in 0 until BOARD_SIZE) {
				val squareImg = (layoutInflater.inflate(R.layout.image_item, board, false) as ImageView).apply {
					when (row) {
						0, 1 -> setImageDrawable(ColorDrawable(Color.BLUE))
						5, 6 -> setImageDrawable(ColorDrawable(Color.RED))
					}

					background = ColorDrawable(Color.GRAY)
				}

				//TODO - fix sizing, make board responsive
				/*
				val params = GridLayout.LayoutParams(
						GridLayout.spec(row, 1f),
						GridLayout.spec(col, 1f))
				board.addView(squareImg, params)
				// */
				board.addView(squareImg)
				// */

				squares[row][col] = Square(row, col, row >= 5, squareImg).apply {
					type = when (row) {
						0, 1 -> Square.Type.UnknownOpp
						else -> Square.Type.Empty
					}
				}
				squareImg.setOnClickListener { state.onSelect(squares[row][col]!!) }
			}
		}

		container.addView(board)
	}

	private fun onDataReceived(json: JSONObject) = state.onReceivedData(json)

	fun randomRPS(v: View) {
		doneBtn.isEnabled = true

		val json = JSONObject().put("token", Shared.token).put("gameId", gameId)
		val types = mapOf(
				Square.Type.Rock to R.mipmap.r_rock,
				Square.Type.Paper to R.mipmap.r_paper,
				Square.Type.Scissors to R.mipmap.r_scissors)

		HttpClient.Game.sendRequest(HttpClient.Endpoints.RANDOM, json) { response ->
			for (e in response.keys()) {
				val position = e.toIntOrNull() ?: continue

				val item = response.getJSONObject(e)
				val square = squares[position / 10][position % 10]!!
				val type = Square.Type.valueOf(item["type"].toString().capitalize())
				if (types.containsKey(type)) {
					square.type = type
					square.img.background = null
					square.img.setImageResource(types[type]!!)
				}
			}
		}
	}

	fun done(v: View) {
		state.done()
	}

	fun gameOver(won: Boolean) {
		//thread safe
		doneBtn.post {
			state = GameOverState(this)
			msgTxt.text = "You ${if (won) "Won!" else "Lost"}"
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		SocketClient.Game.onDataReceived -= ::onDataReceived
	}
}

data class Square(val row: Int, val col: Int, var myRPS: Boolean, val img: ImageView) {
	var type = Type.Empty

	enum class Type {
		Rock, Paper, Scissors, Flag, Trap, Empty, UnknownOpp;

		companion object {
			fun blue(type: Type): Int {
				return when (type) {
					Square.Type.Flag -> R.mipmap.b_flag
					Square.Type.Trap -> R.mipmap.b_trap
					Square.Type.Rock -> R.mipmap.b_rock
					Square.Type.Paper -> R.mipmap.b_paper
					Square.Type.Scissors -> R.mipmap.b_scissors
					else -> throw IllegalArgumentException("invalid type")
				}
			}
		}
	}
}