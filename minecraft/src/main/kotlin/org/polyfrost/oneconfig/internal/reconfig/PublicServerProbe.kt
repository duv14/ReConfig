package org.polyfrost.oneconfig.internal.reconfig

import com.google.gson.JsonParser
import net.minecraft.client.multiplayer.resolver.ServerAddress
import org.polyfrost.oneconfig.internal.ui.screens.CommunityServerStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import javax.imageio.ImageIO

/** Bounded, standard status handshake. Only addresses chosen from the directory are queried. */
object PublicServerProbe {
    fun query(address: String): CommunityServerStatus = runCatching {
        val target = ServerAddress.parseString(address)
        Socket().use { socket ->
            socket.connect(InetSocketAddress(target.host, target.port), 2500)
            socket.soTimeout = 2500
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            fun packet(body: ByteArray) { writeVarInt(output, body.size); output.write(body); output.flush() }
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).use { handshake ->
                writeVarInt(handshake, 0)
                writeVarInt(handshake, -1)
                val host = target.host.toByteArray(Charsets.UTF_8)
                require(host.size <= 255)
                writeVarInt(handshake, host.size)
                handshake.write(host)
                handshake.writeShort(target.port)
                writeVarInt(handshake, 1)
            }
            packet(bytes.toByteArray())
            val started = System.nanoTime()
            packet(byteArrayOf(0))
            val size = readVarInt(input)
            require(size in 1..262144)
            val response = ByteArray(size)
            input.readFully(response)
            val elapsed = (System.nanoTime() - started) / 1_000_000
            val frame = DataInputStream(ByteArrayInputStream(response))
            require(readVarInt(frame) == 0)
            val textSize = readVarInt(frame)
            require(textSize in 1..frame.available())
            val json = JsonParser.parseString(String(frame.readNBytes(textSize), Charsets.UTF_8)).asJsonObject
            val players = json.getAsJsonObject("players")?.get("online")?.asInt?.coerceAtLeast(0)
            val icon = json.get("favicon")?.asString?.takeIf { it.startsWith("data:image/png;base64,") && it.length < 90000 }
                ?.substringAfter(',')?.let { Base64.getDecoder().decode(it) }?.takeIf(::validIcon)
            CommunityServerStatus(elapsed, players, icon)
        }
    }.getOrDefault(CommunityServerStatus())

    private fun validIcon(bytes: ByteArray): Boolean = runCatching {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            if (!readers.hasNext()) return@use false
            val reader = readers.next()
            try {
                reader.input = stream
                reader.formatName.equals("png", true) && reader.getWidth(0) == 64 && reader.getHeight(0) == 64
            } finally { reader.dispose() }
        }
    }.getOrDefault(false)

    private fun writeVarInt(output: DataOutputStream, number: Int) {
        var value = number
        do {
            var byte = value and 127
            value = value ushr 7
            if (value != 0) byte = byte or 128
            output.writeByte(byte)
        } while (value != 0)
    }

    private fun readVarInt(input: DataInputStream): Int {
        var value = 0
        for (shift in 0..28 step 7) {
            val byte = input.readUnsignedByte()
            value = value or ((byte and 127) shl shift)
            if (byte and 128 == 0) return value
        }
        error("Invalid status response")
    }
}
