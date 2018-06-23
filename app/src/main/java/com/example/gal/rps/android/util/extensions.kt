package com.example.gal.rps.android.util

import android.view.View

fun View.hide() {
	this.visibility = View.INVISIBLE
}

fun View.show() {
	this.visibility = View.VISIBLE
}

fun View.gone() {
	this.visibility = View.GONE
}