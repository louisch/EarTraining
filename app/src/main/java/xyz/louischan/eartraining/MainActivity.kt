package xyz.louischan.eartraining

import android.media.AudioAttributes
import android.media.AudioFormat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.media.AudioTrack
import android.media.AudioManager
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.*
import kotlin.math.roundToInt


val C = 0
val CSharp = 1
val DFlat = CSharp
val D = 2
val DSharp = 3
val EFlat = DSharp
val E = 4
val F = 5
val FSharp = 6
val GFlat = FSharp
val G = 7
val GSharp = 8
val AFlat = GSharp
val A = 9
val ASharp = 10
val BFlat = ASharp
val B = 11

class Note(val pitch: Int, val octave: Int) {

    constructor(fromC0: Int) : this(fromC0.rem(12), fromC0.div(12))

    val A4 = 440.0
    val frequency: Double by lazy {
        val semitones = (octave - 4) * 12 + (pitch - A)
        Math.pow(2.0, semitones / 12.0) * A4
    }

    fun fromC0() : Int {
        return pitch + octave * 12
    }

    override fun toString(): String {
        return when (pitch) {
            C -> "C"
            CSharp -> "D#/Db"
            D -> "D"
            DSharp -> "D#/Eb"
            E -> "E"
            F -> "F"
            FSharp -> "F#/Gb"
            G -> "G"
            GSharp -> "G#/Ab"
            A -> "A"
            ASharp -> "A#/Ab"
            B -> "B"
            else -> "Unknown Note"
        } + octave.toString()
    }
}

val bottomNote = 0
val topNote = C + 8 * 12
val notes = IntRange(bottomNote, topNote).map {
    i -> Note(i)
}

fun note(pitch: Int, octave: Int) : Note {
    return notes[pitch + octave * 12]
}

fun fromC0(pitch: Int, octave: Int) : Int {
    return pitch + octave * 12
}

typealias Chord = List<Note>

val I = 0
val MinorII = 1
val MajorII = 2
val MinorIII = 3
val MajorIII = 4
val PerfectIV = 5
val AugmentedIV = 6
val PerfectV = 7
val MinorVI = 8
val MajorVI = 9
val MinorVII = 10
val MajorVII = 11
val VIII = 12

fun intervalAsString(interval: Int): String {
    return when (interval) {
        I -> "I"
        MinorII -> "Minor II"
        MajorII -> "Major II"
        MinorIII -> "Minor III"
        MajorIII -> "Major III"
        PerfectIV -> "Perfect IV"
        AugmentedIV -> "Augmented IV"
        PerfectV -> "Perfect V"
        MinorVI -> "Minor VI"
        MajorVI -> "Major VI"
        MinorVII -> "Minor VII"
        MajorVII -> "Major VII"
        VIII -> "Octave"
        else -> "Unknown"
    }
}

val majorScale = listOf(I, MajorII, MajorIII, PerfectIV, PerfectV, MajorVI, MajorVII)
val minorScale = listOf(I, MajorII, MinorIII, PerfectIV, PerfectV, MinorVI, MajorVII)

class RelativeChord(vararg val intervals: Int) {
    fun absolute(baseNote: Note) : List<Note> {
        return intervals.map { interval -> Note(baseNote.fromC0() + interval) }
    }
}

fun triad() : RelativeChord {
    return RelativeChord(I, MajorIII, PerfectV)
}

fun sineWave(frequency: Double, sampleRate: Int, duration: Int) : DoubleArray {
    val samples = sampleRate / frequency
    val singleSineWave = DoubleArray(samples.roundToInt())
    for (i in singleSineWave.indices) {
        singleSineWave[i] = Math.sin(2.0 * Math.PI * (i.toDouble() / singleSineWave.size))
    }
    val soundBuffer = DoubleArray(duration)
    for (i in soundBuffer.indices) {
        soundBuffer[i] = singleSineWave[i.rem(singleSineWave.size)]
    }
    return soundBuffer
}

class MainActivity : AppCompatActivity() {

    val rnd = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClickPlaySoundButton(view: View?) {
        val questionText = findViewById<TextView>(R.id.question)
        val noteButtons = findViewById<ConstraintLayout>(R.id.notes)
        val answerText = findViewById<TextView>(R.id.answer)
        questionText.visibility = View.INVISIBLE
        noteButtons.visibility = View.INVISIBLE
        answerText.visibility = View.INVISIBLE

        val sampleRate = 44100
        val duration = 44100
        val noteBases = listOf(
                Note(C, 4),
                Note(F, 4),
                Note(G, 4),
                Note(C, 4)
        )

        val chords = noteBases.map {
            noteBase ->
            val triad = triad().absolute(noteBase)
            mixSounds(triad.map { note -> sineWave(note.frequency, sampleRate, duration) })
        }

        for (chord in chords) {
            playSound(chord, sampleRate)
        }

        val bottomOctave = 4
        val topOctave = 6
        val randomInterval = rnd.nextInt(7)
        val randomPitch = majorScale[randomInterval]
        val randomOctave = bottomOctave + rnd.nextInt(topOctave - bottomOctave)

        val randomNote = Note(randomPitch, randomOctave)
        playNote(randomNote, sampleRate, duration)
        questionText.visibility = View.VISIBLE
        for (i in IntRange(0, noteButtons.childCount - 1)) {
            (noteButtons.getChildAt(i) as Button).setOnClickListener {
                val answer =
                    if (i == randomInterval)
                        getString(R.string.correct_answer, intervalAsString(randomPitch), randomNote.toString())
                    else
                        getString(R.string.wrong_answer, intervalAsString(randomPitch), randomNote.toString())

                answerText.text = answer
                answerText.visibility = View.VISIBLE
            }
        }

        noteButtons.visibility = View.VISIBLE
    }

    private fun playNote(note: Note, sampleRate: Int, duration: Int) {
        playSound(sineWave(note.frequency, sampleRate, duration), sampleRate)
    }

    private fun playSound(sample: DoubleArray, sampleRate: Int) {
        val mBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT)

        val mAudioTrack =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    AudioTrack.Builder()
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .setAudioAttributes(AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build())
                            .setAudioFormat(AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build())
                            .setBufferSizeInBytes(mBufferSize)
                            .build()
                else
                    AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                            mBufferSize, AudioTrack.MODE_STREAM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioTrack.setVolume(AudioTrack.getMaxVolume())
        } else {
            mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        }

        val shortBuffer = ShortArray(sample.size)
        for (i in shortBuffer.indices) {
            shortBuffer[i] = (sample[i] * java.lang.Short.MAX_VALUE).toShort()
        }

        mAudioTrack.play()
        mAudioTrack.write(shortBuffer, 0, shortBuffer.size)
        mAudioTrack.stop()
        mAudioTrack.release()
    }

    private fun mixSounds(inputBuffers: List<DoubleArray>) : DoubleArray {
        if (inputBuffers.isEmpty()) return doubleArrayOf()

        val outputBuffer = DoubleArray(inputBuffers[0].size)
        for (i in outputBuffer.indices) {
            val sum = inputBuffers.fold(0.0) { acc : Double, input : DoubleArray -> acc + input[i] }
            outputBuffer[i] = sum / inputBuffers.size.toDouble()
        }
        return outputBuffer
    }
}
