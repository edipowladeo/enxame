package particles.simulation

data class LJParams(
    val epsilon: Double = 1.0,     // well depth (interaction strength)
    val L0: Double = 1.0,          // distance where F = 0
    val cutoffSigma: Double = 1000.0, // cutoff radius in units of sigma (typical: 2.5)
    val softening: Double = 1e-9   // to avoid singularities at r≈0 (in absolute units)
)