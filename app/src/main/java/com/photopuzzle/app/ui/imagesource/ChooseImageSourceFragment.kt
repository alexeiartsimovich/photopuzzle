package com.photopuzzle.app.ui.imagesource

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
import com.photopuzzle.app.BuildConfig
import com.photopuzzle.app.R
import java.io.File

class ChooseImageSourceFragment : Fragment() {
    private val callback: OnImageSourceChosenCallback?
        get() = activity as? OnImageSourceChosenCallback

    private var pickImageLauncher: ActivityResultLauncher<String>? = null
    private var takePictureLauncher: ActivityResultLauncher<Uri>? = null
    private var pictureUri: Uri? = null
    private var pictureFilepath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? -> dispatchResultAsync(uri, null) }
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            if (success) dispatchResultAsync(pictureUri, pictureFilepath)
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
        pictureUri = null
        pictureFilepath = null
        val options = ActivityOptionsCompat.makeBasic()
        pickImageLauncher?.launch("image/*", options)
    }

    private fun takePicture() {
        val context = requireContext().applicationContext
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "image_puzzle").apply {
            createNewFile()
            deleteOnExit()
        }
        pictureUri = FileProvider.getUriForFile(context,
            "${BuildConfig.APPLICATION_ID}.provider", file)
        pictureFilepath = file.absolutePath
        takePictureLauncher?.launch(pictureUri)
    }

    private fun dispatchResultAsync(uri: Uri?, filepath: String?) {
        if (uri == null) {
            return
        }
        view?.post {
            if (view != null) {
                callback?.onImageSourceChosen(uri, filepath)
            }
        }
    }

    fun interface OnImageSourceChosenCallback {
        fun onImageSourceChosen(imageUri: Uri, filepath: String?)
    }
}