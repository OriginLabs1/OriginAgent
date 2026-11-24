package com.oneorigin.originagent.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * On-device speech entry for the OpenDuet-style mic orb.
 *
 * The recogniser is a platform service; no audio leaves the device through Origin Agent.
 * Availability is reported honestly — no transcription is simulated when it is absent.
 */
class VoiceDictation(private val context: Context) {

    sealed interface State {
        data object Idle : State
        data object WaitingForPermission : State
        data object Listening : State
        data object Transcribing : State
        data class Error(val message: String) : State
        data class Result(val text: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!hasPermission()) {
            _state.value = State.WaitingForPermission
            return
        }
        if (!isAvailable) {
            _state.value = State.Error("Voice input is not available on this device. Type instead.")
            return
        }
        releaseRecognizer()
        _state.value = State.Listening
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        this.recognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = State.Listening
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                if (_state.value is State.Listening) _state.value = State.Transcribing
            }

            override fun onError(error: Int) {
                _state.value = State.Error(messageFor(error))
            }

            override fun onResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (heard.isEmpty()) {
                    _state.value = State.Error("Nothing was heard. Try again or type instead.")
                } else {
                    _state.value = State.Result(heard)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "I'm listening")
        }
        runCatching { recognizer.startListening(intent) }
            .onFailure { _state.value = State.Error("Voice input could not start. Type instead.") }
    }

    fun reset() {
        releaseRecognizer()
        _state.value = State.Idle
    }

    fun release() {
        releaseRecognizer()
        _state.value = State.Idle
    }

    private fun releaseRecognizer() {
        recognizer?.runCatching { destroy() }
        recognizer = null
    }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone problem. Try again or type instead."
        SpeechRecognizer.ERROR_CLIENT -> "Voice input stopped. Type instead."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice input."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech service is unavailable offline right now. Type instead."
        SpeechRecognizer.ERROR_NO_MATCH -> "Nothing was recognised. Try again or type instead."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice input is busy. Try again in a moment."
        SpeechRecognizer.ERROR_SERVER -> "Speech service is unavailable. Type instead."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Tap the orb to try again."
        else -> "Voice input stopped. Type instead."
    }
}
