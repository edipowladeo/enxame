package particles.arranger

import Vec3
import particles.Particle
import kotlin.math.ceil
import kotlin.math.sqrt

class GridOnGroundFigureCreator(
    private val count: Int,
    private val center: Vec3,
    private val separation: Double,
    private val preferredCols: Int? = null,
) : FigureCreator {

    override fun create(): FigureOfParticles {
        if (count <= 0) return FigureOfParticles(emptyList())

        val cols = (preferredCols?.coerceAtLeast(1))
            ?: ceil(sqrt(count.toDouble())).toInt()
        val rows = ((count - 1) / cols) + 1

        val width = (cols - 1) * separation
        val height = (rows - 1) * separation

        val particles = (0 until count).map { i ->
            val r = i / cols
            val c = i % cols

            val x = center.x - width / 2.0 + c * separation
            val y = center.y - height / 2.0 + r * separation
            val z = center.z

            val pos = Vec3(x, y, z)
            val vel = Vec3(0.0, 0.0, 0.0)

            Particle(pos, vel)
        }

        return FigureOfParticles(particles = particles)
    }
}