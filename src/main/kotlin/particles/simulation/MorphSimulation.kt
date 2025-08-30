import particles.ParticleState
import particles.morph.MorphAgent
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MorphSimulation(
    val agents: List<MorphAgent>,
    private val physics: PhysicsParams
) {
    // Simple explicit Euler with clamped dt
    fun update(dtSeconds: Double) {
        val dt = dtSeconds.coerceIn(0.0, 1.0 / 30.0) // avoid big steps on frame stalls
        for (agent in agents) {
            val particle = agent.body

            if (particle.state == ParticleState.HALTED) continue

            val goalForce = goalForce(agent)
            val repulsionForce = repulsionForce(agent, agents)

            val ax = goalForce.x + repulsionForce.x - physics.dragCoeff * particle.velocity.x // + physics.gravity.x
            val ay = goalForce.y + repulsionForce.y - physics.dragCoeff * particle.velocity.y // + physics.gravity.y
            val az = goalForce.z + repulsionForce.z - physics.dragCoeff * particle.velocity.z // + physics.gravity.z

            particle.velocity.x += ax * dt
            particle.velocity.y += ay * dt
            particle.velocity.z += az * dt

            particle.position.x += particle.velocity.x * dt
            particle.position.y += particle.velocity.y * dt
            particle.position.z += particle.velocity.z * dt

            // Ground collision (Z <= 0): stick & halt
            if (particle.position.z <= physics.floorZ) {
                particle.position.z = physics.floorZ
                particle.velocity = Vec3(0.0, 0.0, 0.0)
                particle.state = ParticleState.HALTED
            }
        }
    }

    fun goalForce(agent: MorphAgent): Vec3 {
       // return Vec3(0.0,0.0,0.0)
        val toGoal = agent.goalVector()
        val elasticity = 5.0 // tweak this to change "springiness"
        val goalForce = Vec3(
            toGoal.x * elasticity,
            toGoal.y * elasticity,
            toGoal.z * elasticity
        )
        val forceAbs = goalForce.length()
        val maxForce = 4.0 // prevent extreme forces
        if (forceAbs > maxForce) {
            goalForce.scaleInPlace(maxForce / forceAbs)
        }
        return goalForce

    }

    fun repulsionForce(agent: MorphAgent, agents: List<MorphAgent>): Vec3 {
        val total = Vec3(0.0, 0.0, 0.0)
        for (other in agents) {
            if (other === agent) continue
            val f = repulsionForceBetween(agent, other)
            total.x += f.x
            total.y += f.y
            total.z += f.z
        }

        // Optional: clamp the total to avoid huge spikes
        val maxForce = 100.0
        val len = total.length()
        if (len > maxForce) total.scaleInPlace(maxForce / len)

        return total
    }

    fun repulsionForceBetween(
        a: MorphAgent,
        b: MorphAgent,
        dMax: Double = 0.3,
        strength: Double = 10.0
    ): Vec3 {
        val delta = Vec3(
            a.body.position.x - b.body.position.x,
            a.body.position.y - b.body.position.y,
            a.body.position.z - b.body.position.z
        )
        val dist = delta.length()

        // No force if outside radius
        if (dist >= dMax) return Vec3(0.0, 0.0, 0.0)

        // Linear (triangle) falloff: F(0)=strength, F(dMax)=0
        val mag = strength * ((dMax - dist) / dMax).coerceIn(0.0, 1.0)

        // Normalize delta and scale by magnitude.
        // If agents are exactly coincident, pick a fixed direction.
        val force =
            if (dist > 1e-6) delta * (mag / dist)
            else Vec3(Random.nextDouble(0.1), Random.nextDouble(0.1),Random.nextDouble(0.1)) * mag   // arbitrary unit direction at d≈0

        // Debug (optional)
        // println("distance: $dist mag: $mag force: $force")

        return force
    }

}