package com.example.allersafe.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.allersafe.databinding.ActivityFirstpageBinding
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context

class FirstActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFirstpageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstpageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- TAMBAHKAN 3 BARIS INI ---
        val sharedPref = getSharedPreferences("AllerSafeSettings", Context.MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("THEME_MODE", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
        // -----------------------------

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}