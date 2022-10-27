package microscenery.example

import graphics.scenery.Box
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.hardware.DemoMicroscopeHardware
import microscenery.nowMillis
import org.joml.Vector3f
import kotlin.concurrent.thread

class DemoHWScene : DefaultScene() {
    init {
        logger.info("Starting demo hw scene")

        val hw = DemoMicroscopeHardware()
        val stageSpaceManager = StageSpaceManager(hw, scene, addFocusFrame = true)

        stageSpaceManager.stageRoot.spatial().scale *= Vector3f(1f, 1f, 2f)

        val hullbox = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        hullbox.name = "hullbox"
        hullbox.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        scene.addChild(hullbox)

        demoBehavior(DemoBehaviorMode.RandomLive,hw.side.toFloat(), stageSpaceManager)

        thread {
            while (true) {
                Thread.sleep(200)
                scene to stageSpaceManager
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DemoHWScene().main()
        }

        fun demoBehavior(
            mode: DemoBehaviorMode,
            extend: Float,
            stageSpaceManager: StageSpaceManager
        ) {
            val sortedSlices = ArrayList<Vector3f>()
            when (mode) {
                DemoBehaviorMode.RandomStatic -> {
                    for (i in 0..200) {
                        val target = Random.random3DVectorFromRange(0f, extend)
                        sortedSlices.add(target)
                    }
                }
                DemoBehaviorMode.Fixed -> {
                    for (z in listOf(0, 50, 100, 150, 199))
                        for (y in listOf(0, 50, 100, 150))
                            for (x in listOf(0, 50, 100, 150)) {
                                val target = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
                                sortedSlices.add(target)
                            }
                }
                DemoBehaviorMode.RandomLive -> {
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
            }
            sortedSlices.sortBy { it.z() }
            for (target in sortedSlices) {
                stageSpaceManager.stagePosition = target
                stageSpaceManager.snapSlice()
            }
        }
    }
}


enum class DemoBehaviorMode {
    RandomStatic,
    Fixed,
    RandomLive
}
