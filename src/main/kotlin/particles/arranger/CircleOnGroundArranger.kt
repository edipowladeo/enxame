package particles.arranger

import Vec3
import particles.Particle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Circle on the ground plane (Z fixed), evenly spaced
class CircleOnGroundArranger(
    private val center: Vec3,
    private val radius: Double
) : ParticleArranger {
    override fun arrange(particles: MutableList<Particle>) {
        val n = particles.size.coerceAtLeast(1)
        val dTheta = (2.0 * PI) / n
        for (i in 0 until n) {
            val theta = i * dTheta
            val x = center.x + radius * cos(theta)
            val y = center.y + radius * sin(theta)
            val z = center.z
            val p = particles[i]
            p.position = Vec3(x, y, z)
            p.velocity = Vec3(0.0, 0.0, 0.0)
           // p.state = ParticleState.ACTIVE
        }
    }
}
/*
class CircleParticleCreator(
    private val center: Vec3,
    private val radius: Double,
    private val n: Int
) : ParticleCreator {
    override fun create(): List<Particle> {


        val dTheta = (2.0 * PI) / n
        for (i in 0 until n) {
            val theta = i * dTheta
            val x = center.x + radius * cos(theta)
            val y = center.y + radius * sin(theta)
            val z = center.z

            val p = particles[i]
            p.position = Vec3(x, y, z)
            p.velocity = Vec3(0.0, 0.0, 0.0)
            p.state = ParticleState.ACTIVE
        }

        return
    }
}*/