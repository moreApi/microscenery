package microscenery.example.microscope

import graphics.scenery.AmbientLight
import graphics.scenery.Box
import graphics.scenery.Mesh
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Switch
import graphics.scenery.controls.behaviours.WheelMenu
import microscenery.DefaultVRScene
import microscenery.MicroscenerySettings
import microscenery.UI.StageSpaceUI
import microscenery.VRUI.VRUIManager
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.micromanagerConnection.MMConnection
import microscenery.hardware.micromanagerConnection.MicromanagerWrapper
import microscenery.stageSpace.FrameGizmo
import microscenery.stageSpace.MicroscopeLayout
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f

class LocalMMSceneVR : DefaultVRScene() {

    lateinit var stageSpaceManager: StageSpaceManager

    override fun init() {
        super.init()

        MicroscenerySettings.set("Stage.minX", -100f)
        MicroscenerySettings.set("Stage.minY", -100f)
        MicroscenerySettings.set("Stage.minZ", -50f)
        MicroscenerySettings.set("Stage.maxX", 500f)
        MicroscenerySettings.set("Stage.maxY", 500f)
        MicroscenerySettings.set("Stage.maxZ", 250f)

        val stageStart = Vector3f()

        val hardware: MicroscopeHardware =
            MicromanagerWrapper(MMConnection(LocalMMScene.initLocalMMCoreFake()).apply { moveStage(stageStart, false) })
        stageSpaceManager = StageSpaceManager(
            hardware, scene, hub, addFocusFrame = true, scaleDownFactor = 200f,
            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
        )

        hullbox.visible = false

        scene.addChild(AmbientLight(0.25f))

        val p = Box()
        p.material().diffuse = Vector3f(0.5f)
        p.material().cullingMode = Material.CullingMode.FrontAndBack
        p.addAttribute(Grabable::class.java, Grabable())
        p.name = "women"
        val women = Mesh().readFrom("C:\\Users\\JanCasus\\repos\\microscenery\\models\\poster\\vr_women.obj")
        women.spatial().position.y -= 0.55f
        p.addChild(women)
        scene.addChild(p)
    }

    override fun inputSetup() {
        super.inputSetup()
        inputHandler?.let {
            StageSpaceUI(stageSpaceManager).stageKeyUI(it, cam)
        }

        VRUIManager.initBehavior(
            scene, hmd, inputHandler,
            customActions = WheelMenu(
                hmd, listOf(
                    Switch("Steering", false) { value ->
                        stageSpaceManager.focusTarget?.let {
                            if (value && it.mode != FrameGizmo.Mode.STEERING) {
                                it.mode = FrameGizmo.Mode.STEERING
                            } else {
                                it.mode = FrameGizmo.Mode.PASSIVE
                            }
                            logger.info("focusframe mode is now ${it.mode}")
                        }
                    },
                    Switch("Live", false) {
                        if (it)
                            stageSpaceManager.goLive()
                        else
                            stageSpaceManager.stop()
                    },
                )
            )
        ) {
            scene.find("women")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            LocalMMSceneVR().main()
        }
    }

}