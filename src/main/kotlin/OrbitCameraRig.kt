import javafx.geometry.Point3D
import javafx.scene.Group
import javafx.scene.PerspectiveCamera
import javafx.scene.SubScene
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.transform.Rotate
import javafx.scene.transform.Translate
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.tan

class OrbitCameraRig(
    private val subScene: SubScene,
    private val camera: PerspectiveCamera
) {
    val pivot = Group() // ponto de interesse (target) em coordenadas de cena

    private val yawRotate = Rotate(0.0, Rotate.Y_AXIS)
    private val pitchRotate = Rotate(-20.0, Rotate.X_AXIS)
    private val camTranslate = Translate(0.0, 0.0, -400.0)

    var yaw = 0.0; private set
    var pitch = -20.0; private set
    var distance = 400.0; private set

    private var lastX = 0.0
    private var lastY = 0.0
    private var draggingPrimary = false

    init {
        // Estrutura: pivot -> rot(yaw) -> rot(pitch) -> translate(-dist) -> camera
        pivot.transforms.addAll(yawRotate, pitchRotate)
        val holder = Group(camera).apply { transforms.add(camTranslate) }
        pivot.children.add(holder)

        // Controles de mouse
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED) { e ->
            draggingPrimary = e.isPrimaryButtonDown
            lastX = e.sceneX
            lastY = e.sceneY
        }
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED) { e ->
            val dx = e.sceneX - lastX
            val dy = e.sceneY - lastY
            lastX = e.sceneX
            lastY = e.sceneY
            if (draggingPrimary) orbit(dx, dy)
        }
        subScene.addEventHandler(ScrollEvent.SCROLL) { e ->
            zoom(e.deltaY)
        }
    }

    fun setTarget(x: Double, y: Double, z: Double) {
        pivot.translateX = x
        pivot.translateY = y
        pivot.translateZ = z
    }

    fun orbit(dx: Double, dy: Double) {
        val sens = 0.2
        yaw = (yaw + dx * sens) % 360.0
        pitch = (pitch + dy * sens).coerceIn(-89.0, 89.0)
        yawRotate.angle = yaw
        pitchRotate.angle = pitch
    }

    fun zoom(delta: Double) {
        val zoomFactor = 1.0 - (delta / 1000.0) // scroll pra frente aproxima
        distance = (distance * zoomFactor).coerceIn(50.0, 5000.0)
        camTranslate.z = -distance
    }

    fun center(centerView: Point3D, radiusView: Double) {
        setTarget(centerView.x, centerView.y, centerView.z)
        frame(radiusView, subScene.width, subScene.height)
    }

    /** Enquadra uma esfera de raio 'radius' (em unidades da cena), dado o tamanho do viewport. */
    fun frame(radius: Double, viewportWidth: Double, viewportHeight: Double) {
        val vfov = Math.toRadians(camera.fieldOfView)
        val hfov = 2.0 * atan(tan(vfov / 2.0) * (viewportWidth / viewportHeight))
        val distV = radius / tan(vfov / 2.0)
        val distH = radius / tan(hfov / 2.0)
        distance = max(distV, distH) * 1.2 // margem
        camTranslate.z = -distance
    }
}