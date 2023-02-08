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
import microscenery.*
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.StageSpaceManager
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.concurrent.thread

/**
 * Commands for key and swing button ui
 *
 * If key is null, no keyboard behavior will be created.
 * If command is null, no automatic keyboard behavior will be created and in swing gui only a label with the key will
 * be created
 */
data class StageUICommand(val name: String, val key: String?, val command: ClickBehaviour?)

class StageSpaceUI(val stageSpaceManager: StageSpaceManager) {
    var searchCubeStart: Box? = null

    val commands = listOf(
        StageUICommand("drag with mouse", "1", null),
        StageUICommand("snap", "2", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                stageSpaceManager.snapSlice()
            }
        }),
        StageUICommand("goLive", "3", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                stageSpaceManager.goLive()
            }
        }),
        StageUICommand("steering", "4", object : ClickBehaviour {
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
        }),
        StageUICommand("stackAcq", "5", object : ClickBehaviour {
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
        }),
        StageUICommand("searchCube", "6", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                val frame = stageSpaceManager.focusTarget ?: return
                if (searchCubeStart == null) {
                    stageSpaceManager.stageRoot.addChild(Box().apply {
                        spatial {
                            this.position = frame.spatial().position
                            this.scale = Vector3f((frame.children.first().ifSpatial {}?.scale?.x ?: 1f) / 5f)
                        }

                        searchCubeStart = this
                    })
                } else {
                    val p1 = searchCubeStart!!.spatial().position
                    val p2 = frame.spatial().position

                    stageSpaceManager.exploreCubeStageSpace(p1, p2)

                    searchCubeStart?.let { it.parent?.removeChild(it) }
                    searchCubeStart = null
                }
            }
        }),
        StageUICommand("ablate", "7", object : ClickBehaviour {
            val ablationPoints = mutableListOf<HasSpatial>()
            var goneToFirstPoint = false
            override fun click(x: Int, y: Int) {
                val frame = stageSpaceManager.focusTarget ?: return
                if (frame.mode != FrameGizmo.Mode.PASSIVE) {
                    stageSpaceManager.logger.warn("Frame not passive. Not going to plan ablation.")
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
                    stageSpaceManager.logger.info("set first ablation point to ${frame.spatial().position.toReadableString()}")
                    return
                }

                //no movement -> end condition
                if (last.spatial().position == frame.spatial().position) {
                    if (!goneToFirstPoint) {
                        stageSpaceManager.hardware.stagePosition = ablationPoints.first().spatial().position
                        stageSpaceManager.logger.warn("Moving stage to first point. Open laser and press again!")
                        goneToFirstPoint = true
                        return
                    }
                    stageSpaceManager.hardware.ablatePoints(
                        buildLaserPath(ablationPoints.map { it.spatial().position })
                    )

                    ablationPoints.forEach { it.parent?.removeChild(it) }
                    ablationPoints.clear()
                    goneToFirstPoint = false
                    return
                } else if (goneToFirstPoint) {
                    stageSpaceManager.logger.warn("Movement detected, aborting ablation staging.")
                    goneToFirstPoint = false
                }

                val precision = MicroscenerySettings.getVector3("Ablation.precision") ?: Vector3f(1f)

                // sample line between last and current position
                (sampleLine(last.spatial().position, frame.spatial().position, precision) + frame.spatial().position)
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
                    stageSpaceManager.logger.info("set ablation point to ${frame.spatial().position.toReadableString()}")
                }
            }
        }),
        StageUICommand("clearStage", "8", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                stageSpaceManager.clearStage()
            }
        }),
        StageUICommand("stop", "0", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                searchCubeStart?.let { it.parent?.removeChild(it) }
                searchCubeStart = null
                stageSpaceManager.stop()
            }
        }),
        StageUICommand("help", "H", object : ClickBehaviour {
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
        }),
        StageUICommand("openEditors", "T", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                TransferFunctionEditor(stageSpaceManager)
                //SettingsEditor(MicroscenerySettings)
            }
        }),
        StageUICommand("toggle frame/cam control", "E", null),
    )

    fun stageSwingUI(panel: JPanel){
        commands.forEach {
            val (name, key, command) = it

            when {
                (command == null && key != null) -> {
                    panel.add(JLabel(name))
                    // two panels to be aligned with buttons
                    panel.add(JLabel(" : $key"),"wrap")
                }
                (command != null) -> {
                    val but = JButton(name)
                    but.addActionListener { command.click(0,0) }
                    if (key == null){
                        panel.add(but,"wrap")
                    } else {
                        panel.add(but)
                        panel.add(JLabel(" : $key"),"wrap")
                    }
                }
            }
        }
    }

    fun stageKeyUI(inputHandler: InputHandler, cam: Camera) {
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

        commands.forEach {
            val (name, key, command) = it
            if (key == null || command == null) return@forEach

            inputHandler.addBehaviour(name, command)
            inputHandler.addKeyBinding(name, key)
        }
    }

    fun stageUI(base: DefaultScene, inputHandler: InputHandler?){
        base.extraPanel?.let { stageSwingUI(it) }
        base.mainFrame?.pack()

        inputHandler?.let {
            stageKeyUI(it, base.cam)
        }
    }

    companion object {
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