package com.example.gal.rps.android.fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gal.rps.android.activities.MainActivity
import com.example.gal.rps.android.adapters.Player
import com.example.gal.rps.android.adapters.PlayersAdapter
import com.example.gal.rps_android.R

class PlayersFragment : Fragment() {
	val adapter by lazy { PlayersAdapter(activity as MainActivity) }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = inflater.inflate(R.layout.fragment_players, container, false)

		val list = view.findViewById<RecyclerView>(R.id.playersRcv)
		list.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
		list.adapter = adapter

		return view
	}

	fun addPlayer(player: Player) {
		adapter.addPlayer(player)
	}

	fun refreshList(playerList: ArrayList<Player>) {
		adapter.refreshList(playerList)
	}
}

