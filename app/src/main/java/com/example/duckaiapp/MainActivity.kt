package com.example.duckaiapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var chatHistoryButton: Button
    private lateinit var clearHistoryButton: Button
    private lateinit var chatDB: ChatHistoryDatabase

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        chatDB = ChatHistoryDatabase(this)

        // Set up WebView
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject JavaScript to intercept chat content
                webView.evaluateJavascript("""
                    (function() {
                        const observer = new MutationObserver(mutations => {
                            mutations.forEach(mutation => {
                                if (mutation.addedNodes.length) {
                                    // Get chat content (this selector would need to be updated for duck.ai)
                                    const chatContent = document.querySelector('.chat-content')?.innerText;
                                    if (chatContent) {
                                        // Send to Android
                                        Android.saveChatContent(chatContent);
                                    }
                                }
                            });
                        });
                        
                        // Start observing chat container
                        const chatContainer = document.querySelector('#chat-container');
                        if (chatContainer) {
                            observer.observe(chatContainer, { childList: true, subtree: true });
                        }
                    })();
                """, null)
            }
        }
        
        // Enable JavaScript interface
        webView.addJavascriptInterface(WebAppInterface(this, chatDB), "Android")
        
        // Load duck.ai
        webView.loadUrl("https://duck.ai")
        
        // Set up chat history button
        chatHistoryButton = findViewById(R.id.chatHistoryButton)
        chatHistoryButton.setOnClickListener {
            showChatHistory()
        }
        
        // Set up clear history button
        clearHistoryButton = findViewById(R.id.clearHistoryButton)
        clearHistoryButton.setOnClickListener {
            chatDB.clearAllHistory()
            Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showChatHistory() {
        val chatHistory = chatDB.getAllChats()
        
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ChatHistoryAdapter(chatHistory) { chat ->
            // On chat item click, show full chat
            AlertDialog.Builder(this)
                .setTitle("Chat from ${chat.timestamp}")
                .setMessage(decrypt(chat.content, ENCRYPTION_KEY))
                .setPositiveButton("Close", null)
                .show()
        }
        
        AlertDialog.Builder(this)
            .setTitle("Chat History")
            .setView(recyclerView)
            .setPositiveButton("Close", null)
            .show()
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    companion object {
        // This should be generated and stored securely in a real app
        private const val ENCRYPTION_KEY = "DuckAiSecureKey123"
        
        fun encrypt(input: String, key: String): String {
            val secretKey = generateKey(key)
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }
        
        fun decrypt(input: String, key: String): String {
            val secretKey = generateKey(key)
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(input, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }
        
        private fun generateKey(key: String): SecretKeySpec {
            val sha = MessageDigest.getInstance("SHA-256")
            var keyBytes = key.toByteArray(Charsets.UTF_8)
            keyBytes = sha.digest(keyBytes)
            keyBytes = keyBytes.copyOf(16) // Use only first 128 bits
            return SecretKeySpec(keyBytes, "AES")
        }
    }
}

// JavaScript interface
class WebAppInterface(private val context: Context, private val db: ChatHistoryDatabase) {
    @android.webkit.JavascriptInterface
    fun saveChatContent(content: String) {
        val encryptedContent = MainActivity.encrypt(content, MainActivity.ENCRYPTION_KEY)
        db.saveChat(encryptedContent)
    }
}
