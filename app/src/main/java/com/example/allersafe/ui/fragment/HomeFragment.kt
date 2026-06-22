package com.example.allersafe.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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
import com.example.allersafe.databinding.FragmentHomeBinding
import com.example.allersafe.engine.AllergenEngine
import com.example.allersafe.engine.SynonymMap
import com.example.allersafe.ui.MainActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val authRepo = AuthRepository()
    private val scanRepo = ScanRepository()
    private val historyRepo = HistoryRepository()

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var searchAdapter: SearchProductAdapter

    private var isProcessing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearch()
        loadHistoryData()
    }

    private fun setupRecyclerViews() {
        historyAdapter = HistoryAdapter(emptyList()) { scanResult ->
            showIngredientDetailDialog(scanResult)
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        searchAdapter = SearchProductAdapter(emptyList()) { product ->
            analisisProdukTerpilih(product)
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(v.text.toString())
                true
            } else false
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Bisa tambahkan debouncing search di sini jika perlu
            }
        })
    }

    private fun loadHistoryData() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = historyRepo.getScanHistory(uid)
            if (result.isSuccess) {
                historyAdapter.updateData(result.getOrDefault(emptyList()))
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank() || isProcessing) return
        isProcessing = true

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val response = scanRepo.searchMultipleProducts(query)
                if (response.isEmpty()) {
                    Toast.makeText(requireContext(), "Produk tidak ditemukan", Toast.LENGTH_SHORT).show()
                } else {
                    showProductSelectionDialog(response)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                isProcessing = false
            }
        }
    }

    private fun showProductSelectionDialog(products: List<ProductDBModel>) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val rv = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(requireContext())
        
        val adapter = SearchProductAdapter(products) { product ->
            dialog.dismiss()
            analisisProdukTerpilih(product)
        }
        rv.adapter = adapter

        dialog.show()
    }

    private fun analisisProdukTerpilih(product: ProductDBModel) {
        if (isProcessing) return
        isProcessing = true

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val uid = authRepo.getCurrentUser()?.uid ?: return@launch
                val user = authRepo.getUserProfile(uid)

                val scanResult = AllergenEngine.analyze(
                    productName = product.name,
                    productBrand = product.brand,
                    imageUrl = product.imageUrl, // FIX: Pass imageUrl
                    rawIngredients = product.rawIngredientsText,
                    userProfile = user?.allergenProfile ?: AllergenProfile(),
                    userId = uid
                )

                scanRepo.saveScanResult(scanResult)
                loadHistoryData()
                binding.rvHistory.scrollToPosition(0)
                
                // Tampilkan hasil scan
                showScanResultDialog(scanResult)
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menganalisis produk", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                isProcessing = false
            }
        }
    }

    private fun showScanResultDialog(scanResult: ScanResult) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_scan_result, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imgProduct = dialogView.findViewById<ImageView>(R.id.imgProductResult)
        val tvName = dialogView.findViewById<TextView>(R.id.tvProductNameResult)
        val tvBrand = dialogView.findViewById<TextView>(R.id.tvProductBrandResult)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvScanStatusResult)

        tvName.text = scanResult.productName
        tvBrand.text = scanResult.productBrand
        tvStatus.text = scanResult.scanStatus.name

        // Tampilkan gambar produk
        Glide.with(this)
            .load(scanResult.imageUrl.ifEmpty { R.drawable.ic_placeholder_product })
            .placeholder(R.drawable.ic_placeholder_product)
            .error(R.drawable.ic_placeholder_product)
            .into(imgProduct)

        dialogView.findViewById<Button>(R.id.btnDetailIngredients).setOnClickListener {
            dialog.dismiss()
            showIngredientDetailDialog(scanResult)
        }
        
        dialogView.findViewById<Button>(R.id.btnDoneScan).setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    private fun highlightAllergens(ingredients: String, activeAllergens: List<AllergenType>): SpannableString {
        val spannable = SpannableString(ingredients)
        val lowerIngredients = ingredients.lowercase()
        val keywords = SynonymMap.getKeywordsFor(activeAllergens)

        for (keyword in keywords) {
            var startIndex = lowerIngredients.indexOf(keyword.lowercase())
            while (startIndex >= 0) {
                val endIndex = startIndex + keyword.length
                spannable.setSpan(ForegroundColorSpan(Color.RED), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                startIndex = lowerIngredients.indexOf(keyword.lowercase(), endIndex)
            }
        }
        return spannable
    }

    private fun showIngredientDetailDialog(scanResult: ScanResult) {
        viewLifecycleOwner.lifecycleScope.launch {
            val dialogView = layoutInflater.inflate(R.layout.dialog_ingredient_detail, null)
            val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val imgProduct = dialogView.findViewById<ImageView>(R.id.imgProductDetail)
            if (imgProduct != null) {
                Glide.with(this@HomeFragment)
                    .load(scanResult.imageUrl.ifEmpty { R.drawable.ic_placeholder_product })
                    .placeholder(R.drawable.ic_placeholder_product)
                    .into(imgProduct)
            }

            dialogView.findViewById<TextView>(R.id.tvDetailName).text = scanResult.productName

            val uid = authRepo.getCurrentUser()?.uid ?: ""
            val userProfile = authRepo.getUserProfile(uid)?.allergenProfile
            val activeAllergens = userProfile?.activeAllergens() ?: emptyList()

            val rawText = if (scanResult.analyzedIngredients.isEmpty()) {
                "Data komposisi tidak tersedia."
            } else {
                scanResult.analyzedIngredients.joinToString(", ") { it.name }
            }

            dialogView.findViewById<TextView>(R.id.tvDetailIngredients).text =
                highlightAllergens(rawText, activeAllergens)

            dialogView.findViewById<Button>(R.id.btnCloseDetail).setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
