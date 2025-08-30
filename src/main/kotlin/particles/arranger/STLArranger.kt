package particles.arranger

import Vec3
import com.sun.scenario.effect.Offset
import particles.Particle
import stl.StlReader
import kotlin.math.PI

// Circle on the ground plane (Z fixed), evenly spaced
class STLArranger(
    private val modelName: String,
    private val scale: Double = 1.0,
    private val offset: Vec3,
    private val useDebugOffset: Boolean = false,
    private val shuffle: Boolean = true
) : ParticleArranger {
    override fun arrange(particles: MutableList<Particle>) {



        val vertices =   StlReader.loadVerticesWithAssimp("/Users/edipo/repo/enxame/build/resources/main/models/"+modelName, deduplicate = true).toMutableList()
    println("Read ${vertices.size} unique vertices")
    //vertices.take(particles.size) //todo remover isso
        if (shuffle) {
           vertices.shuffle()
        }

        val centerOfMass = vertices.reduce{acc, v -> acc + v} * (1/ vertices.size.toDouble())
        println("centerOfMass = $centerOfMass")

        for (i in 0 until particles.size) {
val debugOffset = if(useDebugOffset) State.offset else Vec3(0.0,0.0,00.0)
            val p = particles[i]

            val vertice = (vertices[i]- centerOfMass) * scale + offset + debugOffset


            p.position = vertice
      //      println("  p.position = ${p.position}")
            p.velocity = Vec3(0.0, 0.0, 0.0)
           // p.state = ParticleState.ACTIVE
        }
    }
}
/*
class CircleParticleCreator(
    private val center: Vec3,
    private val radius: Double,
    private val n: Int
) : ParticleCreator {
    override fun create(): List<Particle> {


        val dTheta = (2.0 * PI) / n
        for (i in 0 until n) {
            val theta = i * dTheta
            val x = center.x + radius * cos(theta)
            val y = center.y + radius * sin(theta)
            val z = center.z

            val p = particles[i]
            p.position = Vec3(x, y, z)
            p.velocity = Vec3(0.0, 0.0, 0.0)
            p.state = ParticleState.ACTIVE
        }

        return
    }
}*/