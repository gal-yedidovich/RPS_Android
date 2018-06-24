package com.example.gal.rps.android.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.gal.rps.android.activities.LobbyActivity

class PlayersAdapter(private val activity: LobbyActivity, private val players: List<Player>) : RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
		val v = TextView(activity).apply {
			textSize = 20f
			setPadding(12, 12, 12, 12)
		}
		return PlayerViewHolder(v)
	}

	override fun getItemCount(): Int = players.size

	override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
		holder.playerTxt.apply {
			text = players[position].name
			setOnClickListener {
				activity.sendInvite(players[position].token)
			}
		}

	}

	class PlayerViewHolder(v: View) : RecyclerView.ViewHolder(v) {

		val playerTxt = v as TextView
	}

}

data class Player(val name: String, val token: Int)
