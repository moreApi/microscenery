package microscenery.example.microscope

import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.StageSpaceManager
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
        for (z in listOf(0, 50, 100, 150, 199)) for (y in listOf(0, 50, 100, 150)) for (x in listOf(0, 50, 100, 150)) {
            val target = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
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

    fun fixedStack(){
        stageSpaceManager.stack(Vector3f(extend/4),Vector3f((extend/4)*3))
    }
}