package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.DataStoreManager
import com.example.gemini.Content
import com.example.gemini.GenerateContentRequest
import com.example.gemini.GenerationConfig
import com.example.gemini.ImageConfig
import com.example.gemini.Part
import com.example.gemini.InlineData
import com.example.gemini.RetrofitClient
import com.example.models.Template
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MainViewModel(context: Context) : ViewModel() {
    private val dataStoreManager = DataStoreManager(context)
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _aiGenerating = MutableStateFlow(false)
    val aiGenerating: StateFlow<Boolean> = _aiGenerating
    
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    init {
        loadTemplates()
        checkPremiumStatus()
    }

    private fun loadTemplates() {
        try {
            firestore.collection("templates").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    Template(
                        id = doc.id,
                        category = doc.getString("category") ?: "Others",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isPremium = doc.getBoolean("isPremium") ?: false
                    )
                }
                _templates.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadTemplate(uri: Uri, category: String, isPremiumTemplate: Boolean) {
        viewModelScope.launch {
            try {
                _uploading.value = true
                val fileName = UUID.randomUUID().toString() + ".jpg"
                val ref = storage.reference.child("templates/$fileName")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                
                firestore.collection("templates").add(
                    mapOf(
                        "category" to category,
                        "imageUrl" to downloadUrl,
                        "isPremium" to isPremiumTemplate
                    )
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uploading.value = false
            }
        }
    }

    private fun checkPremiumStatus() {
        viewModelScope.launch {
            val expiry = dataStoreManager.premiumExpiryDate.first()
            val currentTime = System.currentTimeMillis()
            _isPremium.value = currentTime < expiry
        }
    }

    fun activatePremium() {
        viewModelScope.launch {
            // 30 days = 30L * 24 * 60 * 60 * 1000
            val expireTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            dataStoreManager.setPremiumExpiryDate(expireTime)
            _isPremium.value = true
        }
    }

    suspend fun generateAiImage(prompt: String, size: String = "1K", isPro: Boolean = false): Bitmap? {
        _aiGenerating.value = true
        val modelName = if (isPro) "gemini-3-pro-image-preview" else "gemini-3.1-flash-image-preview"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(imageConfig = ImageConfig("1:1", size)),
            responseModalities = listOf("TEXT", "IMAGE")
        )
        try {
            val response = RetrofitClient.service.generateContent(modelName, BuildConfig.GEMINI_API_KEY, request)
            val inlineData = response.candidates.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData
            inlineData?.data?.let { base64 ->
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                _aiGenerating.value = false
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _aiGenerating.value = false
        return null
    }

    suspend fun analyzeImage(base64Image: String, prompt: String = "What is in this image?"): String {
        _aiGenerating.value = true
        val modelName = "gemini-3.1-pro-preview"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(
                Part(text = prompt),
                Part(inlineData = InlineData("image/jpeg", base64Image))
            )))
        )
        var result = "Analysis failed."
        try {
            val response = RetrofitClient.service.generateContent(modelName, BuildConfig.GEMINI_API_KEY, request)
            result = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response text found."
        } catch (e: Exception) {
            e.printStackTrace()
            result = "Error: ${e.message}"
        }
        _aiGenerating.value = false
        return result
    }
}
