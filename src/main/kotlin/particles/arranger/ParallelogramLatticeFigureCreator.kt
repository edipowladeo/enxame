package particles.arranger

import Vec3
import particles.Particle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt



class ParallelogramLatticeFigureCreator(
    private val count: Int,
    private val center: Vec3,
    private val separation: Vec3,                 // (dx, dy, dz)
    private val ratios: AspectRatios = AspectRatios(),
    private val preferredDims: Triple<Int, Int, Int>? = null, // if set, overrides ratios
    private val xyShear: Double = 0.0             // unitless: X offset per Y step (in dy units)
) : FigureCreator {
    data class AspectRatios(
        val rowsOverCols: Double = 1.0,
        val planeOverLayers: Double = 1.0
    )
    override fun create(): FigureOfParticles {
        if (count <= 0) return FigureOfParticles(emptyList())

        val (nx, ny, nz) = preferredDims ?: solveDims(count, ratios)

        // Basis vectors: a along +X, b skewed in XY, c along +Z
        val a = Vec3(separation.x, 0.0, 0.0)
        val b = Vec3(separation.y * xyShear, separation.y, 0.0)
        val c = Vec3(0.0, 0.0, separation.z)

        // Center the skewed box: origin = center - 0.5*((nx-1)a + (ny-1)b + (nz-1)c)
        val ext = add(add(scale(a, (nx - 1).toDouble()), scale(b, (ny - 1).toDouble())), scale(c, (nz - 1).toDouble()))
        val origin = Vec3(center.x - ext.x / 2.0, center.y - ext.y / 2.0, center.z - ext.z / 2.0)

        val particles = ArrayList<Particle>(count)
        outer@ for (k in 0 until nz) {
            for (j in 0 until ny) {
                for (i in 0 until nx) {
                    if (particles.size >= count) break@outer
                    val pos = add(origin, add(add(scale(a, i.toDouble()), scale(b, j.toDouble())), scale(c, k.toDouble())))
                    particles.add(Particle(pos, Vec3(0.0, 0.0, 0.0)))
                }
            }
        }
        return FigureOfParticles(particles)
    }

    // --- dimension solver guided by your aspect ratios ---
    private fun solveDims(N: Int, ratios: AspectRatios): Triple<Int, Int, Int> {
        val eps = 1e-9
        val rRC = max(ratios.rowsOverCols, eps)       // ny/nx target
        val rPZ = max(ratios.planeOverLayers, eps)    // sqrt(nx*ny)/nz target

        // Continuous guess (derived): nx ≈ (N * rPZ / rRC^(3/2))^(1/3)
        var nx = max(1.0, (N * rPZ / rRC.pow(1.5)).pow(1.0 / 3.0)).roundToInt()
        var ny = max(1, (rRC * nx).roundToInt())
        var nz = max(1, ((nx * sqrt(rRC)) / rPZ).roundToInt())

        fun cap() = nx * ny * nz
        fun err(nx: Int, ny: Int, nz: Int): Double {
            val r1 = ny.toDouble() / nx
            val r2 = sqrt(nx.toDouble() * ny) / nz
            // log-distance is symmetric and scale-invariant
            val e1 = abs(ln(r1 / rRC))
            val e2 = abs(ln(r2 / rPZ))
            return e1 + e2
        }

        // Grow until capacity meets/exceeds N, picking the axis that best preserves ratios
        while (cap() < N) {
            val cand = listOf(
                Triple(nx + 1, ny, nz),
                Triple(nx, ny + 1, nz),
                Triple(nx, ny, nz + 1)
            )
            val best = cand.minBy { (cx, cy, cz) -> err(cx, cy, cz) }
            nx = best.first; ny = best.second; nz = best.third
        }

        return Triple(nx, ny, nz)
    }

    // --- tiny Vec3 helpers (if your Vec3 has operators, replace these) ---
    private fun scale(v: Vec3, s: Double) = Vec3(v.x * s, v.y * s, v.z * s)
    private fun add(a: Vec3, b: Vec3) = Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
}