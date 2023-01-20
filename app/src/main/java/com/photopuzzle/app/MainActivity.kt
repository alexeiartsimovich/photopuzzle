package com.photopuzzle.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureMainFragment()
    }

    private fun ensureMainFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.container)
        if (fragment is MainFragment) {
            return
        }
        val newFragment = MainFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, newFragment)
            .commitNow()
    }
}