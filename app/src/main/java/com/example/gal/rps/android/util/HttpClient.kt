package com.example.gal.rps.android.util

import android.annotation.SuppressLint
import android.os.AsyncTask
import org.json.JSONObject

enum class HttpClient(private val port: Int) {
	lobby(8003), game(8004);


	object KEYS {
		val LOGIN = "login"
		val serverAddress = "84.109.106.163"
		val INVITE = "invite"
	}

	@SuppressLint("StaticFieldLeak")
	fun sendRequest(endpoint: String, data: JSONObject, onPost: (JSONObject) -> Unit = {}, onError: (JSONObject) -> Unit = { println(it) }) {
		object : AsyncTask<Void, Void, JSONObject>() {
			var commError = false
			override fun doInBackground(vararg params: Void?): JSONObject {
				return try {
					HttpRequest("http://${KEYS.serverAddress}:$port/$endpoint")
							.prepare(HttpRequest.Method.POST)
//                .withHeaders("Content-Type=application/json")
							.withData(data.toString().toByteArray())
							.sendAndReadJSON()
				} catch (e: Exception) {
					JSONObject().put("error", e.message)
				}
			}

			override fun onPostExecute(result: JSONObject) {
				if (commError or result.optBoolean("success")) onError(result) //handle error
				else onPost(result) //handle good response
			}
		}.execute()

	}
}