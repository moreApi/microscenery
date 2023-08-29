package microscenery.stageSpace

import fromScenery.utils.extensions.times
import graphics.scenery.Scene
import graphics.scenery.Sphere
import graphics.scenery.utils.lazyLogger
import microscenery.*
import microscenery.VRUI.PathAblationTool
import microscenery.VRUI.PointCloudAblationTool
import microscenery.hardware.MicroscopeHardware
import org.joml.Vector3f


class AblationManager(val hardware: MicroscopeHardware, val stageSpaceManager: StageSpaceManager, val scene: Scene) {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    private var planedPath: List<Sphere> = emptyList()

    private fun getAllInkPositions(): List<Vector3f> {
        val pointsInWS = scene.discover(scene,{it is PointCloudAblationTool.Ink || it is PathAblationTool.InkLine })
            .flatMap { ink ->
                when (ink) {
                    is PointCloudAblationTool.Ink -> {
                        listOf(ink.spatial().position)
                    }
                    is PathAblationTool.InkLine -> {
                        val precision = MicroscenerySettings.getVector3(Settings.Ablation.Precision) ?: Vector3f(1f)
                        val path = mutableListOf<Vector3f>()
                        path += ink.spatial().position
                        if (ink.previous != null) {
                            // sample line between last and current position
                            path.addAll(sampleLineSmooth(ink.previous.spatial().position, ink.spatial().position, precision))
                        }
                        path
                    }
                    else -> {throw IllegalStateException("Unknown ink type")}
                }
            }
        return pointsInWS
    }

    private fun hideAllInk(){
        scene.discover(scene,{it is PointCloudAblationTool.Ink || it is PathAblationTool.InkLine })
            .forEach { it.visible = false }
    }

    private fun unhideAllInk(){
        scene.discover(scene,{it is PointCloudAblationTool.Ink || it is PathAblationTool.InkLine })
            .forEach { it.visible = true}
    }

    /**
     * go through [stageSpaceManager].stageRoot and get all ablationInk and ablationLine objects, discretize them into
     * points and render those points.
     */
    fun composeAblation() {
        scrapAblation()

        val points = getAllInkPositions()

        planedPath = points.map {
            val point = Sphere(0.01f, 8).apply {
                spatial {
                    this.position = it
                    this.scale *= stageSpaceManager.getInverseWorldScale()// todoMicroscenerySettings.getVector3(Settings.Ablation.Size) ?: Vector3f(1f)
                }
            }
            stageSpaceManager.stageRoot.addChild(point)
            point
        }
        hideAllInk()
    }

    fun scrapAblation() {
        planedPath.forEach {
            it.detach()
        }
        planedPath = emptyList()
        unhideAllInk()
    }

    fun executeAblation() {
        if (planedPath.isEmpty()) {
            logger.warn("No path planned")
            scene.findObserver()?.showMessage(
                "No path planned"
            )
            return
        }
        hardware.ablatePoints(buildLaserPath(planedPath.map { it.spatial().position }))
    }
}
