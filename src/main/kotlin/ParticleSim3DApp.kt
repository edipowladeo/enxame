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
import particles.arranger.ParallelogramLatticeFigureCreator
import particles.arranger.STLFigureCreator
import particles.morph.MorphAgent
import particles.morph.MorphPlan
import particles.simulation.MetallicSimulator
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
            particleCount,
            center = Vec3(0.0, 0.0, 30.0),
            separation = 1.5,
        ).create().particles

        val latticeFigure = ParallelogramLatticeFigureCreator(
            count = particleCount,
            center = Vec3(0.0, 0.0, 30.0),
            separation = Vec3(1.5, 1.5, 1.5)
        ).create().particles


        val catFigure = STLFigureCreator(
            "cat.stl",
            1.5,
            Vec3(00.0, 0.0, 30.0),
            limit = particleCount, //todo remove this
        ).create().particles

        val circleFigure = CircleOnGroundFigureCreator(
            center = Vec3(0.0, 00.0, 30.0),
            radius = 15.0,
            count = particleCount,
        ).create().particles

        val startFigure = pantherFigure
        val endFigure = catFigure

        val simParticles = startFigure.map { it.copy() }.toMutableList()



        val startParticles = startFigure.map { it.copy() }.toMutableList()
        val endParticles = endFigure.map { it.copy() }.toMutableList()

        val particles = simParticles// + startParticles + endParticles




        val physics = PhysicsParams(
            gravity = Vec3(0.0, 0.0, -1.7), // tweak for "balloon-like" descent
            dragCoeff = 03.5, floorZ = 0.0
        )

        val assign = minimalAssignmentByDistance(startParticles, endParticles)

// 2) Build agents using the optimal target for each i
        val morphAgents = simParticles.mapIndexed { i, body ->
            val j = assign[i]
            MorphAgent(
                body = body,
                plan = MorphPlan(
                    start = startParticles[i].position.copy(),
                    end   = endParticles[j].position.copy()
                )
            )
        }



        /*val sim = MetallicSimulator(
            agents = morphAgents,
            physics = PhysicsParams(dragCoeff = 1.0),
        )*/
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
            if (e.code == KeyCode.D) {
                AppState.drag = !AppState.drag
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
          //      val dt = ((now - lastNanos).toDouble() / 1e9).coerceIn(0.0, 1.0 / 15.0)
                lastNanos = now
val dt = 1.0 / 60.0
                if (AppState.simulate) sim.update(dt)//, AppState.drag)

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

// --- Minimal total-distance assignment for your existing types ---
// Pairs A[i] (startParticles[i]) to B[j] (endParticles[j]) minimizing sum of Euclidean distances.
// Returns assign[i] = j (column chosen for row i).
fun minimalAssignmentByDistance(
    A: List<Particle>,  // startParticles
    B: List<Particle>   // endParticles
): IntArray {
    require(A.size == B.size) { "Point sets must have same size." }
    val n = A.size
    if (n == 0) return IntArray(0)

    val cost = Array(n) { i ->
        DoubleArray(n) { j ->
            val dx = A[i].position.x - B[j].position.x
            val dy = A[i].position.y - B[j].position.y
            val dz = A[i].position.z - B[j].position.z
            dx*dx + dy*dy + dz*dz
        }
    }
    return hungarian(cost)
}

/**
 * Hungarian (Kuhn–Munkres) for square cost[n][n], minimization.
 * Returns assign[i] = j (column matched to row i).
 */
fun hungarian(cost: Array<DoubleArray>): IntArray {
    val n = cost.size
    require(n > 0 && cost.all { it.size == n }) { "Cost matrix must be square and non-empty." }

    // Potentials and matching (1-indexed internally)
    val u = DoubleArray(n + 1)
    val v = DoubleArray(n + 1)
    val p = IntArray(n + 1)
    val way = IntArray(n + 1)

    for (i in 1..n) {
        p[0] = i
        val minv = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
        val used = BooleanArray(n + 1)
        var j0 = 0
        do {
            used[j0] = true
            val i0 = p[j0]
            var delta = Double.POSITIVE_INFINITY
            var j1 = 0
            for (j in 1..n) {
                if (!used[j]) {
                    val cur = cost[i0 - 1][j - 1] - u[i0] - v[j]
                    if (cur < minv[j]) {
                        minv[j] = cur
                        way[j] = j0
                    }
                    if (minv[j] < delta) {
                        delta = minv[j]
                        j1 = j
                    }
                }
            }
            for (j in 0..n) {
                if (used[j]) {
                    u[p[j]] += delta
                    v[j] -= delta
                } else {
                    minv[j] -= delta
                }
            }
            j0 = j1
        } while (p[j0] != 0)

        // Augment
        do {
            val j1 = way[j0]
            p[j0] = p[j1]
            j0 = j1
        } while (j0 != 0)
    }

    // Convert to row->col mapping
    val assign = IntArray(n)
    for (j in 1..n) {
        if (p[j] != 0) assign[p[j] - 1] = j - 1
    }
    return assign
}
