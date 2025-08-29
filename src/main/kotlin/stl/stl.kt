package stl

import Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

object StlReader {

    /** Load from an absolute/relative file path on disk. */
    fun loadFromFile(path: Path, deduplicate: Boolean = false): List<Vec3> =
        readVertices(Files.readAllBytes(path), deduplicate)

    /** Convenience overload. */
    fun loadFromFile(pathStr: String, deduplicate: Boolean = false): List<Vec3> =
        loadFromFile(Path.of(pathStr), deduplicate)

    /** Load from a classpath resource (e.g., src/main/resources/models/part.stl -> "models/part.stl"). */
    fun loadFromResource(resourcePath: String, deduplicate: Boolean = false): List<Vec3> {
        val stream = Thread.currentThread().contextClassLoader
            .getResourceAsStream(resourcePath)
            ?: error("Resource not found on classpath: $resourcePath")
        val bytes = stream.use { it.readBytes() }
        return readVertices(bytes, deduplicate)
    }

    // --- Turn your existing reader into a bytes-based core ---
    fun readVertices(file: Path, deduplicate: Boolean = false): List<Vec3> =
        readVertices(Files.readAllBytes(file), deduplicate)

    /**
     * Reads all vertices from an STL file (binary or ASCII).
     * @param file path to the .stl
     * @param deduplicate if true, removes duplicate vertices while preserving order
     */
    fun readVertices(bytes: ByteArray, deduplicate: Boolean = false): List<Vec3> {
        val verts = try {
            if (isProbablyBinarySTL(bytes)) parseBinaryVertices(bytes) else parseAsciiVertices(bytes)
        } catch (_: Throwable) {
            // Fallback: try the other parser
            try {
                if (isProbablyBinarySTL(bytes)) parseAsciiVertices(bytes) else parseBinaryVertices(bytes)
            } catch (e: Throwable) {
                error("Could not parse STL as ASCII or binary: ${e.message}")
            }
        }
        return if (deduplicate) verts.distinct() else verts
    }
    // --- Format detection ---
    private fun isProbablyBinarySTL(bytes: ByteArray): Boolean {
        if (bytes.size < 84) return false
        val triCount = toUInt32LE(bytes, 80)
        val payload = bytes.size - 84
        val looksSized = payload >= 0 && payload % 50 == 0
        val headerFits = 84L + 50L * triCount <= bytes.size.toLong()
        // Many bad exporters put a wrong count; size divisibility is a stronger hint.
        if (looksSized || headerFits) return true

        // If it *starts* with "solid" and contains ASCII STL tokens, likely ASCII.
        val head = String(bytes, 0, minOf(bytes.size, 4096), Charsets.US_ASCII)
        val asciiish = head.startsWith("solid", ignoreCase = true) &&
                head.contains("facet", ignoreCase = true) &&
                head.contains("vertex", ignoreCase = true)
        return !asciiish
    }

    private fun toUInt32LE(bytes: ByteArray, offset: Int): Long {
        val b0 = bytes[offset].toLong() and 0xFF
        val b1 = bytes[offset + 1].toLong() and 0xFF
        val b2 = bytes[offset + 2].toLong() and 0xFF
        val b3 = bytes[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    // --- Binary STL parsing ---
    private fun parseBinaryVertices(bytes: ByteArray): List<Vec3> {
        if (bytes.size < 84) error("File too small to be a valid binary STL.")
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        bb.position(80)

        val headerCount = bb.int
        val payloadBytes = bytes.size - 84
        val maxBySize = if (payloadBytes >= 0) payloadBytes / 50 else 0
        val toRead = minOf(maxBySize, headerCount.coerceAtLeast(0))

        val verts = ArrayList<Vec3>(toRead * 3)
        for (i in 0 until toRead) {
            if (bb.remaining() < 50) break // extra guard; don't EOF
            // skip normal
            bb.float; bb.float; bb.float
            // 3 vertices
            repeat(3) {
                val x = bb.double
                val y = bb.double
                val z = bb.double
                verts.add(Vec3(x, y, z))
            }
            // attribute byte count
            bb.short
        }

        // Optional: warn if truncated or header was bogus
        if (headerCount > toRead) {
            System.err.println(
                "Warning: header triangles=$headerCount but bytes allow only $toRead; " +
                        "file may be truncated or has a bad count. Parsed $toRead triangles."
            )
        }
        return verts
    }

    // --- ASCII STL parsing ---
    private fun parseAsciiVertices(bytes: ByteArray): List<Vec3> {
        val text = bytes.toString(Charsets.US_ASCII)
        val verts = ArrayList<Vec3>()
        // Parse lines that start with "vertex x y z"
        text.lineSequence().forEachIndexed { idx, raw ->
            val line = raw.trim()
            if (line.startsWith("vertex", ignoreCase = true)) {
                val toks = line.split(Regex("\\s+"))
                if (toks.size >= 4) {
                    val x = toks[1].toDoubleOrNull()
                    val y = toks[2].toDoubleOrNull()
                    val z = toks[3].toDoubleOrNull()
                    if (x != null && y != null && z != null) {
                        verts.add(Vec3(x, y, z))
                    } else {
                        // Ignore malformed vertex line
                        // (Could throw if you prefer strict parsing)
                    }
                } else {
                    // Ignore malformed vertex line
                }
            }
        }
        if (verts.isEmpty()) {
            // If we got nothing, it's probably a binary STL with a "solid" header edge case.
            // Try binary as a fallback.
            return try {
                parseBinaryVertices(bytes)
            } catch (_: Throwable) {
                error("Could not parse STL as ASCII or binary; file may be corrupt.")
            }
        }
        return verts
    }
}

fun main(args: Array<String>) {
    val vertices =
        if (args.isNotEmpty()) {
            // from disk
            StlReader.loadFromFile(args[0], deduplicate = true)
        } else {
            // from classpath resource bundled in the JAR
            StlReader.loadFromResource("models/cat.stl", deduplicate = true)
        }

    println("Read ${vertices.size} unique vertices")
    vertices.take(10).forEachIndexed { i, v -> println("#$i: (${v.x}, ${v.y}, ${v.z})") }
}