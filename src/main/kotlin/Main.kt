import javafx.application.Application


// ---------- JavaFX View (mapping physics Z↑ to screen Y↓) ----------
//
// JavaFX coords: +X right, +Y down, +Z toward camera by default.
// We use physics Z as "up". We'll map:
//   viewX = physics.x * scale
//   viewY = -physics.z * scale    (invert so higher Z is lower Y value)
//   viewZ = physics.y * scale

// ---------- App ----------

fun main() {
    Application.launch(ParticleSim3DApp::class.java)
}

