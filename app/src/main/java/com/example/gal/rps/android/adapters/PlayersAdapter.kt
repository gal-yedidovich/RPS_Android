package com.example.gal.rps.android.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.gal.rps.android.activities.MainActivity
import com.example.gal.rps_android.R

class PlayersAdapter(private val activity: MainActivity, private val players: ArrayList<Player> = ArrayList()) : RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
//		val v = TextView(activity).apply {
//			textSize = 20f
//			setPadding(12, 12, 12, 12)
//		}
		val v = LayoutInflater.from(activity).inflate(R.layout.player_item, parent, false)
		return PlayerViewHolder(v)
	}

	override fun getItemCount(): Int = players.size

	override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
		holder.playerTxt.apply {
			text = players[position].name
			setOnClickListener {
				activity.sendInvite(players[position])
			}
		}
	}

	class PlayerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		val playerTxt = v as TextView
	}

	fun refreshList(players: List<Player>) {
		this.players.clear()
		this.players.addAll(players)
		notifyDataSetChanged()
	}

	fun addPlayer(player: Player) {
		this.players.add(player)
		notifyItemInserted(players.size - 1)
	}
}

data class Player(val name: String, val token: Int)
