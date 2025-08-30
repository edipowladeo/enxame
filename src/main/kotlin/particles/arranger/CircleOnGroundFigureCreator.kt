package particles.arranger

import Vec3
import particles.Particle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CircleOnGroundFigureCreator(
    private val center: Vec3,
    private val radius: Double,
    private val count: Int,
 ) : FigureCreator {

    override fun create(): FigureOfParticles {
        val n = count.coerceAtLeast(1)
        val dTheta = (2.0 * PI) / n

        val particles = (0 until n).map { i ->
            val theta = i * dTheta
            val x = center.x + radius * cos(theta)
            val y = center.y + radius * sin(theta)
            val z = center.z

            val pos = Vec3(x, y, z)
            val vel = Vec3(0.0, 0.0, 0.0)

            Particle( pos, vel)
        }

        return FigureOfParticles(particles = particles)
    }
}