package com.example.allersafe.ui

import android.app.Activity
import androidx.core.content.ContextCompat
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.allersafe.R
import com.example.allersafe.adapter.HistoryAdapter
import com.example.allersafe.adapter.SearchProductAdapter
import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.model.AllergenType
import com.example.allersafe.data.model.ProductDBModel
import com.example.allersafe.data.model.ScanResult
import com.example.allersafe.data.repository.AuthRepository
import com.example.allersafe.data.repository.HistoryRepository
import com.example.allersafe.data.repository.ScanRepository
import com.example.allersafe.databinding.ActivityMainBinding
import com.example.allersafe.engine.AllergenEngine
import com.example.allersafe.engine.BottomNavHelper
import com.example.allersafe.engine.SynonymMap
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var historyAdapter: HistoryAdapter
    private val authRepo = AuthRepository()
    private val historyRepo = HistoryRepository()
    private val scanRepo = ScanRepository()

    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchBar()
        setupBottomNavigation()

        if (!authRepo.isLoggedIn()) {
            showMandatoryAuthDialog()
        } else {
            loadHistoryData()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.bottomNavigation
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))

        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.accent_primary),
            ContextCompat.getColor(this, R.color.text_secondary)
        )
        val colorStateList = android.content.res.ColorStateList(states, colors)

        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList

        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    if (Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    if (Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
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
        if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun showMandatoryAuthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_mandatory_auth, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btnDialogLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        dialogView.findViewById<Button>(R.id.btnDialogDaftar).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        dialog.show()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList()) { clickedResult ->
            showIngredientDetailDialog(clickedResult)
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun loadHistoryData() {
        lifecycleScope.launch {
            val uid = authRepo.getCurrentUser()?.uid ?: return@launch
            val result = historyRepo.getScanHistory(uid)
            if (result.isSuccess) historyAdapter.updateData(result.getOrDefault(emptyList()))
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                // NORMALISASI: Pencarian selalu diubah ke lowercase
                val inputProduk = binding.etSearch.text.toString().trim().lowercase()
                if (inputProduk.isNotEmpty()) {
                    prosesDeteksiProduk(inputProduk)
                }
                true
            } else false
        }
    }

    private fun showEmptyAllergenDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_empty_allergen, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btnGoToProfile).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        dialogView.findViewById<Button>(R.id.btnCancelSearch).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showProductSelectionDialog(products: List<ProductDBModel>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val rvResults = dialogView.findViewById<RecyclerView>(R.id.rvSearchResults)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val adapter = SearchProductAdapter(products) { selectedProduct ->
            dialog.dismiss()
            analisisProdukTerpilih(selectedProduct)
        }

        rvResults.adapter = adapter
        rvResults.layoutManager = LinearLayoutManager(this)
        dialog.show()
    }

    private fun prosesDeteksiProduk(namaProduk: String) {
        if (isProcessing) return
        isProcessing = true

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val user = authRepo.getUserProfile(authRepo.getCurrentUser()?.uid ?: "")
                val activeAllergens = user?.allergenProfile?.activeAllergens()

                if (activeAllergens.isNullOrEmpty()) {
                    showEmptyAllergenDialog()
                    return@launch
                }

                val response = scanRepo.searchMultipleProducts(namaProduk)
                if (response.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Produk tidak ditemukan", Toast.LENGTH_SHORT).show()
                } else {
                    binding.etSearch.text.clear()
                    showProductSelectionDialog(response)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                isProcessing = false
            }
        }
    }

    private fun analisisProdukTerpilih(product: ProductDBModel) {
        if (isProcessing) return
        isProcessing = true

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val uid = authRepo.getCurrentUser()?.uid ?: return@launch
                val user = authRepo.getUserProfile(uid)

                val scanResult = AllergenEngine.analyze(
                    productName = product.name,
                    productBrand = product.brand,
                    rawIngredients = product.rawIngredientsText,
                    userProfile = user?.allergenProfile ?: AllergenProfile(),
                    userId = uid
                )

                scanRepo.saveScanResult(scanResult)
                loadHistoryData()
                binding.rvHistory.scrollToPosition(0)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Gagal menganalisis produk", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                isProcessing = false
            }
        }
    }

    private fun highlightAllergens(ingredients: String, activeAllergens: List<AllergenType>): SpannableString {
        val spannable = SpannableString(ingredients)
        val lowerIngredients = ingredients.lowercase()
        // SINKRONISASI: Ambil keywords langsung dari SynonymMap agar identik dengan Engine
        val keywords = SynonymMap.getKeywordsFor(activeAllergens)

        for (keyword in keywords) {
            var startIndex = lowerIngredients.indexOf(keyword.lowercase())
            while (startIndex >= 0) {
                val endIndex = startIndex + keyword.length
                spannable.setSpan(ForegroundColorSpan(Color.RED), startIndex, endIndex, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                startIndex = lowerIngredients.indexOf(keyword.lowercase(), endIndex)
            }
        }

        return spannable
    }

    fun showIngredientDetailDialog(scanResult: ScanResult) {
        lifecycleScope.launch {
            val dialogView = layoutInflater.inflate(R.layout.dialog_ingredient_detail, null)
            val dialog = AlertDialog.Builder(this@MainActivity)
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val tvName = dialogView.findViewById<TextView>(R.id.tvDetailName)
            val tvIngredients = dialogView.findViewById<TextView>(R.id.tvDetailIngredients)
            val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)

            tvName.text = scanResult.productName

            val uid = authRepo.getCurrentUser()?.uid ?: ""
            val userProfile = authRepo.getUserProfile(uid)?.allergenProfile
            val activeAllergens = userProfile?.activeAllergens() ?: emptyList()

            val rawText = if (scanResult.analyzedIngredients.isEmpty()) {
                "Data komposisi tidak tersedia di database untuk produk ini."
            } else {
                scanResult.analyzedIngredients.joinToString(", ") { it.name }
            }

            tvIngredients.text = highlightAllergens(rawText, activeAllergens)

            btnClose.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
}
