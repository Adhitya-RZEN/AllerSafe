package com.example.allersafe.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.allersafe.R
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.data.repository.HistoryRepository
import com.example.allersafe.databinding.ActivitySettingsBinding
import com.example.allersafe.engine.BottomNavHelper
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepo = AuthRepository()
    private val historyRepo = HistoryRepository() // Inisialisasi HistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupThemeToggle()
        setupActionButtons()
    }

    private fun setupThemeToggle() {
        val sharedPref = getSharedPreferences("AllerSafeSettings", Context.MODE_PRIVATE)

        val savedTheme = sharedPref.getInt("THEME_MODE", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val isNightMode = if (savedTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } else {
            savedTheme == AppCompatDelegate.MODE_NIGHT_YES
        }

        binding.switchTheme.setOnCheckedChangeListener(null)
        binding.switchTheme.isChecked = isNightMode

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            sharedPref.edit().putInt("THEME_MODE", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.btnThemeRow.setOnClickListener {
            binding.switchTheme.isChecked = !binding.switchTheme.isChecked
        }
    }

    private fun setupActionButtons() {
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Riwayat?")
                .setMessage("Semua riwayat pemindaian alergen akan dihapus permanen dari akun ini.")
                .setPositiveButton("Hapus") { _, _ ->
                    // --- PANGGIL FUNGSI LOGIKA HAPUS KE DATABASE ---
                    clearUserHistory()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Akun Permanen?")
                .setMessage("Tindakan ini tidak dapat dibatalkan. Semua data profil dan riwayat akan hilang selamanya.")
                .setPositiveButton("Ya, Hapus") { _, _ ->
                    deleteUserAccount()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnHelpCenter.setOnClickListener {
            Toast.makeText(this, "Membuka Pusat Bantuan...", Toast.LENGTH_SHORT).show()
        }
        binding.btnFeedback.setOnClickListener {
            Toast.makeText(this, "Membuka Form Masukan...", Toast.LENGTH_SHORT).show()
        }
    }

    // --- FUNGSI BARU UNTUK MENGHAPUS RIWAYAT DARI DATABASE ---
    private fun clearUserHistory() {
        val uid = authRepo.getCurrentUser()?.uid
        if (uid != null) {
            Toast.makeText(this, "Menghapus riwayat...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                val result = historyRepo.clearScanHistory(uid)
                if (result.isSuccess) {
                    Toast.makeText(this@SettingsActivity, "Semua riwayat berhasil dihapus", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Gagal menghapus riwayat. Cek koneksi Anda.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteUserAccount() {
        val user = authRepo.getCurrentUser()
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                authRepo.logout()
                Toast.makeText(this, "Akun berhasil dihapus", Toast.LENGTH_LONG).show()
                val intent = Intent(this, FirstActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                Toast.makeText(this, "Gagal menghapus akun. Silakan login ulang terlebih dahulu.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.bottomNavigation
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
        val colors = intArrayOf(getColor(R.color.accent_primary), getColor(R.color.text_secondary))
        val colorStateList = android.content.res.ColorStateList(states, colors)

        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList
        bottomNav.selectedItemId = R.id.nav_settings

        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNavHelper.loadProfileIcon(this, binding.bottomNavigation)

        // PENTING: Harus nav_settings
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
    }

}