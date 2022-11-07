package microscenery.example

import graphics.scenery.Mesh
import graphics.scenery.controls.behaviours.WheelMenu
import graphics.scenery.volumes.Volume
import microscenery.DefaultVRScene
import microscenery.VRUI.VRUIManager
import org.joml.Vector3f
import java.lang.Thread.sleep
import kotlin.concurrent.thread

class TAC1Example : DefaultVRScene("microscenery") {
//class Playground : DefaultScene(){

    override fun init() {
        super.init()

//        val volume = Volume.forNetwork(
//            Volume.VolumeFileSource(
//            Volume.VolumeFileSource.VolumePath.Given("""C:\Users\JanCasus\volumes\drosophila.xml"""),
//                Volume.VolumeFileSource.VolumeType.SPIM
//        ),hub)
//        val volume = Volume.forNetwork(
//            Volume.VolumeFileSource(
//            Volume.VolumeFileSource.VolumePath.Given("""C:\Users\JanCasus\Downloads\AA-Paw.nrrd"""),
//                Volume.VolumeFileSource.VolumeType.DEFAULT
//        ),hub)
//
//        volume.transferFunction = TransferFunction.ramp(0.1f, distance = 0.5f )
//        volume.transferFunction = TransferFunction().let { tf ->
//            // block TF
//            val start = 0.008f
//            val end = start + 0.01f
//            tf.addControlPoint(0.0f,0.0f)
//            tf.addControlPoint(start,0.0f)
//            tf.addControlPoint(start + 0.0015f,1.0f)
//            tf.addControlPoint(end,1.0f)
//            tf.addControlPoint(end + 0.0015f,0.0f)
//            tf
//        }


//        volume.spatial() {
//            scale.z = 4f
//            position = Vector3f(1f, 2.5f, 4f)
//        }
//
//        scene.addChild(volume)

        val cell = Mesh()
        cell.readFromOBJ(TAC1Example::class.java.getResource("models/cell.obj").file)
        cell.spatial {
            rotation.rotateLocalY(Math.PI.toFloat())
            scale = Vector3f(0.3f)
        }
        scene.addChild(cell)

        thread {
            while (true) {
                sleep(500)
                scene
            }
        }
    }

    override fun inputSetup() {
        super.inputSetup()

//        setupCameraModeSwitching(keybinding = "C")

        VRUIManager.initBehavior(
            scene, hmd, inputHandler, WheelMenu(
                hmd, listOf(
                )
            )
        ) {
            scene.findByClassname(Volume::class.simpleName!!).first() as Volume
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TAC1Example().main()
        }
    }
}