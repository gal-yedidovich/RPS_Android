package com.example.gal.rps.android.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.example.gal.rps.android.adapters.PageAdapter
import com.example.gal.rps.android.adapters.Player
import com.example.gal.rps.android.fragments.PlayersFragment
import com.example.gal.rps.android.util.HttpClient
import com.example.gal.rps.android.util.Shared
import com.example.gal.rps.android.util.SocketClient
import com.example.gal.rps_android.R
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

	private val loadingAlert: AlertDialog by lazy { createLoadingDialog() }
	private val pagerAdapter by lazy { PageAdapter(supportFragmentManager) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabReselected(tab: TabLayout.Tab?) {
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}

			override fun onTabSelected(tab: TabLayout.Tab) {
				pager.currentItem = tab.position
			}
		})
		pager.adapter = pagerAdapter
		tabLayout.setupWithViewPager(pager)
	}

	override fun onStart() {
		super.onStart()
		//getSharedPreferences("user", Context.MODE_PRIVATE).edit().clear().apply()
		if (Shared.token == -1) loadPrefs()
	}

	private fun loadPrefs() {
		val prefs = getSharedPreferences("user", Context.MODE_PRIVATE)

		if (prefs.contains("name")) {
			Shared.name = prefs.getString("name", "")
			login()
		} else {
			val txt = EditText(this).apply { hint = "Enter your name" }
			AlertDialog.Builder(this)
					.setTitle("login")
					.setView(txt)
					.setCancelable(false)
					.setPositiveButton("Log in") { _: DialogInterface, _: Int ->
						val name = txt.text.toString()
						if (name.length > 2) {
							Shared.name = name
							prefs.edit().putString("name", name).apply()
							login()
						} else loadPrefs()
					}.show()
		}
	}

	private fun login() {
		val loadingDialog = createLoadingDialog()
		loadingDialog.show()

		HttpClient.Lobby.sendRequest(HttpClient.Endpoints.LOGIN, JSONObject().put("name", Shared.name)) { response ->
			loadingDialog.dismiss()
			if (response.optBoolean("success")) {
				Shared.token = response.getInt("token")
				getSharedPreferences("user", Context.MODE_PRIVATE).edit()
						.putInt("token", Shared.token)
						.apply()
				SocketClient.Lobby.onDataReceived += ::onDataReceived

				loadLobbyUsers()
				loadLobbyChat()
			} else {
				AlertDialog.Builder(this@MainActivity)
						.setTitle("Error")
						.setMessage("could not sign in")
						.setPositiveButton("Try again") { _: DialogInterface, _: Int -> loadPrefs() }
						.setNegativeButton("Exit") { _: DialogInterface, _: Int -> finish() }
						.show()
			}
		}
	}

	private fun createLoadingDialog(): AlertDialog {
		return AlertDialog.Builder(this)
				.setCancelable(false)
				.setView(ProgressBar(this))
				.setTitle("loading")
				.create()
	}

	private fun loadLobbyChat() {

	}

	private fun loadLobbyUsers() {
		HttpClient.Lobby.sendRequest(HttpClient.Endpoints.LOBBY_PLAYERS, JSONObject().put("token", Shared.token)) { response ->
			val jsonList = response.getJSONArray("player_list")
			val playerList = ArrayList<Player>()
			for (i in 0 until jsonList.length()) {
				val p = jsonList.getJSONObject(i)
				playerList.add(Player(p.getString("name"), p.getInt("token")))
			}

			(pagerAdapter.getItem(0) as PlayersFragment).refreshList(playerList)
		}
	}

	fun sendInvite(player: Player) {
		val inviteJson = JSONObject()
				.put("sender_token", Shared.token)
				.put("target_token", player.token)
				.put("req_type", "invite")
				.put("name", Shared.name)

		HttpClient.Lobby.sendRequest(HttpClient.Endpoints.INVITE, inviteJson) { response ->
			if (response.optBoolean("success")) { //invite sent
				//show progress bar
				loadingAlert.apply {
					setTitle("inviting ${player.name} to game")
					show()
				}
			} else {
				Toast.makeText(this@MainActivity, "error", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun onDataReceived(json: JSONObject) {
		when (json["type"].toString()) {
			"new_user" -> {
				val name = json.getString("name")
				val token = json.getInt("token")

				pager.post {
					(pagerAdapter.getItem(0) as PlayersFragment).addPlayer(Player(name, token))
				}
			}
			"invite" -> {
				pager.post {
					val sender = json["sender_name"].toString()
					AlertDialog.Builder(this)
							.setTitle("Invite")
							.setMessage("You are invited to play with $sender\nDo you want to play?")
							.setCancelable(false)
							.setPositiveButton("Yes") { _: DialogInterface, _: Int -> handleInvite(true, json) }
							.setNegativeButton("No") { _: DialogInterface, _: Int -> handleInvite(false, json) }
							.show()
				}
			}
			"answer" -> {
				pager.post {
					loadingAlert.dismiss()
					if (json.optBoolean("accept")) goToGame(json.getInt("game_id"))
					else AlertDialog.Builder(this).setMessage("${json["name"]} refused to play.").show()
				}
			}
			"msg" -> {
//			string msg = $"{json["sender"]}: {json["content"]}";
//			Dispatcher.Invoke(() => HandleNewMessage(msg));
			}
		}
	}

	private fun handleInvite(accept: Boolean, json: JSONObject) {
		json.put("accept", accept).put("type", "answer").put("target_token", Shared.token)
		HttpClient.Lobby.sendRequest(HttpClient.Endpoints.INVITE, json)

		if (accept) goToGame(json.getInt("game_id"))
	}

	private fun goToGame(gameId: Int) {
		val i = Intent(this, GameActivity::class.java)
		i.putExtra("game_id", gameId)
		startActivity(i)
	}

	override fun onDestroy() {
		super.onDestroy()
		HttpClient.Lobby.sendRequest(HttpClient.Endpoints.LOGOUT, JSONObject().put("token", Shared.token)) //logout
		Shared.token = -1 //TODO - check if not breaking logic
	}
}
