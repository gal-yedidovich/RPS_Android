package com.example.gal.rps.android.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.gal.rps_android.R

class ChatFragment : Fragment() {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_chat, container, false)
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)

	}

	override fun onStop() {
		super.onStop()
		//clean up resources

	}

}
