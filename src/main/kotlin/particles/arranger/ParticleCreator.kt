package particles.arranger

import particles.Particle

fun interface ParticleCreator{
    fun create() : List<Particle>
}