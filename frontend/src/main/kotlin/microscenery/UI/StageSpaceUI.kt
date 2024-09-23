package microscenery.UI

import fromScenery.SettingsEditor
import graphics.scenery.Box
import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.behaviours.MouseDragPlane
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.TransferFunctionEditor
import graphics.scenery.volumes.Volume
import microscenery.*
import microscenery.hardware.micromanagerConnection.MicroManagerUtil
import microscenery.stageSpace.*
import microscenery.stageSpace.FocusManager
import net.miginfocom.swing.MigLayout
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
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

    var searchCubeStart: HasSpatial? = null

    val comGoLive = StageUICommand("goLive", "3"
    ) { _, _ -> stageSpaceManager.goLive() }
    val comSteering = StageUICommand("steering", "4") { _, _ ->
        stageSpaceManager.focusManager.let {
            if (it.mode != FocusManager.Mode.STEERING) {
                it.mode = FocusManager.Mode.STEERING
            } else {
                it.mode = FocusManager.Mode.PASSIVE
            }
            logger.info("focus frame mode is now ${it.mode}")
        }
    }
    val comStackAcq = StageUICommand("stackAcq", "5") { _, _ ->
        stageSpaceManager.focusManager.let {
            if (it.mode == FocusManager.Mode.STACK_SELECTION) {
                it.focusTarget.let { focusTarget ->
                    if (it.stackStartPos.z < focusTarget.spatial().position.z) stageSpaceManager.stack(
                        it.stackStartPos,
                        focusTarget.spatial().position
                    )
                    else stageSpaceManager.stack(focusTarget.spatial().position, it.stackStartPos)
                }
                it.mode = FocusManager.Mode.PASSIVE
            } else {
                it.mode = FocusManager.Mode.STACK_SELECTION
                it.stageSpaceManager.stop()
            }
            logger.info("focus frame mode is now ${it.mode}")
        }
    }
    val comSearchCube = StageUICommand("searchCube", "6", object : ClickBehaviour {
        override fun click(x: Int, y: Int) {
            val focusTarget = stageSpaceManager.focusManager.focusTarget
            if (searchCubeStart == null) {

                searchCubeStart = Frame(stageSpaceManager.uiModel, Vector3f(0.2f,0.8f,0.5f)).also {
                    it.spatial {
                        rotation = stageSpaceManager.layout.sheetRotation()
                        position = focusTarget.spatial().position.copy()
                    }
                    stageSpaceManager.stageRoot.addChild(it)
                }
            } else {
                val p1 = searchCubeStart!!.spatial().position
                val p2 = focusTarget.spatial().position

                stageSpaceManager.exploreCubeStageSpace(p1, p2)

                searchCubeStart?.detach()
                searchCubeStart = null
            }
        }
    })
    val comAblate = AblateStageUICommand(stageSpaceManager)
    val comClearStage = StageUICommand("clearStage", "8") { _, _ -> stageSpaceManager.clearStage() }
    val comStop = StageUICommand("stop", "0") { _, _ ->
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
        StageUICommand("TransFunc", "T", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                TransferFunctionEditor.showTFFrame(stageSpaceManager.sliceManager.transferFunctionManager, "Transfer Function")
            }
        }),
        StageUICommand("Settings", "T", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                SettingsEditor(MicroscenerySettings)
            }
        }),
        StageUICommand("toggle frame/cam control", "E", null),
        StageUICommand("toggleBorders", "B", object : ClickBehaviour {
            override fun click(x: Int, y: Int) {
                val state = MicroscenerySettings.get<Boolean>("Stage.ToggleSliceBorder",false)
                MicroscenerySettings.set("Stage.ToggleSliceBorder", !state)
            }
        }),
    )

    fun stageSwingUI(panel: JPanel, customCommands: List<StageUICommand>, msHub: MicrosceneryHub) {
        val uiModel = msHub.getAttribute(UIModel::class.java)

        val infoPanel = JPanel(MigLayout())
        infoPanel.border = BorderFactory.createTitledBorder("Inspector")
        panel.add(infoPanel, "wrap")

        uiModel.changeEvents += { event ->
            when (event.kProperty) {
                UIModel::selected -> {
                    infoPanel.removeAll()
                    (event.new as? Node)?.let { node ->
                        infoPanel.add(JLabel("name: ${node.name}"), "wrap")
                        val sliceManager = msHub.getAttribute(SliceManager::class.java)
                        when(node){
                            is Volume -> {
                                val meta = sliceManager.getStackMetadata(node) ?: return@let
                                val created = SimpleDateFormat("hh:mm:ss").format(Date(meta.created))
                                infoPanel.add(JLabel("Created: $created"),"wrap")
                                infoPanel.add(JLabel("Slices: ${meta.slicesCount}"),"wrap")
                                infoPanel.add(JLabel("Live: ${meta.live}"),"wrap")
                                if (node.timepointCount > 1) {
                                    infoPanel.add(
                                        JLabel("Time Point: ${node.currentTimepoint + 1}/${node.timepointCount}"),
                                        "wrap"
                                    )
                                    infoPanel.add(JButton("Previous Tp").apply {
                                        this.addActionListener {
                                            if (node.currentTimepoint == 0) return@addActionListener

                                            node.previousTimepoint()
                                            uiModel.updateSelected()
                                        }
                                    },"")
                                    infoPanel.add(JButton("Next Tp").apply {
                                        this.addActionListener {
                                            if (node.currentTimepoint +1 == node.timepointCount) return@addActionListener

                                            node.nextTimepoint()
                                            uiModel.updateSelected()
                                        }
                                    },"wrap")
                                }
                                infoPanel.add(JButton("Delete Volume").apply {
                                    this.addActionListener {
                                        sliceManager.deleteStack(node)
                                        uiModel.selected = null
                                    }
                                },"wrap")
                            }
                            is SliceRenderNode -> {
                                infoPanel.add(JLabel("Position: ${node.spatial().position.toReadableString()}"),"wrap")
                                if (MicroscenerySettings.get(Settings.MMMicroscope.IsMicromanagerMicroscope,false)){
                                    infoPanel.add(JButton("Add to position list").apply {
                                        this.addActionListener{
                                            MicroManagerUtil.addPositionToPositionList(
                                                stageSpaceManager.hardware,
                                                node.name,
                                                node.spatial().position)
                                        }
                                    })
                                }
                                infoPanel.add(JButton("Go to").apply {
                                    this.addActionListener {
                                        stageSpaceManager.stagePosition = node.spatial().position
                                    }
                                },"wrap")
                                infoPanel.add(JButton("Delete Slice").apply {
                                    this.addActionListener {
                                        sliceManager.deleteSlice(node)
                                        uiModel.selected = null
                                    }
                                },"wrap")
                            }
                            else -> infoPanel.add(JLabel("type: ${node.javaClass.name}", SwingConstants.RIGHT), "shrink,wrap")
                        }
                    }
                    infoPanel.revalidate()
                }
            }
        }

        val commandPanel = JPanel(MigLayout())
        commandPanel.border = BorderFactory.createTitledBorder("Commands")

        (desktopCommands + customCommands).forEachIndexed {i,stageUICommand ->
            val name = stageUICommand.name
            val key = stageUICommand.key
            val command = stageUICommand.command
            val layoutConstrains = if (i%2==1)"wrap" else ""

            when {
                (command == null && key != null) -> {
                    commandPanel.add(JLabel("$name : $key"), layoutConstrains)
                }
                (command != null) -> {
                    val but = JButton(name+key?.let { " : $key" })
                    but.addActionListener { command.click(0, 0) }
                    commandPanel.add(but,layoutConstrains)
                }
            }
        }
        panel.add(commandPanel, "wrap")
    }

    fun stageKeyUI(inputHandler: InputHandler, cam: Camera) {
        fun imgSize() = stageSpaceManager.hardware.hardwareDimensions().imageSize
        listOf(
            "frame_forward" to {10}, "frame_back" to {10}, "frame_left" to {imgSize().x}, "frame_right" to {imgSize().x}, "frame_up" to {imgSize().y}, "frame_down"  to {imgSize().y}
        ).forEach { (name, speed) ->
            inputHandler.addBehaviour(
                name,
                MovementCommandLocalSpace(name.removePrefix("frame_"), { stageSpaceManager.focusManager.focusTarget }, cam, speed = {speed().toFloat()})
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
                { stageSpaceManager.focusManager.focusTarget },
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

    fun stageUI(base: DefaultScene, inputHandler: InputHandler?,msHub: MicrosceneryHub, customCommands: List<StageUICommand> = emptyList()) {
        base.extraPanel?.let { stageSwingUI(it,customCommands,msHub)}
        base.mainFrame?.pack()
        DesktopUI.initMouseSelection(inputHandler,msHub)

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