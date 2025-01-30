package com.hari.management.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract

class PickMultipleContacts : ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        
        val uris = mutableListOf<Uri>()
        val clipData = intent.clipData
        
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        } else {
            intent.data?.let { uris.add(it) }
        }
        
        return uris
    }
} 