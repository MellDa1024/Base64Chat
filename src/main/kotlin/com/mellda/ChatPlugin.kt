package com.mellda

import com.lambda.client.plugin.api.Plugin
import com.lambda.client.util.text.MessageSendHelper
import com.mellda.modules.Base64Chat

internal object ChatPlugin : Plugin() {

    override fun onLoad() {
        MessageSendHelper.sendWarningMessage("If base64-encoded chat's length is longer than maximum chat length, it will be cancelled.")
        MessageSendHelper.sendWarningMessage("DO NOT SEND SECRET INFORMATION WITH THIS MODULE, ANYBODY CAN DECODE IT EASILY.")
        modules.add(Base64Chat)
    }

    override fun onUnload() {
        // Here you can unregister threads etc...
    }
}