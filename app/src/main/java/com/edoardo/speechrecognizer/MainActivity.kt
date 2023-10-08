package com.edoardo.speechrecognizer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edoardo.speechrecognizer.databinding.ActivityMainBinding

enum class RecognizerState(val text: String) {
    WAITING("Waiting for commands"),
    LISTENING("Listening"),
    PREPARING("Preparing the recognizer...")
}
class MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = MainActivity::class.java.simpleName
    private var speechRecognizer: SpeechRecognizer? = null
    private val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    private var currentState : RecognizerState = RecognizerState.PREPARING

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                speechRecognizer?.startListening(speechRecognizerIntent)
            } else {
                showPermissionNotAvailableDialog()
            }
        }

    private val commandWords = arrayListOf<String>("code", "count")
    private val parametersWords = arrayListOf<String>("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateState(RecognizerState.WAITING)
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {

            }

            override fun onBufferReceived(buffer: ByteArray?) {

            }

            override fun onEndOfSpeech() {

            }

            override fun onError(error: Int) {
                Log.d(TAG, "Error")
                speechRecognizer?.startListening(speechRecognizerIntent)
            }

            @SuppressLint("SetTextI18n")
            override fun onResults(results: Bundle?) {
                results?.let {
                    val data: ArrayList<String> = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) as ArrayList<String>
                    binding.textCurrentCommand.text = ""
                    binding.textCurrentParameters.text = ""
                    binding.textSpeechDetected.text = data.first()
                    val words = data.first().split(" ")
                    words.forEach {
                        if (currentState == RecognizerState.LISTENING) {
                            //listening for parameters
                            if (parametersWords.contains(it.lowercase())) {
                                //Found a number
                                binding.textCurrentParameters.text = binding.textCurrentParameters.text.toString() + it
                            }
                        } else if (currentState == RecognizerState.WAITING && commandWords.contains(it.lowercase())) {
                            updateState(RecognizerState.LISTENING)
                            binding.textCurrentCommand.text = it
                        }
                    }
                }
                updateState(RecognizerState.PREPARING)
                speechRecognizer?.startListening(speechRecognizerIntent)
            }

            override fun onPartialResults(partialResults: Bundle?) {

            }

            override fun onEvent(eventType: Int, params: Bundle?) {

            }

        })

        binding.stopRecognizerBtn.setOnClickListener {
            speechRecognizer?.let {
                it.stopListening()
                updateState(RecognizerState.PREPARING)
            }
        }

        binding.startRecognizerBtn.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    speechRecognizer?.startListening(speechRecognizerIntent)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showRecordAudioDialog()
            }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    private fun updateState(state: RecognizerState) {
        this.currentState = state
        binding.textCurrentState.text = state.text
    }
    private fun showRecordAudioDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.record_audio_permission))
            .setMessage(getString(R.string.record_audio_explanation))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }
    private fun showPermissionNotAvailableDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied))
            .setMessage(getString(R.string.record_audio_explanation_denied))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }
}