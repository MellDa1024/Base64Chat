package com.mellda.modules

import com.lambda.client.event.events.PacketEvent
import com.lambda.client.manager.managers.MessageManager.newMessageModifier
import com.mellda.ChatPlugin
import com.lambda.client.module.Category
import com.lambda.client.module.modules.chat.ChatTimestamp
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.text.MessageSendHelper.sendServerMessage
import com.lambda.client.util.threads.safeListener
import net.minecraft.network.play.client.CPacketChatMessage
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.client.event.ClientChatReceivedEvent
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min

/**
 * This is a module. First set properties then settings then add listener.
 * **/
internal object Base64Chat : PluginModule(
    name = "Base64Chat",
    category = Category.CHAT,
    description = "Chats with base64!",
    pluginMain = ChatPlugin
) {
    private val originalChat by setting("Print Original Chat", true)
    private val chatPattern = Pattern.compile("<([0-9a-zA-Z_]+)> (.*)")

    private val modifier = newMessageModifier(
        modifier = {
            if (originalChat) MessageSendHelper.sendChatMessage("Original Message : ${it.packet.message}")
            val message = encode(it.packet.message)
            message.substring(0, min(256, message.length))
        }
    )

    init {
 /*       safeListener<PacketEvent.Send> {
            if (it.packet !is CPacketChatMessage) return@safeListener
            if (sended) {
                sended = false
                it.cancelled = true
            } else {
                val rawMessage = (it.packet as CPacketChatMessage).message
                it.cancelled = true
                if (originalChat) MessageSendHelper.sendChatMessage("Original Message : $rawMessage")
                val encodedMessage = encode(rawMessage)
                if (encodedMessage != "") {
                    sended = true
                    sendServerMessage(encodedMessage)
                }
            }
        }*/
        onEnable {
            modifier.enable()
        }

        onDisable {
            modifier.disable()
        }

        safeListener<ClientChatReceivedEvent> {
            val rawMessage = removecolorcode(it.message.unformattedText)
            val patternedMessage = chatPattern.matcher(rawMessage)
            if (patternedMessage.find()) {
                val onlyMessage = patternedMessage.group(2)
                if (onlyMessage.length > 7) {
                    if (onlyMessage.slice(IntRange(0,5)) == "base64") {
                        if (patternedMessage.group(1) != mc.session.username) {
                            val encodedMessage = onlyMessage.slice(IntRange(6, onlyMessage.length - 1))
                            val decodedMessage = decode(encodedMessage)
                            if (decodedMessage == "") {
                                it.isCanceled = true
                                MessageSendHelper.sendErrorMessage("$chatName ${patternedMessage.group(1)} send non-valid base64 String.\nOriginal Message : $onlyMessage")
                            } else {
                                if (originalChat) MessageSendHelper.sendChatMessage("Original Message : " + it.message.formattedText)
                                it.message = TextComponentString("<${(patternedMessage.group(1))}> $decodedMessage")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun encode(message : String) : String {
        val encoder: Base64.Encoder = Base64.getEncoder()
        var encoded: String = encoder.encodeToString(message.toByteArray())
        if (encoded.length <= 250) {
            encoded = "base64$encoded"
        } else {
            encoded = ""
            MessageSendHelper.sendWarningMessage("Cancelling Message because the Encoded message's length is more than 250.")
        }
        return encoded
    }

    private fun decode(message: String) : String {
        val decoder: Base64.Decoder = Base64.getDecoder()
        try {
            return String(decoder.decode(message))
        } catch (e: IllegalArgumentException) {
            return ""
        } catch (e: Exception) {
            MessageSendHelper.sendErrorMessage("Error : $e")
            return ""
        }
    }

    private fun removecolorcode(message: String): String {
        val colorcode = arrayOf("§0","§1","§2","§3","§4","§5","§6","§7","§8","§9","§a","§b","§c","§d","§e","§f","§k","§l","§m","§n","§o","§r")
        var temp = message
        for (i in colorcode) {
            temp = temp.replace(i,"")
        }
        return temp
    }
}