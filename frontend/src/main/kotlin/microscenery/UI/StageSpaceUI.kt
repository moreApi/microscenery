package microscenery.UI

import fromScenery.SettingsEditor
import graphics.scenery.Box
import graphics.scenery.Camera
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.MouseDragPlane
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.TransferFunctionEditor
import microscenery.DefaultScene
import microscenery.MicroscenerySettings
import microscenery.initAblationSettings
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.StageSpaceManager
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
open class StageUICommand(val name: String, val key: String?, val command: ClickBehaviour?)

class StageSpaceUI(val stageSpaceManager: StageSpaceManager) {
    internal val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    init {
        initAblationSettings()
    }

    var searchCubeStart: Box? = null

    val comGoLive = StageUICommand("goLive", "3"
    ) { x, y -> stageSpaceManager.goLive() }
    val comSteering = StageUICommand("steering", "4") { x, y ->
        stageSpaceManager.focusTarget?.let {
            if (it.mode != FrameGizmo.Mode.STEERING) {
                it.mode = FrameGizmo.Mode.STEERING
            } else {
                it.mode = FrameGizmo.Mode.PASSIVE
            }
            logger.info("focus frame mode is now ${it.mode}")
        }
    }
    val comStackAcq = StageUICommand("stackAcq", "5") { x, y ->
        stageSpaceManager.focusTarget?.let {
            if (it.mode == FrameGizmo.Mode.STACK_SELECTION) {
                stageSpaceManager.focusTarget?.let {
                    if (it.stackStartPos.z < it.spatial().position.z) stageSpaceManager.stack(
                        it.stackStartPos,
                        it.spatial().position
                    )
                    else stageSpaceManager.stack(it.spatial().position, it.stackStartPos)
                }
                it.mode = FrameGizmo.Mode.PASSIVE
            } else {
                it.mode = FrameGizmo.Mode.STACK_SELECTION
            }
            logger.info("focus frame mode is now ${it.mode}")
        }
    }
    val comSearchCube = StageUICommand("searchCube", "6", object : ClickBehaviour {
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
    })
    val comAblate = AblateStageUICommand(stageSpaceManager)
    val comClearStage = StageUICommand("clearStage", "8") { x, y -> stageSpaceManager.clearStage() }
    val comStop = StageUICommand("stop", "0") { x, y ->
        searchCubeStart?.let { it.parent?.removeChild(it) }
        searchCubeStart = null
        stageSpaceManager.stop()
    }

    val desktopCommands = listOf(
        StageUICommand("drag with mouse", "1", null),
        StageUICommand("snap", "2", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                logger.info("Snap slice command")
                stageSpaceManager.snapSlice()
            }
        }),
        comGoLive,
        comSteering,
        comStackAcq,
        comSearchCube,
        comAblate,
        comClearStage,
        comStop,
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
                TransferFunctionEditor.showTFFrame(stageSpaceManager.sliceManager.transferFunctionManager)
                SettingsEditor(MicroscenerySettings)
            }
        }),
        StageUICommand("toggle frame/cam control", "E", null),
        StageUICommand("toggleBorders", "B", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                val state = MicroscenerySettings.get<Boolean>("Stage.ToggleSliceBorder")
                MicroscenerySettings.set("Stage.ToggleSliceBorder", !state)
            }
        }),
    )

    val vrCommands = listOf(comGoLive,comSteering,comStackAcq,comSearchCube,comClearStage,comStop)

    fun vrMenuActions(): List<Pair<String, (Spatial) -> Unit>> = vrCommands.map {
            it.name to {_ -> it.command?.click(0,0)}
        }

    fun stageSwingUI(panel: JPanel) {
        desktopCommands.forEach {
            val name = it.name
            val key = it.key
            val command = it.command

            when {
                (command == null && key != null) -> {
                    panel.add(JLabel(name))
                    // two panels to be aligned with buttons
                    panel.add(JLabel(" : $key"), "wrap")
                }
                (command != null) -> {
                    val but = JButton(name)
                    but.addActionListener { command.click(0, 0) }
                    if (key == null) {
                        panel.add(but, "wrap")
                    } else {
                        panel.add(but)
                        panel.add(JLabel(" : $key"), "wrap")
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
        remapControl(inputHandler)
        MicroscenerySettings.addUpdateRoutine(
            "FrameControl"
        ) {
            logger.info("FrameControl = ${MicroscenerySettings.getProperty<Boolean>("FrameControl")}")
            remapControl(inputHandler)
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
                mouseSpeed = { 25f })
        )
        inputHandler.addKeyBinding("frameDragging", "1")

        desktopCommands.forEach {
            val name = it.name
            val key = it.key
            val command = it.command
            if (key == null || command == null) return@forEach

            inputHandler.addBehaviour(name, command)
            inputHandler.addKeyBinding(name, key)
        }
    }

    fun stageUI(base: DefaultScene, inputHandler: InputHandler?) {
        base.extraPanel?.let { stageSwingUI(it) }
        base.mainFrame?.pack()

        inputHandler?.let {
            stageKeyUI(it, base.cam)
        }
    }

    private fun remapControl(inputHandler: InputHandler) {
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
            logger.info("Frame control active.")
            defaultBehaviours.forEach { (name, _) ->
                inputHandler.removeKeyBinding(name)
                logger.debug("removed keys $name")
            }
            frameBehaviours.forEach { (name, key) ->
                inputHandler.addKeyBinding(name, key)
                logger.debug("added key $key to $name")
            }
        } else {
            logger.info("Camera control active.")
            frameBehaviours.forEach { (name, _) ->
                inputHandler.removeKeyBinding(name)
                logger.debug("removed keys from $name")
            }
            defaultBehaviours.forEach { (name, key) ->
                inputHandler.addKeyBinding(name, key)
                logger.debug("added key $key to $name")
            }
        }
    }
}