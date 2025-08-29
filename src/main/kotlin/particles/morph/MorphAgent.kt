package particles.morph

import particles.Particle

data class MorphAgent(
    val body: Particle,   // your existing simulation state (pos/vel/halted)
    val plan: MorphPlan
)