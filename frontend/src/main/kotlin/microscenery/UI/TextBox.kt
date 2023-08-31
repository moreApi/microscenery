package microscenery.UI

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.primitives.TextBoard
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * Text with a Box behind it.
 */
class TextBox(text: String, var padding: Float = 0.2f, var minSize: Float = 0f, thickness: Float = 0.5f):
    RichNode("TextBox"), Ui3DElement {
    val box = Box(Vector3f(1f, 1f, thickness))
    val board = TextBoard()

    var text by board::text
    var fontColor by board::fontColor
    var backgroundColor by box.material()::diffuse

    override var width = 0f
        private set

    init {
        board.text = text
        board.name = "$text TextBox"
        board.transparent = 1
        board.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        box.material().diffuse = Vector3f(1f)
        box.spatial().position.z = box.sizes.z * -0.5f - 0.05f

        this.addChild(board)
        this.addChild(box)

        var textGeom = board.geometry().vertices
        this.update +=
            {
                if (textGeom != board.geometry().vertices) {
                    val bv = board.geometry().vertices.duplicate().clear()
                    var maxX = minSize
                    while (bv.hasRemaining()) {
                        maxX = java.lang.Float.max(bv.get(), maxX)
                        bv.get()
                        bv.get()
                    }

                    box.spatial {
                        scale.x = maxX + padding
                        position = Vector3f(
                            maxX / 2f,
                            0.43f,
                            box.sizes.z * -0.5f - 0.05f
                        )
                        needsUpdate = true
                    }
                    width = maxX
                    textGeom = board.geometry().vertices
                }
            }
    }
}