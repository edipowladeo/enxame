package particles.arranger

import particles.Particle

// Pluggable arrangement API
fun interface ParticleArranger {
    fun arrange(particles: MutableList<Particle>)
}

fun interface ParticleCreator{
    fun create() : List<Particle>
}