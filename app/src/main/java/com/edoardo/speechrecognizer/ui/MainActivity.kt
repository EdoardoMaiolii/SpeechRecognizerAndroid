package com.edoardo.speechrecognizer.ui

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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.edoardo.speechrecognizer.R
import com.edoardo.speechrecognizer.databinding.ActivityMainBinding
import com.edoardo.speechrecognizer.model.Output
import com.edoardo.speechrecognizer.model.Word
import com.edoardo.speechrecognizer.ui.OutputAdapter

enum class RecognizerState(val text: String) {
    WAITING("Waiting for commands"),
    LISTENING("Listening"),
    PREPARING("Preparing the recognizer...")
}
class MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var outputAdapter: OutputAdapter
    private val TAG = MainActivity::class.java.simpleName
    private var speechRecognizer: SpeechRecognizer? = null
    private val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    private var currentState : RecognizerState = RecognizerState.PREPARING
    private var isStopping = false

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

    private val commandWords = arrayListOf<Word>(
        Word(arrayListOf("code", "cold","cod", "good"),"code"),
        Word(arrayListOf("count", "countdown","caunt"),"count"),
        Word(arrayListOf("reset", "present"),"reset"),
        Word(arrayListOf("back", "bak", "bac", "bec", "beck"),"back"),
        )
//    private val parametersWords = arrayListOf<String>("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    private val parametersWords = arrayListOf<Word>(
    Word(arrayListOf("zero","0", "ziro"), "0"),
        Word(arrayListOf("one","1", "uan"), "1"),
    Word(arrayListOf("two", "2", "chu", "to"), "2"),
    Word(arrayListOf("three","3", "tree", "free"), "3"),
    Word(arrayListOf("4","four", "for"), "4"),
    Word(arrayListOf("5", "five"), "5"),
    Word(arrayListOf("6", "six", "sics"), "6"),
    Word(arrayListOf("7", "seven"), "7"),
    Word(arrayListOf("8", "eight", "eit","ete"), "8"),
    Word(arrayListOf("9", "nine", "nain", "-line", "nin", "line"), "9"),
    )

    private var currentOutputs = arrayListOf<Output>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-PH")
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        outputAdapter = OutputAdapter()
        binding.outputsRecyclerView.adapter = outputAdapter

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
                Log.d(TAG, "Error $error")
                if (!isStopping) {
                    when (error) {
                        SpeechRecognizer.ERROR_NETWORK -> Toast.makeText(this@MainActivity, "Check your internet connection", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Toast.makeText(this@MainActivity, "Network timeout error", Toast.LENGTH_SHORT).show()
                        SpeechRecognizer.ERROR_NO_MATCH -> {}
                        else -> Toast.makeText(this@MainActivity, "Error $error", Toast.LENGTH_SHORT).show()
                    }
                    speechRecognizer?.startListening(speechRecognizerIntent)
                } else {
                    isStopping = false
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onResults(results: Bundle?) {
                results?.let {
                    val data: ArrayList<String> = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) as ArrayList<String>
                    Log.d(TAG, data.joinToString())
                    binding.textCurrentCommand.text = ""
                    binding.textCurrentParameters.text = ""
                    binding.textSpeechDetected.text = data.first()
                    val words = data.first().split(" ")
                    val currentParameters = arrayListOf<Word>()
                    words.forEach {
                        val lowerCasedString = it.lowercase()
                        val command = commandWords.firstOrNull { command -> command.spelling.contains(lowerCasedString) }
                        if (command != null) {
                            if(command.value == "back") {
                                //back command
                                backCommand()
                            } else if (command.value == "reset") {
                                //reset command
                                binding.textCurrentCommand.text = ""
                                binding.textCurrentParameters.text = ""
                                currentParameters.clear()
                                updateState(RecognizerState.WAITING)
                            }
                            else {
                                if (currentParameters.isNotEmpty()) {
                                    addOutput(currentParameters)
                                    currentParameters.clear()
                                    binding.textCurrentParameters.text = ""
                                }
                                binding.textCurrentCommand.text = command.value
                                updateState(RecognizerState.LISTENING)
                            }
                        }
                        else if (currentState == RecognizerState.LISTENING) {
                            //listening for parameters
                            val parameter = parametersWords.firstOrNull { parameter -> parameter.spelling.contains(lowerCasedString)}
                            if ( parameter!= null) {
                                //Found a number
                                currentParameters.add(parameter)
                                binding.textCurrentParameters.text = binding.textCurrentParameters.text.toString() + parameter.value
                            }
                        }
                    }
                    if (currentParameters.isNotEmpty()) {
                        addOutput(currentParameters)
                        currentParameters.clear()
                    }
                }
                updateUI()
                updateState(RecognizerState.PREPARING)
                speechRecognizer?.startListening(speechRecognizerIntent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.let {
                    val data: ArrayList<String> = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) as ArrayList<String>
                    binding.textSpeechDetected.text = data[data.size - 1];
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {

            }

        })

        binding.stopRecognizerBtn.setOnClickListener {
            speechRecognizer?.let {
                isStopping = true
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

    private fun updateUI() {
        Log.d(TAG, currentOutputs.joinToString { it -> "value ${it.value} command ${it.command}" })
        outputAdapter.addOutputs(currentOutputs)
        binding.outputsRecyclerView.scrollToPosition(outputAdapter.itemCount - 1)
        currentOutputs.clear()
    }

    fun backCommand() {
        outputAdapter.removeLastOutput()
    }

    fun addOutput(currentParameters: ArrayList<Word>) {
        val output = Output(binding.textCurrentCommand.text.toString(),
            currentParameters.joinToString("") { it.value })
        Log.d(TAG, "Aggiungo il comando ${binding.textCurrentCommand.text} con i parametri ${output.value}")
        currentOutputs.add(output)
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