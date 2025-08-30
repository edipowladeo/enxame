package particles.arranger

import Vec3
import particles.Particle
import kotlin.math.ceil
import kotlin.math.sqrt

class GridOnGroundArranger(
    private val center: Vec3,
    private val separation: Double,
    private val preferredCols: Int? = null // optional: force a specific number of columns
) : ParticleArranger {
    override fun arrange(particles: MutableList<Particle>) {
        val n = particles.size
        if (n == 0) return

        val cols = (preferredCols?.coerceAtLeast(1)) ?: ceil(sqrt(n.toDouble())).toInt()
        val rows = ((n - 1) / cols) + 1

        // Total span so we can center the grid around `center`
        val width = (cols - 1) * separation
        val height = (rows - 1) * separation

        for (i in 0 until n) {
            val r = i / cols
            val c = i % cols

            val x = center.x - width / 2.0 + c * separation
            val y = center.y - height / 2.0 + r * separation
            val z = center.z

            val p = particles[i]
            p.position = Vec3(x, y, z)
            p.velocity = Vec3(0.0, 0.0, 0.0)
            // p.state = ParticleState.ACTIVE
        }
    }
}