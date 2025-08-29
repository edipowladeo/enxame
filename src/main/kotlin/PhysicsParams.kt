data class PhysicsParams(
    val gravity: Vec3 = Vec3(0.0, 0.0, -2.5), // gentle "balloon" fall (m/s^2)
    val dragCoeff: Double = 0.5,              // linear drag (1/s)
    val floorZ: Double = 0.0
)