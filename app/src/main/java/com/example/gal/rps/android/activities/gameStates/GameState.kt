package com.example.gal.rps.android.activities.gameStates

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.example.gal.rps.android.activities.GameActivity
import com.example.gal.rps.android.activities.Square
import com.example.gal.rps.android.alerts.DrawAlert
import com.example.gal.rps.android.util.HttpClient
import com.example.gal.rps.android.util.Shared
import com.example.gal.rps_android.R
import kotlinx.android.synthetic.main.activity_game.*
import org.json.JSONObject

interface GameState {
	fun onSelect(square: Square)
	fun onReceivedData(json: JSONObject)
	fun done()
}

class SelectFlagState(private val context: GameActivity) : GameState {
	private var oppReady = false

	override fun onSelect(square: Square) {
		square.apply {
			if (myRPS) {
				val json = JSONObject()
						.put("token", Shared.token)
						.put("gameId", context.gameId)
						.put("row", row)
						.put("col", col)

				HttpClient.Game.sendRequest(HttpClient.Endpoints.FLAG, json) { response ->
					if (response.optBoolean("success")) {
						img.setImageResource(R.mipmap.r_flag)
						img.background = null
						context.state = SelectTrapState(context, oppReady)
						type = Square.Type.Flag
					}
				}
			}
		}
	}

	override fun onReceivedData(json: JSONObject) {
		if (json["type"] == "opponent ready") oppReady = true
		context.msgTxt.post { context.msgTxt.text = context.getString(R.string.oppReady) }
	}

	override fun done() {}
}

class SelectTrapState(private val context: GameActivity, private var oppReady: Boolean) : GameState {
	override fun onSelect(square: Square) {
		square.apply {
			if (myRPS && type != Square.Type.Flag) {
				val json = JSONObject()
						.put("token", Shared.token)
						.put("gameId", context.gameId)
						.put("row", row)
						.put("col", col)

				HttpClient.Game.sendRequest(HttpClient.Endpoints.TRAP, json) { response ->
					if (response.optBoolean("success")) {
						img.setImageResource(R.mipmap.r_trap)
						img.background = null
						context.state = RandomRPSState(context, oppReady)
						type = Square.Type.Trap
					}
				}
			}
		}
	}

	override fun onReceivedData(json: JSONObject) {
		if (json["type"] == "opponent ready") oppReady = true
		context.msgTxt.post { context.msgTxt.text = context.getString(R.string.oppReady) }
	}

	override fun done() {}
}

class RandomRPSState(private val context: GameActivity, private var oppReady: Boolean) : GameState {

	init {
		context.randomBtn.isEnabled = true
	}

	override fun onSelect(square: Square) {
		//do nothing
	}

	override fun onReceivedData(json: JSONObject) {
		if (json["type"] == "opponent ready") oppReady = true
		context.msgTxt.post { context.msgTxt.text = context.getString(R.string.oppReady) }
	}

	override fun done() {
		context.randomBtn.isEnabled = false
		context.doneBtn.isEnabled = false
//		//send ready
		val json = JSONObject()
				.put("token", Shared.token)
				.put("gameId", context.gameId)

		HttpClient.Game.sendRequest(HttpClient.Endpoints.READY, json) { response ->
			if (!response.optBoolean("success")) throw Exception("ready request failed")

//			if (oppReady){
//				if(response.getInt("turn") == Shared.token) context.state = MyTurnState(context)
//				else context.state = WaitingState(context)
//			}
//			else context.state = ReadyState(context)
			val myTurn = response.getInt("turn") == Shared.token
			context.state = when {
				oppReady && myTurn -> MyTurnState(context)
				oppReady -> WaitingState(context)
				else -> ReadyState(context, myTurn)
			}
		}
	}
}

class ReadyState(private val context: GameActivity, private val myTurn: Boolean) : GameState {
	override fun onSelect(square: Square) {}

	override fun onReceivedData(json: JSONObject) {
		if (json["type"] == "opponent ready") context.state = if (myTurn) MyTurnState(context) else WaitingState(context)
	}

	override fun done() {}

}

class WaitingState(private val context: GameActivity) : GameState {
	init {
		context.msgTxt.post {
			context.msgTxt.text = context.getString(R.string.oppTurn)
		}
	}

	override fun onSelect(square: Square) {}

	override fun onReceivedData(json: JSONObject) {
		if (json["type"] == "move") {//opponent is moving
			//rotate position
			val size = GameActivity.BOARD_SIZE
			val from = size - 1 - json.getJSONObject("from").getInt("row") to size - 1 - json.getJSONObject("from").getInt("col")
			val to = size - 1 - json.getJSONObject("to").getInt("row") to size - 1 - json.getJSONObject("to").getInt("col")
			//get squares from rotated position
			val fromSquare = context.squares[from.first][from.second]!!//get selected square
			val toSquare = context.squares[to.first][to.second]!!//get target square

			if (json.has("battle")) {
				if (json.has("square_type"))//reveal unknown opponent
				{
					revealOpponent(json.getString("square_type"), fromSquare)
					Thread.sleep(400)
				}

				val result = json["battle"] as Int
				context.doneBtn.post { Moves.battle(result, attacker = fromSquare, target = toSquare, gameId = context.gameId) }
			} else context.doneBtn.post { Moves.moveTo(fromSquare, toSquare) }

			if (json.has("winner")) context.gameOver(false)
			else context.state = MyTurnState(context)
		}
	}

	private fun revealOpponent(typeStr: String, square: Square) {
		val type = Square.Type.valueOf(typeStr.toLowerCase().capitalize())

		square.type = type
		val resource = when (type) {
			Square.Type.Rock -> R.mipmap.b_rock
			Square.Type.Paper -> R.mipmap.b_paper
			Square.Type.Scissors -> R.mipmap.b_scissors
			else -> throw Exception("invalid type")
		}
		context.doneBtn.post {
			square.img.background = null
			square.img.setImageResource(resource)
		}
	}

	override fun done() {}
}

class MyTurnState(private val context: GameActivity) : GameState {
	init {
		context.msgTxt.post {
			context.msgTxt.text = context.getString(R.string.my_turn)
		}
	}

	override fun onSelect(square: Square) {
		if (square.myRPS && square.type != Square.Type.Trap && square.type != Square.Type.Flag) { //valid selection - find possible moves
			val possibleMoves = ArrayList<Square>()
			val squares = context.squares //shorter
			val (row, col) = square.row to square.col //shorter

			//check move is valid & can go there (not my soldier)
			//up
			if (row - 1 >= 0 && !squares[row - 1][col]!!.myRPS) possibleMoves.add(squares[row - 1][col]!!)
			//left
			if (col - 1 >= 0 && !squares[row][col - 1]!!.myRPS) possibleMoves.add(squares[row][col - 1]!!)
			//down
			if (row + 1 < GameActivity.BOARD_SIZE && !squares[row + 1][col]!!.myRPS) possibleMoves.add(squares[row + 1][col]!!)
			//right
			if (col + 1 < GameActivity.BOARD_SIZE && !squares[row][col + 1]!!.myRPS) possibleMoves.add(squares[row][col + 1]!!)

			//highlight moves

			for (move in possibleMoves) move.img.setBackgroundColor(Color.parseColor("#ff008800"))
//
			//go to selected state
			context.state = SelectedSquareState(context, square, possibleMoves)
		}
	}

	override fun onReceivedData(json: JSONObject) {}

	override fun done() {}
}

class SelectedSquareState(private val context: GameActivity, private val selected: Square, private val possibleMoves: ArrayList<Square>) : GameState {

	override fun onSelect(square: Square) {
		for (move in possibleMoves) move.img.setBackgroundColor(Color.GRAY)

		if (possibleMoves.contains(square)) sendMove(square)
		else {
			context.state = MyTurnState(context)
			context.state.onSelect(square) //change state & execute clicking (selecting) on new square (this square is now selected)
		}
	}

	private fun sendMove(target: Square) {
		val json = JSONObject()
				.put("token", Shared.token)
				.put("gameId", context.gameId)
				.put("type", "move")
				.put("from", JSONObject()
						.put("row", selected.row)
						.put("col", selected.col))
				.put("to", JSONObject()
						.put("row", target.row)
						.put("col", target.col))

		if (target.type != Square.Type.Empty && !target.myRPS) json.put("square_type", selected.type.toString())

		HttpClient.Game.sendRequest(HttpClient.Endpoints.MOVE, json) { response ->
			if (!response.optBoolean("success")) throw Exception("error - $response")

			if (response.has("winner")) context.gameOver(true)
			else context.state = WaitingState(context)

			Thread {
				if (response.has("s_type")) { //reveal unknown opponent
					val square = context.squares[target.row][target.col]!!
					square.type = Square.Type.valueOf(response["s_type"].toString().capitalize())

					square.img.post {
						square.img.setImageResource(Square.Type.blue(square.type))
//						square.img.background = null
					}
					Thread.sleep(400) //wait for UX - let user see opponent rps
				}

				if (response.has("battle")) { //do battle
					val result = response.getInt("battle") //get battle result
					context.doneBtn.post { Moves.battle(result, selected, target, context.gameId) }
				} else context.doneBtn.post { Moves.moveTo(selected, target) }

			}.start()

		}
	}

	override fun onReceivedData(json: JSONObject) {}

	override fun done() {}

}

class GameOverState(private val context: GameActivity) : GameState {
	override fun onSelect(square: Square) {
	}

	override fun onReceivedData(json: JSONObject) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun done() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

}

//static reusable functions
object Moves {
	internal fun battle(result: Int, attacker: Square, target: Square, gameId: Int) {
		when (result) {
			-1 -> kill(attacker) //attack lost
			1 -> moveTo(attacker, target) //attack won
			0 -> resolveDraw(attacker, target, gameId) //draw
			else -> throw Exception("unknown result") //other
		}
	}

	internal fun moveTo(from: Square, to: Square) { //not thread safe - must run in UI main thread

		//move image to target
		to.img.background = null
		to.img.setImageDrawable(from.img.drawable)

		//remove image from old position
		from.img.background = ColorDrawable(Color.GRAY)
		from.img.setImageDrawable(null)

		to.myRPS = from.myRPS //pass ownership
		to.type = from.type//pass type

		from.type = Square.Type.Empty //remove old type
		from.myRPS = false//in case old square was mine, remove ownership
	}

	private fun resolveDraw(selected: Square, target: Square, gameId: Int) {
		val draw = DrawAlert(selected, target, gameId) { result ->
			Thread.sleep(500)

			battle(result, selected, target, gameId)
		}
	}

	private fun kill(square: Square) {
		square.img.setImageDrawable(null)
		square.img.setBackgroundColor(Color.GRAY)
		square.type = Square.Type.Empty
	}
}