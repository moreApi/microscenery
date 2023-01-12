package microscenery.UI

import graphics.scenery.Camera
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.MouseDragPlane
import microscenery.MicroscenerySettings
import microscenery.stageSpace.FocusFrame
import microscenery.stageSpace.StageSpaceManager
import org.scijava.ui.behaviour.ClickBehaviour
import kotlin.concurrent.thread

class StageSpaceUI {
    companion object {
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
                        if (it.mode != FocusFrame.Mode.STEERING) {
                            it.mode = FocusFrame.Mode.STEERING
                        } else {
                            it.mode = FocusFrame.Mode.PASSIVE
                        }
                        stageSpaceManager.logger.info("focusframe mode is now ${it.mode}")
                    }
                }
            })
            inputHandler.addKeyBinding("steering", "4")

            inputHandler.addBehaviour("stackAcq", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    stageSpaceManager.focusTarget?.let {
                        if (it.mode == FocusFrame.Mode.STACK_SELECTION) {
                            stageSpaceManager.focusTarget?.let {
                                if (it.stackStartPos.z < it.spatial().position.z) stageSpaceManager.stack(
                                    it.stackStartPos,
                                    it.spatial().position,
                                    false
                                )
                                else stageSpaceManager.stack(it.spatial().position, it.stackStartPos, false)
                            }
                            it.mode = FocusFrame.Mode.PASSIVE
                        } else {
                            it.mode = FocusFrame.Mode.STACK_SELECTION
                        }
                        stageSpaceManager.logger.info("focusframe mode is now ${it.mode}")
                    }
                }
            })
            inputHandler.addKeyBinding("stackAcq", "5")


            inputHandler.addBehaviour("stop", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    stageSpaceManager.stop()
                }
            })
            inputHandler.addKeyBinding("stop", "0")


            inputHandler.addBehaviour("help", object : ClickBehaviour {
                override fun click(x: Int, y: Int) {
                    thread {
                        stageSpaceManager.scene.findObserver()?.showMessage(
                            "1:drag 2:snap 3:live 4:steer 5:stack 0:STOP E:toggle control"
                        )
//                    Thread.sleep(2000)
//                    scene.findObserver()?.showMessage(
//                        "AD - X, WS - Y, JK - Z"
//                    )
                    }

                }
            })
            inputHandler.addKeyBinding("help", "H")
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