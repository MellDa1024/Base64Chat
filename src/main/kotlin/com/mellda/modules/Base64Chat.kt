package com.mellda.modules

import com.lambda.client.LambdaMod
import com.lambda.client.manager.managers.FriendManager
import com.lambda.client.manager.managers.MessageManager.newMessageModifier
import com.mellda.ChatPlugin
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.color.EnumTextColor
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
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
    description = "Chat with base64!",
    pluginMain = ChatPlugin
) {
    private val encode by setting("Encode", true)
    private val encodeWithGreenChat by setting("Enable Green Chat", true, { encode }, description = "Move > to prefix when player try to use Green Chat, chat length limit will decrease by 2.")
    private val decode by setting("Decode", true)
    private val decodeOnlyFriend by setting("Decode Only Friend", false, { decode }, description = "Only decode friend's message.")
    private val twob2tMode by setting("2B2T Mode", true, description = "Make chat length limit to 144.")
    private val originalChat by setting("Print Original Chat", true)
    private val disableOnCommand by setting("Disable on Command", true, description = "Stops encoding when you are using command.")
    private val chatColor by setting("Chat Color", EnumTextColor.WHITE, description = "Highlight Message(Original Message when send / Decoded Message when received) with color.")
    private val overwriteGreenChat by setting("Overwrite Green Chat", false, { chatColor != EnumTextColor.GREEN } ,description = "Overwrite Green chat to Chat Color.")

    private val chatPattern = Pattern.compile("<([0-9a-zA-Z_]+)> (b64.*)")
    private val greenChatPattern = Pattern.compile("<([0-9a-zA-Z_]+)> (> b64.*)")
    private val colorChatPattern = Pattern.compile("<([0-9a-zA-Z_§]+)> (b64.*)")
    private val greenColorChatPattern = Pattern.compile("<([0-9a-zA-Z_§]+)> (> b64.*)")

    private val modifier = newMessageModifier(
        filter = {
            (disableOnCommand && !it.packet.message.startsWith("/")) || !disableOnCommand
        },
        modifier = {
            if (encode && originalChat) MessageSendHelper.sendChatMessage("Original Message : ${chatColor}${it.packet.message}")
            val message = if (encode) {
                encode(it.packet.message)
            } else { it.packet.message }
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
            if (!decode) return@safeListener
            val rawMessage = removeColorCode(it.message.unformattedText)
            val patternedMessage = chatPattern.matcher(rawMessage)
            val patternedMessageGreen = greenChatPattern.matcher(rawMessage)
            val isGreenChat = patternedMessageGreen.find()
            if (patternedMessage.find() || isGreenChat) {
                val messagePair = if (isGreenChat) {
                    Pair(greenColorChatPattern.matcher(it.message.unformattedText), patternedMessageGreen)
                } else {
                    Pair(colorChatPattern.matcher(it.message.unformattedText), patternedMessage)
                }
                val playerName = if (messagePair.first.find()) { messagePair.first.group(1)
                } else { messagePair.second.group(1) }
                LambdaMod.LOG.info(playerName)
                val onlyMessage = messagePair.second.group(2)
                if (removeColorCode(playerName) != mc.session.username) {
                    if (decodeOnlyFriend && !FriendManager.isFriend(removeColorCode(playerName))) return@safeListener
                    val decodedMessage = if (isGreenChat) {
                        decode(onlyMessage.slice(IntRange(5, onlyMessage.length - 1)))
                    } else {
                        decode(onlyMessage.slice(IntRange(3, onlyMessage.length - 1)))
                    }
                    if (decodedMessage == "") {
                        it.isCanceled = true
                        MessageSendHelper.sendErrorMessage("$chatName $playerName§r send non-valid base64 String.\nOriginal Message : ${returnColor(isGreenChat, true)}$onlyMessage")
                    } else {
                        if (originalChat) MessageSendHelper.sendChatMessage("Original Message : " + it.message.formattedText)
                        it.message = TextComponentString("<${playerName}§r> ${returnColor(isGreenChat)}$decodedMessage")
                    }
                }
            }
        }
    }

    private fun returnColor(isGreenChat : Boolean, blank : Boolean = false) : String {
        return if (blank) {
            if (isGreenChat) { "§a" } else { "" }
        } else {
            if (isGreenChat) { if (overwriteGreenChat) { "$chatColor> " } else { "§a> " } }
            else { "$chatColor" }
        }
    }
    private fun encode(message : String) : String {
        val encoder: Base64.Encoder = Base64.getEncoder()
        var maxlength = if (twob2tMode) { 144 } else { 255 }
        var shouldEncodeWithGreen = false
        if (message.length > 2) { shouldEncodeWithGreen = (message.slice(IntRange(0, 1)) == "> " && encodeWithGreenChat) }
        val modifiedMessage = if (shouldEncodeWithGreen) { message.slice(IntRange(2, message.length-1)) } else { message }
        var encoded: String = encoder.encodeToString(modifiedMessage.toByteArray())
        encoded = "b64$encoded"
        if (shouldEncodeWithGreen) encoded = "> $encoded"
        if (encoded.length > maxlength) {
            encoded = ""
            if (shouldEncodeWithGreen) maxlength -= 2
            MessageSendHelper.sendWarningMessage("Cancelling Message because the Encoded message's length is longer than ${maxlength - 3}.")
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

    private fun removeColorCode(message: String): String {
        val colorcode = arrayOf("§0","§1","§2","§3","§4","§5","§6","§7","§8","§9","§a","§b","§c","§d","§e","§f","§k","§l","§m","§n","§o","§r")
        var temp = message
        for (i in colorcode) {
            temp = temp.replace(i,"")
        }
        return temp
    }
}