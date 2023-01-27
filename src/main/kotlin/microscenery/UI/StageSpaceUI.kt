package microscenery.UI

import graphics.scenery.Box
import graphics.scenery.Camera
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.MouseDragPlane
import graphics.scenery.primitives.Cylinder
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.MicroscenerySettings
import microscenery.UP
import microscenery.let
import microscenery.signals.ClientSignal
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.StageSpaceManager
import microscenery.toReadableString
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.concurrent.thread

class StageSpaceUI {
    companion object {
        /**
         * Samples a line between [p1] and [p2] with [precision] steps
         *
         * @return all points between [p1] and [p2]
         */
        private fun sampleLine(p1: Vector3f, p2: Vector3f, precision: Vector3f): List<Vector3f>{
            val diff = p2 - p1
            val subDivisions = Vector3f(diff).absolute() / precision
            val leadDim = subDivisions.maxComponent()
            val otherDims = listOf(0,1,2).filter { it != leadDim }
            val stepSize = Vector3f(diff) / subDivisions

            val result = mutableListOf<Vector3f>()
            for (i in 1 until subDivisions[leadDim].toInt()){
                val exactPosition = diff * (i / subDivisions[leadDim] )
                val p = Vector3f()
                p.setComponent(leadDim, p1[leadDim] + stepSize[leadDim] * i)
                for (dim in otherDims) {
                    val precisionSteps = exactPosition[dim] / precision[dim]
                    p.setComponent(dim, p1[dim]
                            + precisionSteps.toInt() * precision[dim]
                            + if(precisionSteps - precisionSteps.toInt() > 0.5f ) precision[dim] else 0f)
                }
                result += p
            }
            return result
        }

        fun stageUserInteraction(stageSpaceManager: StageSpaceManager, inputHandler: InputHandler, cam: Camera) {
            listOf(
                "frame_forward", "frame_back", "frame_left", "frame_right", "frame_up", "frame_down"
            ).forEach { name ->
                inputHandler.addBehaviour(
                    name,
                    MovementCommand(name.removePrefix("frame_"), { stageSpaceManager.focusTarget }, cam, speed = 1f)
                )
            }
            MicroscenerySettings.setIfUnset("FrameControl", false)
            remapControl(stageSpaceManager, inputHandler)
            MicroscenerySettings.addUpdateRoutine(
                "FrameControl"
            ) {
                stageSpaceManager.logger.info("FrameControl = ${MicroscenerySettings.getProperty<Boolean>("FrameControl")}")
                remapControl(stageSpaceManager, inputHandler)
            }

            inputHandler.addBehaviour("switchControl", ClickBehaviour { _, _ ->
                val frameControl = MicroscenerySettings.getProperty<Boolean>("FrameControl")
                MicroscenerySettings.set("FrameControl", !frameControl)
            })
            inputHandler.addKeyBinding("switchControl", "E")

            inputHandler.addBehaviour(
                "frameDragging", MouseDragPlane("frameDragging",
                    { stageSpaceManager.scene.findObserver() },
                    { stageSpaceManager.focusTarget },
                    false,
                    mouseSpeed = { 100f * 5 / stageSpaceManager.scaleDownFactor })
            )
            inputHandler.addKeyBinding("frameDragging", "1")

            inputHandler.addBehaviour("snap", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    stageSpaceManager.snapSlice()
                }
            })
            inputHandler.addKeyBinding("snap", "2")

            inputHandler.addBehaviour("goLive", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    stageSpaceManager.goLive()
                }
            })
            inputHandler.addKeyBinding("goLive", "3")

            inputHandler.addBehaviour("steering", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    stageSpaceManager.focusTarget?.let {
                        if (it.mode != FrameGizmo.Mode.STEERING) {
                            it.mode = FrameGizmo.Mode.STEERING
                        } else {
                            it.mode = FrameGizmo.Mode.PASSIVE
                        }
                        stageSpaceManager.logger.info("focusframe mode is now ${it.mode}")
                    }
                }
            })
            inputHandler.addKeyBinding("steering", "4")

            inputHandler.addBehaviour("stackAcq", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    stageSpaceManager.focusTarget?.let {
                        if (it.mode == FrameGizmo.Mode.STACK_SELECTION) {
                            stageSpaceManager.focusTarget?.let {
                                if (it.stackStartPos.z < it.spatial().position.z) stageSpaceManager.stack(
                                    it.stackStartPos,
                                    it.spatial().position,
                                    false
                                )
                                else stageSpaceManager.stack(it.spatial().position, it.stackStartPos, false)
                            }
                            it.mode = FrameGizmo.Mode.PASSIVE
                        } else {
                            it.mode = FrameGizmo.Mode.STACK_SELECTION
                        }
                        stageSpaceManager.logger.info("focusframe mode is now ${it.mode}")
                    }
                }
            })
            inputHandler.addKeyBinding("stackAcq", "5")

            var searchCubeStart: Box? = null
            inputHandler.addBehaviour("searchCube", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    val frame = stageSpaceManager.focusTarget ?: return
                    if (searchCubeStart == null){
                        stageSpaceManager.stageRoot.addChild(Box().apply {
                            spatial{
                                this.position = frame.spatial().position
                                this.scale = Vector3f((frame.children.first().ifSpatial {}?.scale?.x  ?: 1f )/5f)
                            }

                            searchCubeStart = this
                        })
                    } else {
                        val p1 = searchCubeStart!!.spatial().position
                        val p2 = frame.spatial().position

                        stageSpaceManager.exploreCubeStageSpace(p1,p2)

                        searchCubeStart?.let { it.parent?.removeChild(it) }
                        searchCubeStart = null
                    }
                }
            })
            inputHandler.addKeyBinding("searchCube", "6")

            val ablationPoints = mutableListOf<HasSpatial>()
            var goneToFirstPoint = false
            inputHandler.addBehaviour("ablate", object : ClickBehaviour{
                override fun click(x: Int, y: Int) {
                    val frame = stageSpaceManager.focusTarget ?: return
                    if (frame.mode != FrameGizmo.Mode.PASSIVE){
                        stageSpaceManager.logger.warn("Frame not passive. Not going to plan ablation.")
                        return
                    }

                    val last = ablationPoints.lastOrNull()
                    if (last == null){
                        //first point
                        val point = Sphere(0.25f,8).apply {
                            spatial {
                                this.position = frame.spatial().position
                                //get scaling from frame
                                this.scale = Vector3f((frame.children.first().ifSpatial {}?.scale?.x  ?: 1f )/5f)
                            }
                        }
                        stageSpaceManager.stageRoot.addChild(point)
                        ablationPoints += point
                        stageSpaceManager.logger.info("set first ablation point to ${frame.spatial().position.toReadableString()}")
                        return
                    }

                    //no movement -> end condition
                    if (last.spatial().position == frame.spatial().position) {
                        if (!goneToFirstPoint){
                            stageSpaceManager.hardware.stagePosition = ablationPoints.first().spatial().position
                            stageSpaceManager.logger.warn("Moving stage to first point. Open laser and press again!")
                            goneToFirstPoint = true
                            return
                        }
                        stageSpaceManager.hardware.ablatePoints(
                            ClientSignal.AblationPoints(
                                ablationPoints.mapIndexed { index, node ->
                                    ClientSignal.AblationPoint(
                                        node.spatial().position,
                                        0L,
                                        index == 0,
                                        index == ablationPoints.size - 1,
                                        0f, //todo set actual laser power
                                        false
                                    ).apply {
                                        stageSpaceManager.logger.info("Building Ablation Point $this")
                                    }
                                }
                            )
                        )

                        ablationPoints.forEach { it.parent?.removeChild(it) }
                        ablationPoints.clear()
                        goneToFirstPoint = false
                        return
                    } else if(goneToFirstPoint){
                        stageSpaceManager.logger.warn("Movement detected, aborting ablation staging.")
                        goneToFirstPoint = false
                    }

                    val precision = MicroscenerySettings.getOrNull<Float>("Stage.precisionXY").let(
                        MicroscenerySettings.getOrNull<Float>("Stage.precisionZ")){ xy: Float, z: Float ->
                        Vector3f(xy,xy,z)
                    } ?: Vector3f(1f)

                    // sample line between last and current position
                    (sampleLine(last.spatial().position, frame.spatial().position, precision ) + frame.spatial().position)
                        .forEach {
                            val point = Sphere(0.25f,8).apply {
                                spatial {
                                    this.position = it
                                    this.scale = Vector3f(last.spatial().scale)/ 2f
                                }
                            }
                            stageSpaceManager.stageRoot.addChild(point)
                            ablationPoints += point
                        }

                    // increase size of this positions marker and add line
                    ablationPoints.last().let{
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
                        stageSpaceManager.logger.info("set ablation point to ${frame.spatial().position.toReadableString()}")
                    }
                }
            })
            inputHandler.addKeyBinding("ablate", "7")

            inputHandler.addBehaviour("stop", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    searchCubeStart?.let { it.parent?.removeChild(it) }
                    searchCubeStart = null
                    stageSpaceManager.stop()
                }
            })
            inputHandler.addKeyBinding("stop", "0")


            inputHandler.addBehaviour("help", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    thread {
                        stageSpaceManager.scene.findObserver()?.showMessage(
                            "1:drag 2:snap 3:live 4:steer"
                        )
                        Thread.sleep(2000)
                        stageSpaceManager.scene.findObserver()?.showMessage(
                            "5:stack 6: explCube 0:STOP E:toggle control"
                        )
                    }

                }
            })
            inputHandler.addKeyBinding("help", "H")

            inputHandler.addBehaviour("openEditors", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    TransferFunctionEditor(stageSpaceManager)
                    //SettingsEditor(MicroscenerySettings)
                }
            })
            inputHandler.addKeyBinding("openEditors", "T")

        }
        private fun remapControl(stageSpaceManager: StageSpaceManager, inputHandler: InputHandler) {
            val frameControl = MicroscenerySettings.getProperty<Boolean>("FrameControl")
            val defaultBehaviours = listOf(
                "move_forward" to "W",
                "move_back" to "S",
                "move_left" to "A",
                "move_right" to "D",
                "move_up" to "K",
                "move_down" to "J"
            )
            val frameBehaviours = listOf(
                "frame_forward" to "J",
                "frame_back" to "K",
                "frame_left" to "A",
                "frame_right" to "D",
                "frame_up" to "W",
                "frame_down" to "S"
            )
            if (frameControl) {
                defaultBehaviours.forEach { (name, _) ->
                    inputHandler.removeKeyBinding(name)
                    stageSpaceManager.logger.info("removed keys $name")
                }
                frameBehaviours.forEach { (name, key) ->
                    inputHandler.addKeyBinding(name, key)
                    stageSpaceManager.logger.info("added key $key to $name")
                }
            } else {
                frameBehaviours.forEach { (name, _) ->
                    inputHandler.removeKeyBinding(name)
                    stageSpaceManager.logger.info("removed keys from $name")
                }
                defaultBehaviours.forEach { (name, key) ->
                    inputHandler.addKeyBinding(name, key)
                    stageSpaceManager.logger.info("added key $key to $name")
                }
            }
        }
    }
}