package com.example.gal.rps.android.alerts

import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import com.example.gal.rps.android.activities.Square
import com.example.gal.rps.android.util.*
import com.example.gal.rps_android.R
import org.json.JSONObject

class DrawAlert(private val attacker: Square, val target: Square, val gameId: Int, private val onComplete: (Int) -> Unit) {
	private var dialog: AlertDialog
	private var state: DrawState = ChoosingState(this)
	private val progress: ProgressBar
	private val msgTxt: TextView
	private val opponent: Array<Square>

	init {
		val context = attacker.img.context
		val view = LayoutInflater.from(context).inflate(R.layout.draw_alert, null)

		//register to network event
		SocketClient.Game.onDataReceived += this::onReceive

		//create alert
		dialog = AlertDialog.Builder(context)
				.setView(view)
				.setCancelable(false)
				.setOnDismissListener { SocketClient.Game.onDataReceived -= this::onReceive } //cleanup on dismiss
				.create()

		val squares = arrayOf(Square(0, 0, true, view.findViewById(R.id.rock)).apply { type = Square.Type.Rock },
				Square(0, 1, true, view.findViewById(R.id.paper)).apply { type = Square.Type.Paper },
				Square(0, 2, true, view.findViewById(R.id.scissors)).apply { type = Square.Type.Scissors }
		)
		progress = view.findViewById(R.id.progress)
		msgTxt = view.findViewById(R.id.msgTxt)

		opponent = arrayOf(Square(0, 0, true, view.findViewById(R.id.oppRock)).apply { type = Square.Type.Rock },
				Square(0, 1, true, view.findViewById(R.id.oppPaper)).apply { type = Square.Type.Paper },
				Square(0, 2, true, view.findViewById(R.id.oppScissors)).apply { type = Square.Type.Scissors }
		)

		//set click events handlers
		squares.forEach { square -> square.img.setOnClickListener { state.onSelecting(square) } }

		attacker.img.post {
			//present dialog
			dialog.show()
			view.findViewById<TextView>(R.id.myNameTxt).text = Shared.name
		}
	}

	private fun onReceive(json: JSONObject) = state.onOppSelected(json)

	private fun resolve(selected: Square, opponentSelection: String, result: Int) {
		val type = Square.Type.valueOf(opponentSelection.toLowerCase().capitalize()) //make sure string is valid for enum
		val index = type.ordinal

		Thread {
			progress.post {
				//hide other squares
				opponent[index + 1 % 3].img.hide()
				opponent[index + 2 % 3].img.hide()

				opponent[index].img.setImageResource(Square.Type.blue(type))
			}

			Thread.sleep(500) //for UX

			progress.post {
				//update original square at game board
				if (attacker.myRPS) updateSquares(selected, opponent[index])
				else updateSquares(opponent[index], selected)

				dialog.dismiss() //close
			}
			onComplete(result) //callback
		}.start()
	}

	private fun updateSquares(attacker: Square, target: Square) {
		this.attacker.img.setImageDrawable(attacker.img.drawable)
		this.attacker.type = attacker.type

		this.target.img.setImageDrawable(target.img.drawable)
		this.target.type = target.type
	}

	//region states
	private interface DrawState {
		fun onSelecting(selected: Square)
		fun onOppSelected(json: JSONObject)
	}

	private class ChoosingState(private val context: DrawAlert) : DrawState {

		override fun onOppSelected(json: JSONObject) {
			context.progress.post { context.msgTxt.text = json.getString("msg") }
		}

		override fun onSelecting(selected: Square) {
			selected.img.setBackgroundColor(green)

			context.progress.show()
			val json = JSONObject()
					.put("token", Shared.token)
					.put("decision", selected.type.toString().toLowerCase())
					.put("gameId", context.gameId)

			//send selection to server
			HttpClient.Game.sendRequest(HttpClient.Endpoints.DRAW, json) { response ->
				if (response.has("opponent")) { //opponent has attacker
					val opponentSelection = response.getString("opponent")
					context.progress.hide()
					val result = response.getInt("result")
					context.resolve(selected, opponentSelection, result)
				} else context.state = WaitingState(context, selected)
			}
		}
	}

	private class WaitingState(private val context: DrawAlert, private val selection: Square) : DrawState {
		override fun onSelecting(selected: Square) {} //do nothing

		override fun onOppSelected(json: JSONObject) {
			val selected = json["opponent"] as String
			val result = json["result"] as Int

			context.progress.post { context.progress.hide() }

			context.resolve(selection, selected, result)
		}

	}
	//endregion
}