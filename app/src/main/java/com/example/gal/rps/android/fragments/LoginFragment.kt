package com.example.gal.rps.android.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gal.rps.android.activities.MainActivity
import com.example.gal.rps.android.util.*
import com.example.gal.rps_android.R
import kotlinx.android.synthetic.main.fragment_login.*
import org.json.JSONObject

class LoginFragment : Fragment() {
	private val context by lazy { activity as MainActivity }


	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_login, container, false)
	}

	override fun onStart() {
		super.onStart()
		loginBtn.setOnClickListener {

			val login = loginTxt.text.toString()
			//check name
			if (login.length < 2) {
				//show invalid name

				return@setOnClickListener
			}

			//perform login
			val data = JSONObject()
					.put("name", login)
					.put("req_type", "login")

			progress.show()
			HttpClient.lobby.sendRequest(HttpClient.KEYS.LOGIN, data, onPost = { response ->
				progress.gone()

				Shared.token = response["token"] as Int
				Shared.name = login

				context.goToFragment(ChatFragment())//go to next fragment
			})

		}
	}
}
