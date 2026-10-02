package com.dentalscan.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

object VoiceFeedback {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingMessages = mutableListOf<String>()

    private val URDU_LOCALE = Locale("ur", "PK")
    private val ENGLISH_LOCALE = Locale.ENGLISH
    private var currentLocale = ENGLISH_LOCALE

    // Voice on/off switch (Settings). Default is on.
    private var enabled = true

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            pendingMessages.clear()
            tts?.stop()
        }
    }

    fun initialize(context: Context) {
        enabled = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("voice_enabled", true)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setLanguage(currentLocale)

                pendingMessages.forEach { speakInternal(it) }
                pendingMessages.clear()

                Log.d("VoiceFeedback", "TTS initialized")
            } else {
                Log.e("VoiceFeedback", "TTS initialization failed")
            }
        }
    }

    fun setLanguage(isUrdu: Boolean) {
        currentLocale = if (isUrdu) URDU_LOCALE else ENGLISH_LOCALE
        if (isInitialized) {
            setLanguage(currentLocale)
        }
    }

    private fun setLanguage(locale: Locale) {
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(ENGLISH_LOCALE)
            Log.w("VoiceFeedback", "Urdu not supported, using English")
        }
    }

    fun speak(message: String) {
        if (!enabled) return
        if (!isInitialized) {
            pendingMessages.add(message)
            return
        }
        speakInternal(message)
    }

    private fun speakInternal(message: String) {
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun speakDetectionResult(condition: String, confidence: String, isUrdu: Boolean) {
        val message = if (isUrdu) {
            when (condition.lowercase()) {
                "caries" -> "آپ کے دانتوں میں سڑن پائی گئی ہے۔ $confidence یقین۔"
                "gingivitis" -> "آپ کے مسوڑوں میں سوزش ہے۔ $confidence یقین۔"
                "calculus" -> "آپ کے دانتوں پر پتھر بن گیا ہے۔ $confidence یقین۔"
                else -> "دانتوں میں کچھ خرابی پائی گئی ہے۔"
            }
        } else {
            "$condition detected with $confidence confidence."
        }
        speak(message)
    }

    fun speakGuidance(message: String) {
        if (enabled && isInitialized) {
            tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
    }
}