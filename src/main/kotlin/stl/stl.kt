package stl

import Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import org.lwjgl.assimp.*
import org.lwjgl.system.MemoryStack

object StlReader {

    fun loadVerticesWithAssimp(path: String, deduplicate: Boolean = false): List<Vec3> {
        MemoryStack.stackPush().use {
            val flags = Assimp.aiProcess_Triangulate or
                    Assimp.aiProcess_JoinIdenticalVertices or
                    Assimp.aiProcess_ImproveCacheLocality or
                    Assimp.aiProcess_OptimizeMeshes or
                    Assimp.aiProcess_GenNormals // if missing

            val scene = Assimp.aiImportFile(path, flags)
                ?: error("Assimp failed: ${Assimp.aiGetErrorString()}")

            val meshes = scene.mMeshes() ?: return emptyList()
            val verts = ArrayList<Vec3>()

            for (i in 0 until scene.mNumMeshes()) {
                val mesh = AIMesh.create(meshes.get(i))
                val pos: AIVector3D.Buffer = mesh.mVertices() ?: continue
                for (v in 0 until mesh.mNumVertices()) {
                    val p = pos[v]
                    verts.add(Vec3(p.x().toDouble(), p.y().toDouble(), p.z().toDouble()))
                }
            }
            Assimp.aiReleaseImport(scene)
            return if (deduplicate) verts.distinct() else verts
        }
    }
}

fun main(args: Array<String>) {
    val vertices =
        if (args.isNotEmpty()) {
            // from disk
            StlReader.loadVerticesWithAssimp(args[0], deduplicate = true)
        } else {
            // from classpath resource bundled in the JAR
            StlReader.loadVerticesWithAssimp("models/cat.stl", deduplicate = true)
        }

    println("Read ${vertices.size} unique vertices")
    vertices.take(10).forEachIndexed { i, v -> println("#$i: (${v.x}, ${v.y}, ${v.z})") }
}