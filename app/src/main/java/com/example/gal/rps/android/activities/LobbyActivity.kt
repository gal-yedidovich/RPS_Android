package com.example.gal.rps.android.activities

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.ProgressBar
import com.example.gal.rps.android.adapters.Player
import com.example.gal.rps.android.util.HttpClient
import com.example.gal.rps.android.util.Shared
import com.example.gal.rps_android.R
import org.json.JSONObject

class LobbyActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_lobby)


	}

	fun sendInvite(player: Player) {
		val data = JSONObject()
				.put("sender_token", Shared.token)
				.put("target_token", player.token)
				.put("req_type", "invite")


		val dialog = AlertDialog.Builder(this).setView(ProgressBar(this)).setTitle("Inviting ${player.name} to game").setCancelable(false).create()
		dialog.show()
		HttpClient.lobby.sendRequest(HttpClient.KEYS.INVITE, data, onPost = {
			if (!it.optBoolean("success")) dialog.cancel()
		})
	}
}
