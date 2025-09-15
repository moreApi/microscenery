package anon.scenes.network

import graphics.scenery.Light
import graphics.scenery.utils.Wiggler
import graphics.scenery.volumes.Colormap
import anon.DefaultScene
import anon.MicrosceneryHub
import anon.MicroscenerySettings
import anon.Settings
import anon.UI.CrossRayToPosBehavior
import anon.UI.StageSpaceUI
import anon.UI.StageUICommand
import anon.VRUI.PointCloudAblationTool.Ink
import anon.hardware.MicroscopeHardware
import anon.network.RemoteMicroscopeClient
import anon.stageSpace.StageSpaceManager
import org.joml.Vector3f
import org.zeromq.ZContext

class RemoteZenPMScene : DefaultScene(withSwingUI = true) {
    lateinit var  stageSpaceManager: StageSpaceManager
    val msHub = MicrosceneryHub(hub)

    override fun init() {
        super.init()
        MicroscenerySettings.set(Settings.StageSpace.HideFocusFrame,true)
        MicroscenerySettings.set(Settings.StageSpace.HideFocusTargetFrame,true)

        cam.spatial().position = Vector3f(0f, 0f, 5f)
        val zContext = ZContext()
        val client = RemoteMicroscopeClient(zContext = zContext, nonMicroscopeMode = false)

        val hardware: MicroscopeHardware = client
        stageSpaceManager = StageSpaceManager(hardware, scene, msHub)

        //for nice cut pictures
        stageSpaceManager.scene.findByClassname("Light").forEach { (it as Light).intensity *= 0.25f }
        stageSpaceManager.sliceManager.stacks.firstOrNull()?.volume?.colormap = Colormap.get("grays")

    }

    fun placeAblationInk(pos: Vector3f){
        val pointColor = Vector3f(1f)
        val ink = Ink(MicroscenerySettings.get(Settings.Ablation.SizeUM, 8f) * 0.5f, pointColor, null)
        ink.spatial().let {
            it.position = pos
        }
        scene.addChild(ink)
        ink.spatial().updateWorld(false,true)

        val posInStageSpace = stageSpaceManager.worldToStageSpace(ink.spatial().worldPosition())
        val coerced = stageSpaceManager.hardware.hardwareDimensions().coercePosition(posInStageSpace, null, true)

        if (posInStageSpace != coerced) {
            //ink is out of stage space bounds, wiggle in protest
            Wiggler(ink, 0.01f, 300)
            scene.removeChild(ink)
            return
        }

        //ink.spatial().position = ink.spatial().worldPosition()
        stageSpaceManager.worldToStageSpace(ink.spatial())
        ink.spatial().scale = Vector3f(1f)

        scene.removeChild(ink)
        stageSpaceManager.stageRoot.addChild(ink)
    }

    override fun inputSetup() {
        super.inputSetup()
        StageSpaceUI(stageSpaceManager).stageUI(this,inputHandler, msHub, listOf(
            StageUICommand("ablateZen compose",null){_,_ ->
                stageSpaceManager.ablationManager.composeAblation()
            },
            StageUICommand("ablateZen scrap",null){_,_ ->
                stageSpaceManager.ablationManager.scrapAblation()
            },
            StageUICommand("ablateZen execute",null){_,_ ->
                stageSpaceManager.ablationManager.executeAblation()
            }
        ))


        val crossRay = CrossRayToPosBehavior(scene.activeObserver!!){
            placeAblationInk(it)
        }
        inputHandler!!.addBehaviour("crossRay", crossRay)
        inputHandler!!.addKeyBinding("crossRay","G")

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RemoteZenPMScene().main()
        }
    }
}