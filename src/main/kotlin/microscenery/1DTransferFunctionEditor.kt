package microscenery

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.Sphere
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Touchable
import graphics.scenery.primitives.Line
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import org.joml.Vector3f
import tpietzsch.example2.VolumeViewerOptions
import kotlin.concurrent.thread

class LineBetweenNodes2(var from: Spatial, var to: Spatial, transparent: Boolean = false, simple: Boolean = false) :
    Line(capacity = 3, transparent, simple) {

    init {

        addPoint(Vector3f())
        addPoint(Vector3f(0f, 1f, 0f))

        update.add {
            if (!visible) {
                return@add
            }
            spatial() {
                val p1 = from.worldPosition(Vector3f())
                val p2 = to.worldPosition(Vector3f())
                orientBetweenPoints(p1, p2)
                scale = Vector3f(p1.distance(p2)) //todo: times the inverse of world
                position = p1 - (parent?.spatialOrNull()?.worldPosition() ?: Vector3f())
            }
        }
    }
}

/**
 * It goes stale
 */
class RottenTransferFunction(): TransferFunction(){
    fun setStale() { stale = true }
}

class TransferFunction1DEditor : RichNode("Transfer function editor") {

    val start = Sphere(0.1f)
    val low = Sphere(0.1f)
    val high = Sphere(0.1f)
    val end = Sphere(0.1f)

    val transferFunction = RottenTransferFunction()
    val cpStart = transferFunction.addControlPoint(0.0f, 0.0f)
    val cpLow = transferFunction.addControlPoint(0.25f, 0.0f)
    val cpHigh = transferFunction.addControlPoint(0.75f, 1f)
    var cpEnd = transferFunction.addControlPoint(1f,1f)

    init {
        val background = Box(Vector3f(2f, 1f, 0.1f))
        background.spatial().position = background.sizes * 0.5f + Vector3f(0f, 0f, -0.2f)
        background.material().diffuse = Vector3f(0.3f, 0.3f, 1f)

        start.addAttribute(Grabable::class.java,
            Grabable(onDrag = {
                start.spatial{
                    position.x = 0f
                    position.y = position.y.coerceIn(0f, 1f)
                    position.z = 0f
                    needsUpdate = true
                }
                this.updateTransferFunction()
            }, lockRotation = true)
        )

        low.spatial().position = Vector3f(0.5f, 0f, 0f)
        low.addAttribute(Grabable::class.java,
            Grabable(onDrag = {
                low.spatial {
                    position.x = position.x.coerceIn(0f, high.spatial().position.x)
                    position.y = position.y.coerceIn(0f, 1f)
                    position.z = 0f
                    needsUpdate = true
                }
                this.updateTransferFunction()
            }, lockRotation = true)
        )
        this.addChild(LineBetweenNodes2(start.spatial(), low.spatial(), simple = true))

        high.spatial().position = Vector3f(1.5f, 1f, 0f)
        high.addAttribute(Grabable::class.java,
            Grabable(onDrag = {
                high.spatial {
                    position.x = position.x.coerceIn(low.spatial().position.x, 2f)
                    position.y = position.y.coerceIn(0f, 1f)
                    position.z = 0f
                    needsUpdate = true
                }
                this.updateTransferFunction()
            }, lockRotation = true)
        )
        this.addChild(LineBetweenNodes2(low.spatial(), high.spatial(), simple = true))

        end.spatial().position = Vector3f(2f, 1f, 0f)
        end.addAttribute(Grabable::class.java,
            Grabable(onDrag = {
                end.spatial {
                    position.x = 2f
                    position.y = position.y.coerceIn(0f, 1f)
                    position.z = 0f
                    needsUpdate = true
                }
                this.updateTransferFunction()
            }, lockRotation = true)
        )
        this.addChild(LineBetweenNodes2(high.spatial(), end.spatial(), simple = true))

        listOf(background, start, low, high, end).forEach {
            this.addChild(it)
            this.addAttribute(Touchable::class.java, Touchable())
        }

        thread {
            while (true) {
                val t = (System.currentTimeMillis() / 100) % 100
                low.spatial().position = Vector3f(0.1f + (t / 100f), 0.5f, 0f)
                updateTransferFunction()
            }
        }
    }

    private fun updateTransferFunction() {
        start.spatial {
            cpStart.factor = position.y
        }
        low.spatial {
            cpLow.value = position.x / 2f
            cpLow.factor = position.y
        }
        high.spatial {
            cpHigh.value = position.x / 2f
            cpHigh.factor = position.y
        }
        end.spatial {
            cpEnd.factor = position.y
        }
        transferFunction.setStale()
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DefaultScene({ scene, hub ->
                val tfe = TransferFunction1DEditor()
                scene.addChild(tfe)
                val volume = Volume.fromXML("""C:\Users\JanCasus\volumes\drosophila.xml""",hub, VolumeViewerOptions())
                volume.transferFunction = tfe.transferFunction
                scene.addChild(volume)
            }).main()
        }
    }
}