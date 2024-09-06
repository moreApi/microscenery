package microscenery.scenes.microscope

import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.nowMillis
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

@Suppress("unused")
class DemoBehavior(private val extend: Vector3f, private val stageSpaceManager: StageSpaceManager) {

    fun randomStatic(amount: Int = 200) {
        val sortedSlices = ArrayList<Vector3f>()
        for (i in 0..amount) {
            val target = Random.random3DVectorFromRange(0f, 1f) * extend
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
        for (z in 0..4) for (y in 0..4) for (x in 0..4) {

            val target = Vector3f(
                ((extend.x / 4) * x).coerceAtMost(extend.x - 1f),
                ((extend.y / 4) * y).coerceAtMost(extend.y - 1f),
                ((extend.z / 4) * z).coerceAtMost(extend.z - 1f)
            )

            sortedSlices.add(target)
        }

        sortedSlices.sortBy { it.z() }
        for (target in sortedSlices) {
            stageSpaceManager.stagePosition = target
            stageSpaceManager.snapSlice()
        }
    }

    fun positions(vararg pos: Vector3f) {
        pos.sortBy { it.z() }
        for (target in pos) {
            stageSpaceManager.stagePosition = target
            stageSpaceManager.snapSlice()
        }
    }

    fun randomLive() {
        stageSpaceManager.goLive()
        stageSpaceManager.focusManager.focusTarget.let { focus ->
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
                        target = Random.random3DVectorFromRange(0f, 1f) * extend
                    }

                    val dir = target - start
                    val relPos = (nowMillis() - startTime) / travelTime.toFloat()
                    position = start + (dir * relPos)
                    stageSpaceManager.stagePosition = position
                }
            }
        }
    }

    fun fixedStack(from: Vector3f, to: Vector3f) {
        stageSpaceManager.stack(from, to, false)
    }

    fun liveStack(from: Vector3f, to: Vector3f) {
        stageSpaceManager.stack(from, to, true)
    }
}