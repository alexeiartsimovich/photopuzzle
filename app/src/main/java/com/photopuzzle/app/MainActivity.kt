package com.photopuzzle.app

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity(), ChooseImageSourceFragment.OnImageSourceChosenCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureFragment()
    }

    private fun ensureFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.container)
        if (fragment is ChooseImageSourceFragment || fragment is ImagePuzzleFragment) {
            return
        }
        val newFragment = ChooseImageSourceFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, newFragment)
            .commitNow()
    }

    override fun onImageSourceChosen(imageUri: Uri) {
        val fragment = ImagePuzzleFragment.newInstance(imageUri)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow()
    }
}