package com.photopuzzle.app.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.photopuzzle.app.R
import com.photopuzzle.app.ui.imagesource.ChooseImageSourceFragment
import com.photopuzzle.app.ui.puzzle.ImagePuzzleFragment

class MainActivity : AppCompatActivity(),
    ChooseImageSourceFragment.OnImageSourceChosenCallback,
    ImagePuzzleFragment.OnNewGameClickedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureFragment()
    }

    private fun ensureFragment() {
        val fragment = getCurrentFragment()
        if (fragment is ChooseImageSourceFragment || fragment is ImagePuzzleFragment) {
            return
        }
        val newFragment = ChooseImageSourceFragment()
        setCurrentFragment(newFragment)
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.container)
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow()
    }

    override fun onImageSourceChosen(imageUri: Uri, filepath: String?) {
        val fragment = ImagePuzzleFragment.newInstance(imageUri, filepath)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow()
    }

    override fun onNewGameClicked() {
        setCurrentFragment(ChooseImageSourceFragment())
    }

    override fun onBackPressed() {
        if (!goBack()) {
            super.onBackPressed()
        }
    }

    private fun goBack(): Boolean {
        val fragment = getCurrentFragment()
        if (fragment is ImagePuzzleFragment) {
            setCurrentFragment(ChooseImageSourceFragment())
            return true
        }
        return false
    }
}