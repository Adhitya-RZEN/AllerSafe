package com.example.allersafe.ui.fragment

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.data.repository.HistoryRepository
import com.example.allersafe.databinding.FragmentSettingsBinding
import com.example.allersafe.ui.FirstActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val authRepo = AuthRepository()
    private val historyRepo = HistoryRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeToggle()
        setupActionButtons()
    }

    private fun setupThemeToggle() {
        val sharedPref = requireContext().getSharedPreferences("AllerSafeSettings", Context.MODE_PRIVATE)
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
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Riwayat?")
                .setMessage("Semua riwayat pemindaian alergen akan dihapus permanen.")
                .setPositiveButton("Hapus") { _, _ -> clearUserHistory() }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Akun Permanen?")
                .setMessage("Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Ya, Hapus") { _, _ -> deleteUserAccount() }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnHelpCenter.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Pusat Bantuan...", Toast.LENGTH_SHORT).show()
        }
        binding.btnFeedback.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Form Masukan...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearUserHistory() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        Toast.makeText(requireContext(), "Menghapus riwayat...", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = historyRepo.clearScanHistory(uid)
            val msg = if (result.isSuccess) "Semua riwayat berhasil dihapus" else "Gagal menghapus riwayat."
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteUserAccount() {
        authRepo.getCurrentUser()?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                authRepo.logout()
                Toast.makeText(requireContext(), "Akun berhasil dihapus", Toast.LENGTH_LONG).show()
                val intent = Intent(requireContext(), FirstActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Gagal. Silakan login ulang terlebih dahulu.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}