package particles

import Vec3
import javafx.scene.paint.Color

data class Particle(
    var position: Vec3 = Vec3(0.0, 0.0, 0.0),
    var velocity: Vec3 = Vec3(0.0, 0.0, 0.0),
    var state: ParticleState = ParticleState.ACTIVE,
  //  var color: Color = Color.WHITE
)

