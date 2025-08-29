package particles.morph

import Vec3

data class MorphPlan(
    val start: Vec3,   // where it began (useful for reset/metrics)
    var end: Vec3      // desired target (can be swapped on the fly)
)