package particles.arranger

import Vec3
import particles.Particle
import stl.StlReader

class STLFigureCreator(
    private val stlPath: String,                         // e.g. "/.../models/myModel.stl"
    private val scale: Double = 1.0,
    private val offset: Vec3,
    private val shuffle: Boolean = true,
    private val limit: Int? = null,                      // cap the number of particles/vertices to use
) : FigureCreator {

    override fun create(): FigureOfParticles {
        val vertices = StlReader
            .loadVerticesWithAssimp("/Users/edipo/repo/enxame/build/resources/main/models/"+  stlPath, deduplicate = true)
            .toMutableList()

        require(vertices.isNotEmpty()) { "No vertices found in STL at $stlPath" }

        if (shuffle) vertices.shuffle()

        val centerOfMass = vertices.reduce { acc, v -> acc + v } * (1.0 / vertices.size.toDouble())

        val count = (limit ?: vertices.size).coerceAtMost(vertices.size)

        val particles: List<Particle> = (0 until count).map { i ->
            val pos = (vertices[i] - centerOfMass) * scale + offset
            val vel = Vec3(0.0, 0.0, 0.0)
            Particle(pos, vel)
        }

        return FigureOfParticles(particles = particles)
    }
}