package microscenery.example.microscope

import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.stageSpace.StageSpaceManager
import microscenery.nowMillis
import org.joml.Vector3f

@Suppress("unused")
class DemoBehavior(private val extend: Float, private val stageSpaceManager: StageSpaceManager) {

    fun randomStatic() {
        val sortedSlices = ArrayList<Vector3f>()
        for (i in 0..200) {
            val target = Random.random3DVectorFromRange(0f, extend)
            sortedSlices.add(target)
        }

        sortedSlices.sortBy { it.z() }
        for (target in sortedSlices) {
            stageSpaceManager.stagePosition = target
            stageSpaceManager.snapSlice()
        }
    }

    fun fixed() {
        val sortedSlices = ArrayList<Vector3f>()
        for (z in 0 .. 3) for (y in  0 .. 3) for (x in  0 .. 3) {
            val target = Vector3f((extend/4)*x, (extend/4)*y, (extend/4)*z)
            sortedSlices.add(target)
        }

        sortedSlices.sortBy { it.z() }
        for (target in sortedSlices) {
            stageSpaceManager.stagePosition = target
            stageSpaceManager.snapSlice()
        }
    }

    fun randomLive() {
        stageSpaceManager.live(true)
        stageSpaceManager.focusFrame?.let { focus ->
            var start = Vector3f()
            var target = Vector3f()
            var startTime = 0L
            val travelTime = 3000
            focus.update += {
                focus.spatial {

                    if (nowMillis() > startTime + travelTime) {
                        startTime = nowMillis()
                        position = target
                        start = position
                        target = Random.random3DVectorFromRange(0f, extend)
                    }

                    val dir = target - start
                    val relPos = (nowMillis() - startTime) / travelTime.toFloat()
                    position = start + (dir * relPos)
                    stageSpaceManager.stagePosition = position
                }
            }
        }
    }

    fun fixedStack(from: Vector3f = Vector3f(extend / 4), to: Vector3f =  Vector3f((extend / 4) * 3)) {
        stageSpaceManager.stack(from ,to)
    }
}