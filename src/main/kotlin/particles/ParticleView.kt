package particles

import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import javafx.scene.shape.Sphere

class ParticleView(
    val particle: Particle,
    radiusWorld: Double,
    val scale: Double,
    val activeMaterial: PhongMaterial,
    val haltedMaterial: PhongMaterial
) {
    val node = Sphere(radiusWorld * scale).apply {
        material = activeMaterial
        drawMode = DrawMode.FILL
    }

    fun syncToModel() {
        node.translateX = particle.position.x * scale
        node.translateY = -particle.position.z * scale
        node.translateZ = particle.position.y * scale
        node.material = if (particle.state == ParticleState.HALTED) haltedMaterial else activeMaterial
    }
}

