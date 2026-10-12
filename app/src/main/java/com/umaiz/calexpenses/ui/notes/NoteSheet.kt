package com.umaiz.calexpenses.ui.notes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.NoteEntity
import com.umaiz.calexpenses.databinding.SheetNoteBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class NoteSheet(private val existing: NoteEntity? = null) : BottomSheetDialogFragment() {

    private var _b: SheetNoteBinding? = null
    private val b get() = _b!!

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recognizer: SpeechRecognizer? = null
    private var audioPath: String? = null
    private var isRecording = false
    private var pendingAction: String? = null

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                !granted -> b.tvStatus.text =
                    "Microphone permission denied — you can still type the note."
                pendingAction == "record" -> startRecording()
                pendingAction == "dictate" -> startDictation()
            }
            pendingAction = null
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetNoteBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = (requireActivity().application as CalExpensesApp).repository

        if (existing != null) {
            b.tvTitle.text = "Edit note"
            b.etTitle.setText(existing.title)
            b.etText.setText(existing.text)
            audioPath = existing.audioPath
            b.btnDelete.visibility = View.VISIBLE
        } else {
            b.tvTitle.text = "New note"
        }

        if (!audioPath.isNullOrBlank() && File(audioPath!!).exists()) b.btnPlay.visibility = View.VISIBLE

        b.btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else ensurePermission("record")
        }
        b.btnDictate.setOnClickListener { ensurePermission("dictate") }
        b.btnPlay.setOnClickListener { togglePlay() }

        b.btnSave.setOnClickListener {
            val title = b.etTitle.text?.toString()?.trim().orEmpty()
            val text = b.etText.text?.toString()?.trim().orEmpty()
            if (title.isBlank() && text.isBlank() && audioPath.isNullOrBlank()) {
                toast("Nothing to save yet"); return@setOnClickListener
            }
            val finalTitle = title.ifBlank { text.take(40).ifBlank { "Voice note" } }
            lifecycleScope.launch {
                if (existing == null) {
                    repo.addNote(NoteEntity(title = finalTitle, text = text, audioPath = audioPath))
                } else {
                    repo.updateNote(
                        existing.copy(
                            title = finalTitle, text = text, audioPath = audioPath,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                dismiss()
            }
        }

        b.btnDelete.setOnClickListener {
            val n = existing ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete note?")
                .setMessage("The note and any attached recording will be removed.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        n.audioPath?.let { runCatching { File(it).delete() } }
                        repo.deleteNote(n); dismiss()
                    }
                }.show()
        }
    }

    // ---------------------------------------------------------------- recording
    private fun ensurePermission(action: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (action == "record") startRecording() else startDictation()
        } else {
            pendingAction = action
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        try {
            val dir = File(requireContext().filesDir, "notes").apply { mkdirs() }
            val file = File(dir, "note_${System.currentTimeMillis()}.m4a")
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext())
            else @Suppress("DEPRECATION") MediaRecorder()
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            audioPath = file.absolutePath
            isRecording = true
            b.btnRecord.text = "Stop"
            b.tvStatus.text = "Recording… tap Stop when you're done."
        } catch (e: Exception) {
            b.tvStatus.text = "Could not start recording: ${e.message}"
            runCatching { recorder?.release() }
            recorder = null
        }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            runCatching { recorder?.release() }
            recorder = null
        }
        isRecording = false
        b.btnRecord.text = "Record"
        b.btnPlay.visibility = View.VISIBLE
        b.tvStatus.text = "Recording saved. Tap Play to listen."
    }

    // ---------------------------------------------------------------- dictation
    private fun startDictation() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            b.tvStatus.text = "Speech recognition isn't available here — please type the note."
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { b.tvStatus.text = "Listening… speak now" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { b.tvStatus.text = "Processing…" }
                override fun onError(error: Int) {
                    b.tvStatus.text = "Speech error ($error). You can type the text instead."
                }
                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (spoken.isNotBlank()) {
                        val cur = b.etText.text?.toString().orEmpty()
                        val merged = if (cur.isBlank()) spoken else "$cur $spoken"
                        b.etText.setText(merged)
                        b.etText.setSelection(merged.length)
                    }
                    b.tvStatus.text = "Added to the note — edit it freely."
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(intent)
        b.tvStatus.text = "Listening…"
    }

    // ---------------------------------------------------------------- playback
    private fun togglePlay() {
        val path = audioPath ?: return
        if (player?.isPlaying == true) {
            player?.pause(); b.btnPlay.text = "Play"; return
        }
        if (player == null) {
            player = runCatching {
                MediaPlayer().apply {
                    setDataSource(path)
                    setOnCompletionListener { b.btnPlay.text = "Play" }
                    prepare()
                }
            }.getOrNull()
        }
        player?.start()
        b.btnPlay.text = "Pause"
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        runCatching { if (isRecording) recorder?.stop() }
        runCatching { recorder?.release() }
        runCatching { player?.release() }
        runCatching { recognizer?.destroy() }
        recorder = null; player = null; recognizer = null
        _b = null
    }
}
