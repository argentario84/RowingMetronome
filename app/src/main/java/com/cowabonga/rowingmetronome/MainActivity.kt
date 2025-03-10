package com.cowabonga.rowingmetronome
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.content.ContextCompat
import android.graphics.drawable.ColorDrawable
import kotlin.concurrent.thread
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private lateinit var spmInput: EditText
    private lateinit var waterPhaseInput: EditText
    private lateinit var startStopButton: Button
    private lateinit var enableToneCheckBox: CheckBox
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var metronomeRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Imposta i colori della barra superiore
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimary)))
        supportActionBar?.title = "Rowing Metronome"

        spmInput = findViewById(R.id.spmInput)
        waterPhaseInput = findViewById(R.id.waterPhaseInput)
        startStopButton = findViewById(R.id.startStopButton)
        enableToneCheckBox = findViewById(R.id.enableToneCheckBox)

        startStopButton.setOnClickListener {
            if (isRunning) {
                stopMetronome()
            } else {
                startMetronome()
            }
        }
    }

    private fun startMetronome() {
        val spm = spmInput.text.toString().toInt()
        val waterPhase = waterPhaseInput.text.toString().toInt()
        val cycleDuration = (60000 / spm).toLong()
        val waterDuration = (cycleDuration * waterPhase / 100).toLong()
        val returnDuration = cycleDuration - waterDuration
        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

        metronomeRunnable = object : Runnable {
            override fun run() {
                // Suono del metronomo per il colpo a 0
                //toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 100)
                toneGenerator.startTone(ToneGenerator.TONE_SUP_BUSY, 300)

                if (enableToneCheckBox.isChecked) {
                    // Suono crescente per la fase in acqua
                    generateTone(500f, 1500f, waterDuration)

                    handler.postDelayed({
                        // Suono decrescente per la fase aerea di ritorno
                        generateTone(1500f, 500f, returnDuration)
                    }, waterDuration)
                }

                handler.postDelayed(this, cycleDuration)
            }
        }

        handler.post(metronomeRunnable!!)
        isRunning = true
        startStopButton.text = "Ferma"
    }

    private fun stopMetronome() {
        handler.removeCallbacks(metronomeRunnable!!)
        isRunning = false
        startStopButton.text = "Avvia"
    }

    private fun generateTone(startFreq: Float, endFreq: Float, durationMs: Long) {
        thread {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            val volume = 32767 // Massima ampiezza per un suono chiaro

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val freq = startFreq + (endFreq - startFreq) * (i.toDouble() / numSamples)
                samples[i] = (volume * sin(2 * Math.PI * freq * t)).toInt().toShort()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                numSamples * 2,
                AudioTrack.MODE_STATIC
            )

            audioTrack.write(samples, 0, numSamples)
            audioTrack.play()
        }
    }
}
