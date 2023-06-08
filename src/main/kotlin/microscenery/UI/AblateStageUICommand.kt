package microscenery.UI

import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.primitives.Cylinder
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.lazyLogger
import microscenery.*
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour

class AblateStageUICommand(stageSpaceManager: StageSpaceManager): StageUICommand("ablate", "7", object : ClickBehaviour {
    val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val ablationPoints = mutableListOf<HasSpatial>()
    var goneToFirstPoint = false

    override fun click(x: Int, y: Int) {
        val frame = stageSpaceManager.focusTarget ?: return
        if (frame.mode != FrameGizmo.Mode.PASSIVE) {
            logger.warn("Frame not passive. Not going to plan ablation.")
            return
        }

        val last = ablationPoints.lastOrNull()
        if (last == null) {
            //first point
            val point = Sphere(0.25f, 8).apply {
                spatial {
                    this.position = frame.spatial().position
                    //get scaling from frame
                    this.scale = Vector3f((frame.children.first().ifSpatial {}?.scale?.x ?: 1f) / 5f)
                }
            }
            stageSpaceManager.stageRoot.addChild(point)
            ablationPoints += point
            logger.info("set first ablation point to ${frame.spatial().position.toReadableString()}")
            return
        }

        //no movement -> end condition
        if (last.spatial().position == frame.spatial().position) {
            if (!goneToFirstPoint) {
                stageSpaceManager.hardware.stagePosition = ablationPoints.first().spatial().position
                logger.warn("Moving stage to first point. Open laser and press again!")
                goneToFirstPoint = true
                return
            }
            executeAblationCommandSequence(
                stageSpaceManager.hardware,
                buildLaserPath(ablationPoints.map { it.spatial().position })
            )

            ablationPoints.forEach { it.parent?.removeChild(it) }
            ablationPoints.clear()
            goneToFirstPoint = false
            return
        } else if (goneToFirstPoint) {
            logger.warn("Movement detected, aborting ablation staging.")
            goneToFirstPoint = false
        }

        val precision = MicroscenerySettings.getVector3("Ablation.precision") ?: Vector3f(1f)

        // sample line between last and current position
        (sampleLineGrid(last.spatial().position, frame.spatial().position, precision) + frame.spatial().position)
            .forEach {
                val point = Sphere(0.25f, 8).apply {
                    spatial {
                        this.position = it
                        this.scale = Vector3f(last.spatial().scale) / 2f
                    }
                }
                stageSpaceManager.stageRoot.addChild(point)
                ablationPoints += point
            }

        // increase size of this positions marker and add line
        ablationPoints.last().let {
            it.spatial().scale *= 2f

            val diff = it.spatial().position - last.spatial().position

            val line = Cylinder(0.01f, 1f, 20)
            line.material().metallic = 0.0f
            line.material().roughness = 1.0f
            line.spatial {
                scale.y = diff.length() / last.spatial().scale.x
                rotation = Quaternionf().rotationTo(UP, diff)
            }
            last.addChild(line)
            logger.info("set ablation point to ${frame.spatial().position.toReadableString()}")
        }
    }
}) {
}