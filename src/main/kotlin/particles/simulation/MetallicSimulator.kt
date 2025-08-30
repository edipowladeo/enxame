package particles.simulation

import PhysicsParams
import Vec3
import particles.ParticleState
import particles.morph.MorphAgent
import kotlin.math.max
import kotlin.math.pow

class MetallicSimulator(
    val agents: List<MorphAgent>,
    private val physics: PhysicsParams,
    private val lj: LJParams = LJParams()
) {
    // Choose sigma so that F(r) = 0 at r = L0.
    private val sigma: Double = lj.L0 / 2.0.pow(1.0 / 6.0)
    private val sigma2: Double = sigma * sigma
    private val rc: Double = lj.cutoffSigma * sigma
    private val rc2: Double = rc * rc
    private val soft2: Double = lj.softening * lj.softening

    // Simple explicit Euler with clamped dt
    fun update(dtSeconds: Double, drag: Boolean) {
        val dt = dtSeconds.coerceIn(0.0, 1.0 / 30.0) // avoid big steps on stalls
        val n = agents.size

        // Accumulate internal forces (pairwise; O(N^2))
        val forces = Array(n) { Vec3(0.0, 0.0, 0.0) }

        for (i in 0 until n - 1) {
            val pi = agents[i].body
            for (j in i + 1 until n) {
                val pj = agents[j].body

                // Relative vector r = ri - rj
                val dx = pi.position.x - pj.position.x
                val dy = pi.position.y - pj.position.y
                val dz = pi.position.z - pj.position.z
                val r2raw = dx * dx + dy * dy + dz * dz
                if (r2raw > rc2) continue // beyond cutoff → ignore

                // Softened r^2 to prevent blowups at r≈0
                val r2 = max(r2raw, soft2)

                // Compute LJ force coefficient without sqrt:
                // F_vec = 24*epsilon * inv_r2 * (2*(sigma/r)^12 - (sigma/r)^6) * r_vec
                val inv_r2 = 1.0 / r2
                val sr2 = sigma2 * inv_r2          // (sigma/r)^2
                val sr6 = sr2 * sr2 * sr2          // (sigma/r)^6
                val sr12 = sr6 * sr6               // (sigma/r)^12
                val coef = 24.0 * lj.epsilon * inv_r2 * (2.0 * sr12 - sr6)

                val fx = coef * dx
                val fy = coef * dy
                val fz = coef * dz

                // Symmetric action-reaction
                forces[i].x += fx; forces[i].y += fy; forces[i].z += fz
                forces[j].x -= fx; forces[j].y -= fy; forces[j].z -= fz
            }
        }

        // Integrate with drag and (optional) gravity/floor like your current sim
        for (k in 0 until n) {
            val p = agents[k].body
            if (p.state == ParticleState.HALTED) continue

            // Linear drag
            if (drag) {
                forces[k].x += -physics.dragCoeff * p.velocity.x
                forces[k].y += -physics.dragCoeff * p.velocity.y
                forces[k].z += -physics.dragCoeff * p.velocity.z
            } else{
                forces[k].x += -physics.dragCoeff * p.velocity.x * 0.01
                forces[k].y += -physics.dragCoeff * p.velocity.y * 0.01
                forces[k].z += -physics.dragCoeff * p.velocity.z  * 0.01
            }
            // Uncomment if you want gravity:
            // forces[k].x += physics.gravity.x
            // forces[k].y += physics.gravity.y
            // forces[k].z += physics.gravity.z

            // Assume unit mass; otherwise divide by mass here
            p.velocity.x += forces[k].x * dt
            p.velocity.y += forces[k].y * dt
            p.velocity.z += forces[k].z * dt

            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.position.z += p.velocity.z * dt

            // Ground collision (stick & halt), like your original
            if (p.position.z <= physics.floorZ) {
                p.position.z = physics.floorZ
                p.velocity = Vec3(0.0, 0.0, 0.0)
                p.state = ParticleState.HALTED
            }
        }
    }
}

