package dev.duv14.build

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.essential.gradle.util.relocate.KotlinMetadataRemappingClassVisitor
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Relocate both the runtime and every caller after Loom has remapped Minecraft. */
abstract class IsolateComposeRuntime : DefaultTask() {
    @get:InputFile abstract val inputJar: RegularFileProperty
    @get:OutputFile abstract val outputJar: RegularFileProperty

    private val prefixes = listOf("androidx/", "org/jetbrains/compose/")
    private fun mapPath(value: String): String = if (prefixes.any(value::startsWith))
        "dev/duv14/reconfig/shaded/$value" else value
    private fun mapText(value: String): String {
        if (prefixes.any(value::startsWith)) return mapPath(value)
        if (prefixes.any { value.startsWith(it.replace('/', '.')) })
            return "dev.duv14.reconfig.shaded.$value"
        if (prefixes.any { value.startsWith("/$it") }) return "/" + mapPath(value.substring(1))
        // Mixin @At targets are strings such as "Landroidx/...;method()V".
        // ASM maps class literals, but these member-selector descriptors need
        // explicit handling as well or the relocated mixin will fail at launch.
        return prefixes.fold(value) { text, prefix ->
            text.replace("L$prefix", "Ldev/duv14/reconfig/shaded/$prefix")
        }
    }
    private val mapper = object : Remapper() {
        override fun map(internalName: String): String = mapPath(internalName)
        override fun mapValue(value: Any?): Any? = if (value is String) mapText(value) else super.mapValue(value)
    }
    private fun entries(bytes: ByteArray): List<Pair<String, ByteArray>> {
        val result = ArrayList<Pair<String, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) result += entry.name to zip.readBytes()
            }
        }
        return result
    }
    private fun collectIds(bytes: ByteArray, ids: MutableMap<String, String>) {
        val files = entries(bytes)
        files.filter { it.first.endsWith(".jar") }.forEach { collectIds(it.second, ids) }
        val json = files.firstOrNull { it.first == "fabric.mod.json" } ?: return
        val mod = JsonParser.parseString(json.second.toString(Charsets.UTF_8)).asJsonObject
        val id = mod["id"].asString
        if (id == "compose-multiplatform" || files.any { (name, _) ->
                name.endsWith(".class") && prefixes.any(name::startsWith)
            }) {
            val hash = MessageDigest.getInstance("SHA-256").digest(id.toByteArray())
                .take(10).joinToString("") { "%02x".format(it) }
            ids[id] = "reconfig_rt_$hash"
        }
    }
    private fun metadata(element: JsonElement, ids: Map<String, String>): JsonElement {
        if (element.isJsonObject) {
            val result = JsonObject()
            element.asJsonObject.entrySet().forEach { (key, value) ->
                result.add(ids[key] ?: key, metadata(value, ids))
            }
            return result
        }
        if (element.isJsonArray) {
            val result = com.google.gson.JsonArray()
            element.asJsonArray.forEach { result.add(metadata(it, ids)) }
            return result
        }
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString)
            return com.google.gson.JsonPrimitive(ids[element.asString] ?: mapText(element.asString))
        return element
    }
    private fun rewrite(bytes: ByteArray, ids: Map<String, String>): ByteArray {
        val result = ByteArrayOutputStream()
        ZipOutputStream(result).use { output ->
            entries(bytes).forEach { (original, contents) ->
                // Archive signatures cannot describe rewritten bytecode. Licenses and
                // attribution resources are retained unchanged.
                if (original.startsWith("META-INF/") &&
                    listOf(".SF", ".RSA", ".DSA").any(original::endsWith)) return@forEach
                var name = mapPath(original)
                val data = when {
                    original.endsWith(".jar") -> rewrite(contents, ids)
                    original.endsWith(".class") -> {
                        val text = contents.toString(Charsets.ISO_8859_1)
                        if (listOf("androidx/", "androidx.", "org/jetbrains/compose/", "org.jetbrains.compose.")
                                .none(text::contains)) contents else {
                            val writer = ClassWriter(0)
                            ClassReader(contents).accept(
                                ClassRemapper(KotlinMetadataRemappingClassVisitor(mapper, writer), mapper), 0)
                            writer.toByteArray()
                        }
                    }
                    original == "fabric.mod.json" -> {
                        val source = JsonParser.parseString(contents.toString(Charsets.UTF_8)).asJsonObject
                        // Skiko contains JNI methods: moving its Java package alone
                        // breaks native lookup. Keep its shared namespace, but require
                        // the exact renderer ABI this private Compose was built with.
                        // Without this dependency a newer public Compose bundle could
                        // supply an incompatible RenderNodeContext constructor.
                        if (source["id"].asString == "compose-multiplatform") {
                            val depends = source.getAsJsonObject("depends") ?: JsonObject().also {
                                source.add("depends", it)
                            }
                            val skikoId = "org_jetbrains_skiko_skiko-awt"
                            val renderer = entries(bytes).first { (path, jar) ->
                                path.endsWith(".jar") && entries(jar)
                                    .any { it.first == "org/jetbrains/skiko/Library.class" }
                            }
                            val rendererMetadata = entries(renderer.second).first { it.first == "fabric.mod.json" }
                            val rendererVersion = JsonParser.parseString(rendererMetadata.second.toString(Charsets.UTF_8))
                                .asJsonObject["version"].asString
                            depends.addProperty(skikoId, "=$rendererVersion")
                        }
                        metadata(source, ids).toString().toByteArray(Charsets.UTF_8)
                    }
                    original.startsWith("META-INF/services/") -> {
                        name = "META-INF/services/" + mapText(original.removePrefix("META-INF/services/"))
                        contents.toString(Charsets.UTF_8).lineSequence().map(::mapText)
                            .joinToString("\n").toByteArray(Charsets.UTF_8)
                    }
                    else -> contents
                }
                output.putNextEntry(ZipEntry(name).apply { time = 0L })
                output.write(data)
                output.closeEntry()
            }
        }
        return result.toByteArray()
    }
    @TaskAction fun isolate() {
        val bytes = inputJar.get().asFile.readBytes()
        val ids = LinkedHashMap<String, String>()
        collectIds(bytes, ids)
        val target = outputJar.get().asFile
        target.parentFile.mkdirs()
        target.writeBytes(rewrite(bytes, ids))
        logger.lifecycle("Isolated ReConfig Compose runtime ({} private nested library IDs) into {}", ids.size, target)
    }
}
