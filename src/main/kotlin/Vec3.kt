import kotlin.math.sqrt

data class Vec3(var x: Double, var y: Double, var z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    fun addInPlace(o: Vec3) { x += o.x; y += o.y; z += o.z }
    fun scaleInPlace(s: Double) { x *= s; y *= s; z *= s }

    fun length() = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val len = length()
        return if (len > 0) Vec3(x / len, y / len, z / len) else Vec3(0.0, 0.0, 0.0)
    }
}