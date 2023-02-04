package com.photopuzzle.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class ChooseImageSourceFragment : Fragment() {
    private val callback: OnImageSourceChosenCallback?
        get() = activity as? OnImageSourceChosenCallback

    private var pickImageLauncher: ActivityResultLauncher<String>? = null
    private var takePictureLauncher: ActivityResultLauncher<Uri>? = null
    private var pictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? -> dispatchResultAsync(uri) }
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            if (success) dispatchResultAsync(pictureUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_choose_image_source, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.pick_from_gallery_button)
            .setOnClickListener { pickFromGallery() }
        view.findViewById<View>(R.id.take_picture_button)
            .setOnClickListener { takePicture() }
    }

    private fun pickFromGallery() {
        val options = ActivityOptionsCompat.makeBasic()
        pickImageLauncher?.launch("image/*", options)
    }

    private fun takePicture() {
        val uri = getTmpFileUri()
        pictureUri = uri
        takePictureLauncher?.launch(uri)
    }

    private fun getTmpFileUri(): Uri {
        val context = requireContext().applicationContext
        val cacheDir = context.cacheDir
        val tmpFile = File.createTempFile("puzzle_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    private fun dispatchResultAsync(uri: Uri?) {
        if (uri == null) {
            return
        }
        view?.post {
            if (view != null) {
                callback?.onImageSourceChosen(uri)
            }
        }
    }

    fun interface OnImageSourceChosenCallback {
        fun onImageSourceChosen(imageUri: Uri)
    }
}