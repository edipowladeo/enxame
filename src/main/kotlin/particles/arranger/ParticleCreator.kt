package particles.arranger

import particles.Particle

fun interface ParticleCreator{
    fun create() : FigureOfParticles
}

data class FigureOfParticles(
    val particles: List<Particle>
)