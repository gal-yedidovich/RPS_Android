package com.example.gal.rps.android.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.example.gal.rps.android.fragments.ChatFragment
import com.example.gal.rps.android.fragments.PlayersFragment

class PageAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

	private val frags = arrayOf("Player List" to PlayersFragment(), "Chat" to ChatFragment())

	override fun getCount(): Int = 2
	override fun getItem(position: Int): Fragment = frags[position].second
	override fun getPageTitle(position: Int): CharSequence = frags[position].first
}
