import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.geometry.Point3D
import javafx.scene.AmbientLight
import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.PointLight
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.SubScene
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.transform.Rotate
import javafx.stage.Stage
import particles.Particle
import particles.ParticleView
import particles.arranger.CircleOnGroundFigureCreator
import particles.arranger.GridOnGroundFigureCreator
import particles.arranger.STLFigureCreator
import particles.morph.MorphAgent
import particles.morph.MorphPlan
import kotlin.math.sqrt

class ParticleSim3DApp : Application() {

    override fun start(stage: Stage) {
        val worldScale = 20.0           // world meters -> pixels
        val particleRadius = 0.15       // meters
        val particleCount = 1000  //cat.stl tem 1003 vertices

        val pantherFigure = STLFigureCreator(
            "panther.stl",
            0.25,
            Vec3(0.0, 0.0, 30.0),
            limit = particleCount,
        ).create().particles

        val gridFigure = GridOnGroundFigureCreator(
            1000,
            center = Vec3(0.0, 0.0, 30.0),
            separation = 1.5,
        ).create().particles


        val catFigure = STLFigureCreator(
            "cat.stl",
            1.5,
            Vec3(0.0, 0.0, 30.0),
            limit = particleCount, //todo remove this
        ).create().particles

        val circleFigure = CircleOnGroundFigureCreator(
            center = Vec3(0.0, 0.0, 30.0),
            radius = 15.0,
            count = particleCount,
        ).create().particles

        val startFigure = pantherFigure
        val endFigure = catFigure

        val simParticles = startFigure.map { it.copy() }.toMutableList()

        val particles = simParticles //+ startParticles + endParticles

        val startParticles = startFigure.map { it.copy() }.toMutableList()
        val endParticles = endFigure.map { it.copy() }.toMutableList()

        for (i in 0 until particleCount) {
            simParticles[i].position = startParticles[i].position.copy()
            simParticles[i].velocity = Vec3(0.0, 0.0, 0.0)
        }


        val physics = PhysicsParams(
            gravity = Vec3(0.0, 0.0, -1.7), // tweak for "balloon-like" descent
            dragCoeff = 03.5, floorZ = 0.0
        )

        endParticles.shuffle()

        val morphAgents = simParticles.mapIndexed { i, it ->

            //todo this is a mess, refactor
            val endParticle = endParticles[i]
            val particle = it
            val startParticle = startParticles[i]

            MorphAgent(
                body = particle, plan = MorphPlan(
                    start = startParticle.position, end = endParticle.position
                    //  end = it.position.copy().also { it.z += 8 }, // will set later
                    //todo end is a particle?
                )
            )
        }

        val sim = MorphSimulation(morphAgents, physics)
        // val sim = Simulation(morphAgents.map { it.body }.toMutableList(), physics)

        // Materials
        val activeMat = PhongMaterial(Color.CORNFLOWERBLUE)
        val haltedMat = PhongMaterial(Color.FIREBRICK)

        // Ground (a wide, thin box at Z=0)
        val ground = Box(2000.0, 2.0, 2000.0).apply {
            material = PhongMaterial(Color.rgb(60, 60, 60))
            // Position ground so its *top* sits at physics Z=0 -> viewY=0
            // Box centered at its Y; so lift it up by half height
            translateY = 1.0 // half of height (2.0 / 2) -> top ~ 0
        }

        // Build particle views
        val views = particles.map {
            ParticleView(
                particle = it,
                radiusWorld = particleRadius,
                scale = worldScale,
                activeMaterial = activeMat,
                haltedMaterial = haltedMat
            )
        }

        val world = Group().apply {
            children.add(ground)
            children.addAll(views.map { it.node })
            // Some ambient + directional light
            children.add(AmbientLight(Color.color(0.35, 0.35, 0.35)))
            children.add(PointLight(Color.WHITE).apply {
                translateX = -300.0; translateY = -600.0; translateZ = -300.0
            })
        }

        // Camera setup (orbit a bit to show depth)
        val camera = PerspectiveCamera(true).apply {
            nearClip = 0.1
            farClip = 10000.0
            fieldOfView = 55.0
        }


        val subScene = SubScene(world, 1280.0, 800.0, true, SceneAntialiasing.BALANCED).apply {
            fill = Color.rgb(18, 18, 18)
            this.camera = camera
        }
        val root = Group(subScene)
        val scene = Scene(root, 1280.0, 800.0, true)
        stage.title = "Kotlin 3D Particles — Gravity + Drag (Z-up)"
        stage.scene = scene
        stage.show()

// Rig/orbit: câmera controlada por mouse
        val rig = OrbitCameraRig(subScene, camera)

// ATENÇÃO: o rig precisa estar no grafo do SubScene
        (world.children).add(rig.pivot)

        // Keep subscene sized to window
        scene.widthProperty().addListener { _, _, w -> subScene.width = w.toDouble() }
        scene.heightProperty().addListener { _, _, h -> subScene.height = h.toDouble() }

        val (centerView, radiusView) = computeBoundingSphere(particles, worldScale)
        rig.center(centerView, radiusView)

        // Re-enquadra se a janela/subscene mudar de tamanho
        scene.widthProperty().addListener { _, _, w ->
            subScene.width = w.toDouble()
            val (_, r) = computeBoundingSphere(particles, worldScale)
            rig.frame(r, subScene.width, subScene.height)
        }
        scene.heightProperty().addListener { _, _, h ->
            subScene.height = h.toDouble()
            val (_, r) = computeBoundingSphere(particles, worldScale)
            rig.frame(r, subScene.width, subScene.height)
        }

// Tecla F: re-enquadra manualmente
        scene.setOnKeyPressed { e ->
            if (e.code == KeyCode.F) {
                AppState.autoCenter = !AppState.autoCenter
            }
            if (e.code == KeyCode.S) {
                AppState.showStart = !AppState.showStart
            }
            if (e.code == KeyCode.E) {
                AppState.showEnd = !AppState.showEnd
            }
            if (e.code == KeyCode.SPACE) {
                AppState.simulate = !AppState.simulate
            }

            if (e.code == KeyCode.UP) {
                AppState.offset.x += 1.0
                println("offset: ${AppState.offset.x}, ${AppState.offset.y}, ${AppState.offset.z}")
            }
            if (e.code == KeyCode.DOWN) {
                AppState.offset.x -= 1.0

                println("offset: ${AppState.offset.x}, ${AppState.offset.y}, ${AppState.offset.z}")
            }
            if (e.code == KeyCode.LEFT) {
                AppState.offset.y -= 1.0
                println("offset: ${AppState.offset.x}, ${AppState.offset.y}, ${AppState.offset.z}")

            }
            if (e.code == KeyCode.RIGHT) {
                AppState.offset.y += 1.0

                println("offset: ${AppState.offset.x}, ${AppState.offset.y}, ${AppState.offset.z}")
            }
        }

        // Animation loop
        var lastNanos = System.nanoTime()
        object : AnimationTimer() {
            override fun handle(now: Long) {
                val dt = ((now - lastNanos).toDouble() / 1e9).coerceIn(0.0, 1.0 / 15.0)
                lastNanos = now

                if (AppState.simulate) sim.update(dt)

                //center
                if (AppState.autoCenter) {
                    val (c, r) = computeBoundingSphere(particles, worldScale)
                    rig.center(c, r)
                }

                views.forEach { it.syncToModel() }
            }
        }.start()
    }

    // Small helper to attach multiple rotates to a group cleanly
    private fun Group.rotates(vararg r: Rotate) {
        this.transforms.addAll(r)
    }
}


private fun computeBoundingSphere(ps: List<Particle>, scale: Double): Pair<Point3D, Double> {
    if (ps.isEmpty()) return Point3D(0.0, 0.0, 0.0) to 1.0

    val pts = ps.map { physicsToView(it.position, scale) }
    val cx = pts.map { it.x }.average()
    val cy = pts.map { it.y }.average()
    val cz = pts.map { it.z }.average()
    val center = Point3D(cx, cy, cz)

    var r = 1.0
    for (pt in pts) {
        val dx = pt.x - cx
        val dy = pt.y - cy
        val dz = pt.z - cz
        val d = sqrt(dx * dx + dy * dy + dz * dz)
        if (d > r) r = d
    }
    return center to r
}

private fun physicsToView(p: Vec3, scale: Double): Point3D =
    Point3D(p.x * scale, -p.z * scale, p.y * scale) // (X, -Z, Y) conforme seu mapeamento
