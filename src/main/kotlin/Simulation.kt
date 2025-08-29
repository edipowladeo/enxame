import particles.Particle
import particles.ParticleState

class Simulation(
    val particles: MutableList<Particle>,
    private val physics: PhysicsParams
) {
    // Simple explicit Euler with clamped dt
    fun update(dtSeconds: Double) {
        val dt = dtSeconds.coerceIn(0.0, 1.0 / 30.0) // avoid big steps on frame stalls
        for (p in particles) {
            if (p.state == ParticleState.HALTED) continue

            // a = g - c * v
            val ax = physics.gravity.x - physics.dragCoeff * p.velocity.x
            val ay = physics.gravity.y - physics.dragCoeff * p.velocity.y
            val az = physics.gravity.z - physics.dragCoeff * p.velocity.z

            p.velocity.x += ax * dt
            p.velocity.y += ay * dt
            p.velocity.z += az * dt

            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.position.z += p.velocity.z * dt

            // Ground collision (Z <= 0): stick & halt
            if (p.position.z <= physics.floorZ) {
                p.position.z = physics.floorZ
                p.velocity = Vec3(0.0, 0.0, 0.0)
                p.state = ParticleState.HALTED
            }
        }
    }
}
class MorphSimulation(
    val particles: MutableList<Particle>,
    private val physics: PhysicsParams
) {
    // Simple explicit Euler with clamped dt
    fun update(dtSeconds: Double) {
        val dt = dtSeconds.coerceIn(0.0, 1.0 / 30.0) // avoid big steps on frame stalls
        for (p in particles) {
            if (p.state == ParticleState.HALTED) continue

            // a = g - c * v
            val ax = physics.gravity.x - physics.dragCoeff * p.velocity.x
            val ay = physics.gravity.y - physics.dragCoeff * p.velocity.y
            val az = physics.gravity.z - physics.dragCoeff * p.velocity.z

            p.velocity.x += ax * dt
            p.velocity.y += ay * dt
            p.velocity.z += az * dt

            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.position.z += p.velocity.z * dt

            // Ground collision (Z <= 0): stick & halt
            if (p.position.z <= physics.floorZ) {
                p.position.z = physics.floorZ
                p.velocity = Vec3(0.0, 0.0, 0.0)
                p.state = ParticleState.HALTED
            }
        }
    }
}