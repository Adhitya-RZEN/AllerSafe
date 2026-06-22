package com.example.allersafe.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.allersafe.R
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.databinding.ActivityMainBinding
import com.example.allersafe.engine.BottomNavHelper
import com.example.allersafe.ui.fragment.HomeFragment
import com.example.allersafe.ui.fragment.ProfileFragment
import com.example.allersafe.ui.fragment.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepo = AuthRepository()

    // Tag untuk setiap Fragment agar bisa kita find & reuse
    private val TAG_HOME = "tag_home"
    private val TAG_PROFILE = "tag_profile"
    private val TAG_SETTINGS = "tag_settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!authRepo.isLoggedIn()) {
            showMandatoryAuthDialog()
            return
        }

        setupBottomNavigation()

        // Hanya tambahkan fragment pertama kali, jangan saat rotate/recreate
        if (savedInstanceState == null) {
            showFragment(TAG_HOME)
        }
    }

    private fun setupBottomNavigation() {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(
            getColor(R.color.accent_primary),
            getColor(R.color.text_secondary)
        )
        val colorStateList = android.content.res.ColorStateList(states, colors)
        binding.bottomNavigation.itemIconTintList = colorStateList
        binding.bottomNavigation.itemTextColor = colorStateList

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { showFragment(TAG_HOME);     true }
                R.id.nav_profile  -> { showFragment(TAG_PROFILE);  true }
                R.id.nav_settings -> { showFragment(TAG_SETTINGS); true }
                else -> false
            }
        }

        BottomNavHelper.loadProfileIcon(this, binding.bottomNavigation)
    }

    /**
     * Inti dari solusi: reuse Fragment yang sudah ada (hide/show),
     * atau buat baru jika belum pernah dibuat.
     * Dengan cara ini tidak ada destroy/recreate sama sekali.
     */
    private fun showFragment(tag: String) {
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()

        // Sembunyikan semua fragment yang sedang tampil
        fm.fragments.forEach { tx.hide(it) }

        // Cari fragment dengan tag ini, atau buat baru
        var target = fm.findFragmentByTag(tag)
        if (target == null) {
            target = when (tag) {
                TAG_HOME     -> HomeFragment()
                TAG_PROFILE  -> ProfileFragment()
                TAG_SETTINGS -> SettingsFragment()
                else -> return
            }
            tx.add(R.id.fragment_container, target, tag)
        } else {
            tx.show(target)
        }

        tx.commit()
    }

    // Navigasi ke item menu tertentu (misal dari Fragment)
    fun navigateTo(menuItemId: Int) {
        binding.bottomNavigation.selectedItemId = menuItemId
    }

    // Dipanggil dari HomeFragment saat perlu refresh icon profil
    fun refreshProfileIcon() {
        BottomNavHelper.loadProfileIcon(this, binding.bottomNavigation)
    }

    private fun showMandatoryAuthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_mandatory_auth, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<android.widget.Button>(R.id.btnDialogLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        dialogView.findViewById<android.widget.Button>(R.id.btnDialogDaftar).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        dialog.show()
    }
}