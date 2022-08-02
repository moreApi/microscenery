package microscenery.VRUI.swingBridge

import copy
import graphics.scenery.primitives.Plane
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.concurrent.thread

class SwingUiNode(val swingBridgeFrame: SwingBridgeFrame, val refreshRate: Long = 100) : Plane(
    Vector3f(-0.5f,-0.5f,0f),
    Vector3f(-0.5f,0.5f,0f),
    Vector3f(0.5f,-0.5f,0f),
    Vector3f(0.5f,0.5f,0f)
) {

    val swingUiNode = this

    var swingUiDimension = 0 to 0


    init {

        val backSide = Plane(Vector3f(0.5f))
        backSide.spatial().rotation = Quaternionf().rotationY(Math.PI.toFloat())
        backSide.material().diffuse = Vector3f(0.5f)
        this.addChild(backSide)

        thread {
            while (true) {
                updateUITexture()
                Thread.sleep(refreshRate)
            }
        }
    }

    private fun updateUITexture() {
        val bimage = swingBridgeFrame.getScreen()
        val flipped = Image.createFlipped(bimage)
        val buffer = Image.bufferedImageToRGBABuffer(flipped)
        val final = Image(buffer, bimage.width, bimage.height)
        swingUiNode.material {
            textures["diffuse"] = Texture.fromImage(final)
        }
        swingUiDimension = bimage.width to bimage.height
    }

    fun click(wPos: Vector3f) {

        val hitPosModel = Vector4f(wPos, 1f).mul(this.spatial().model.copy().invert())
//            println("${hitPosModel.x},${hitPosModel.y},${hitPosModel.z},${hitPosModel.w},")

        val swingX = (hitPosModel.x + 0.5f) * swingUiDimension.first
        val swingY = swingUiDimension.second - (hitPosModel.y + 0.5f) * swingUiDimension.second
        println("Cliky at $swingX : $swingY")
        swingBridgeFrame.click(swingX.toInt(), swingY.toInt())
    }

}