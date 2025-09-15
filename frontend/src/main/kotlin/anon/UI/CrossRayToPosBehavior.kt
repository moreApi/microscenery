package anon.UI

import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import fromScenery.utils.extensions.timesAssign
import graphics.scenery.Camera
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour

class CrossRayToPosBehavior(val cam: Camera, var action: (Vector3f) -> Unit): ClickBehaviour {

    private fun Pair<Vector3f, Vector3f>.flip() : Pair<Vector3f, Vector3f>{
        return this.second to this.first
    }

    var firstRayDir: Vector3f? = null
    var firstRayStart: Vector3f? = null

    /**
     * copied from https://math.stackexchange.com/a/4289668 then corrected
     */
    fun closestPointOnLines(line1Point: Vector3f, line1Dir: Vector3f, line2Point: Vector3f, line2Dir: Vector3f): Vector3f {
        val P1 = line1Point
        val P2 = line2Point
        val V1 = line1Dir
        val V2 = line2Dir
        val V21 = Vector3f(P2).sub(P1)

        val v22 = Vector3f(V2).dot(V2)
        val v11 = Vector3f(V1).dot( V1)
        val v21 = Vector3f(V2).dot( V1)
        val v21_1 = Vector3f(V21).dot( V1)
        val v21_2 = Vector3f(V21).dot( V2)
        val denom = v21 * v21 - v22 * v11

        val s = (v21_2 * v21 - v22 * v21_1) / denom
        val t = (-v21_1 * v21 + v11 * v21_2) / denom

        val p_a = P1 + s * V1
        val p_b = P2 + t * V2

        p_a.add(p_b)
        p_a.timesAssign(0.5f)

        return p_a
    }

    override fun click(x: Int, y: Int) {
        val (start, dir) = cam.screenPointToRay(x,y)
        if (firstRayStart == null){
            firstRayStart = start
            firstRayDir = dir
        } else {
            val (secondRayStart,secondRayDir) = cam.screenPointToRay(x,y)
            val S1 = closestPointOnLines(firstRayStart!!,firstRayDir!!,secondRayStart,secondRayDir)

            action(S1)

            firstRayStart = null
            firstRayDir = null
        }
    }
}