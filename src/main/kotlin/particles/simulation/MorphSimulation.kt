import particles.ParticleState
import particles.morph.MorphAgent

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


            val ax = goalForce.x  - physics.dragCoeff * particle.velocity.x // + physics.gravity.x
            val ay = goalForce.y  - physics.dragCoeff * particle.velocity.y // + physics.gravity.y
            val az = goalForce.z  - physics.dragCoeff * particle.velocity.z // + physics.gravity.z

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
        val toGoal = agent.goalVector()
        val elasticity = 5.0 // tweak this to change "springiness"
        val goalForce = Vec3(
            toGoal.x * elasticity,
            toGoal.y * elasticity,
            toGoal.z * elasticitymorph works
        )
        val forceAbs = goalForce.length()
        val maxForce = 5.0 // prevent extreme forces
        if (forceAbs > maxForce) {
            goalForce.scaleInPlace(maxForce / forceAbs)
        }
        println("goalForce: Before $forceAbs after ${goalForce.length()}")
        return goalForce

    }
}