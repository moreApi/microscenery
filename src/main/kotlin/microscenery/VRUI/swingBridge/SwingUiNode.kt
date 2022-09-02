package microscenery.VRUI.swingBridge

import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Plane
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import microscenery.copy
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f

class SwingUiNode(val swingBridgeFrame: SwingBridgeFrame) : Plane(
    Vector3f(-0.5f,-0.5f,0f),
    Vector3f(-0.5f,0.5f,0f),
    Vector3f(0.5f,-0.5f,0f),
    Vector3f(0.5f,0.5f,0f)
) {

    var swingUiDimension = 0 to 0

    init {


        this.material().cullingMode = Material.CullingMode.None

        this.update += {
            updateUITexture()
            spatial().needsUpdate = true
        }
    }

    private fun updateUITexture() {
        val bimage = swingBridgeFrame.getScreen()
        val flipped = Image.createFlipped(bimage)
        val buffer = Image.bufferedImageToRGBABuffer(flipped)
        val final = Image(buffer, bimage.width, bimage.height)
        this.material {
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