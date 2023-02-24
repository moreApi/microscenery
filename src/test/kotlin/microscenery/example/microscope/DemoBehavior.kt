package microscenery.example.microscope

import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.nowMillis
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

@Suppress("unused")
class DemoBehavior(private val extend: Float, private val stageSpaceManager: StageSpaceManager) {

    fun randomStatic() {
        val sortedSlices = ArrayList<Vector3f>()
        for (i in 0..2) {
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
        for (z in 0..4) for (y in 0..4) for (x in 0..4) {
            val target = Vector3f(
                ((extend / 4) * x).coerceAtMost(extend - 1f),
                ((extend / 4) * y).coerceAtMost(extend - 1f),
                ((extend / 4) * z).coerceAtMost(extend - 1f)
            )

            sortedSlices.add(target)
        }

        sortedSlices.sortBy { it.z() }
        for (target in sortedSlices) {
            stageSpaceManager.stagePosition = target
            stageSpaceManager.snapSlice()
        }
    }

    fun randomLive() {
        stageSpaceManager.goLive()
        stageSpaceManager.focusTarget?.let { focus ->
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

    fun fixedStack(
        from: Vector3f = Vector3f(extend / 2, extend / 2, extend / 4),
        to: Vector3f = Vector3f(extend / 2, extend / 2, (extend / 4) * 3)
    ) {
        stageSpaceManager.stack(from, to, false)
    }

    fun liveStack(
        from: Vector3f = Vector3f(extend / 2, extend / 2, extend / 4),
        to: Vector3f = Vector3f(extend / 2, extend / 2, (extend / 4) * 3)
    ) {
        stageSpaceManager.stack(from, to, true)
    }
}