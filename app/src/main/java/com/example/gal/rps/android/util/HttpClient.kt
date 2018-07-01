package com.example.gal.rps.android.util

import android.annotation.SuppressLint
import android.os.AsyncTask
import org.json.JSONObject

enum class HttpClient(private val port: Int) {
	Lobby(8003), Game(8004);


	object Endpoints {
		const val LOBBY_PLAYERS = "/lobby/players"
		const val INVITE = "/lobby/invite"
		const val LOGOUT = "/logout"
		const val LOGIN = "/login"
		const val GET_PORT = "/game/get_port"
		const val READY = "/game/ready"
		const val MOVE = "/game/move"
		const val DRAW = "/game/draw"
		const val RANDOM = "/game/random"
		const val FLAG = "/game/flag"
		const val TRAP = "/game/trap"
		const val CHAT = "/lobby/chat"
	}

	@SuppressLint("StaticFieldLeak")
	fun sendRequest(endpoint: String, data: JSONObject, onPost: ((JSONObject) -> Unit)? = null) {
		object : AsyncTask<Void, Void, JSONObject>() {
			override fun doInBackground(vararg params: Void?): JSONObject {
				return try {
					HttpRequest("http://84.109.106.163:$port$endpoint")
							.prepare(HttpRequest.Method.POST)
//                .withHeaders("Content-Type=application/json")
							.withData(data.toString().toByteArray())
							.sendAndReadJSON()
				} catch (e: Exception) {
					JSONObject().put("error", e.message).put("success", false)
				}
			}

			override fun onPostExecute(result: JSONObject) {
				onPost?.invoke(result) //handle response
			}
		}.execute()
	}
}