package dev.anilbeesetti.nextplayer.core.common.mod69

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

/** 69 mod: picker contracts that open the chosen picker (69-Material Files / MiXplorer / phone). */
object Mod69PickerContracts {
    class Tree : ActivityResultContracts.OpenDocumentTree() {
        override fun createIntent(context: Context, input: Uri?): Intent =
            Mod69Picker.apply(context, super.createIntent(context, input))
    }

    class Open : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent =
            Mod69Picker.apply(context, super.createIntent(context, input))
    }

    class Create(mime: String) : ActivityResultContracts.CreateDocument(mime) {
        override fun createIntent(context: Context, input: String): Intent =
            Mod69Picker.apply(context, super.createIntent(context, input))
    }
}
