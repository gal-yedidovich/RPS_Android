package com.example.gal.rps.android.util

import org.json.JSONObject
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class SocketClient(private val port: Int) {
	Lobby(15001), Game(15002);

	val onDataReceived: ArrayList<((JSONObject) -> Unit)> = ArrayList()

	init {
		listen()
	}

	private fun listen() {
		Thread {
			val socket = Socket("84.109.106.163", port)
			socket.getOutputStream().write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(Shared.token).array())
			val input = socket.getInputStream()
			while (true) {
				var buffer = ByteArray(4)
				input.read(buffer)//receive data length
//
				val size = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).int //get size of data
				if (size == 0) break
				buffer = ByteArray(size)
				input.read(buffer) //receive actual data

				onDataReceived.forEach { it.invoke(JSONObject(String(buffer))) }
			}
		}.start()
	}

	operator fun ArrayList<((JSONObject) -> Unit)>.plusAssign(action: (JSONObject) -> Unit) {
		add(action)
	}

	operator fun ArrayList<((JSONObject) -> Unit)>.minusAssign(action: (JSONObject) -> Unit) {
		remove(action)
	}
}