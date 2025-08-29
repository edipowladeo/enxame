package particles.morph

import Vec3
import particles.Particle

data class MorphAgent(
    val body: Particle,   // your existing simulation state (pos/vel/halted)
    val plan: MorphPlan
){
    fun goalVector(): Vec3 {
        return Vec3(
            plan.end.x - body.position.x,
            plan.end.y - body.position.y,
            plan.end.z - body.position.z
        )
    }
}