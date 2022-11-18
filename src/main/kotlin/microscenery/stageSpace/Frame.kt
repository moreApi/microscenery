package microscenery.stageSpace

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.signals.HardwareDimensions
import microscenery.toReadableString
import org.joml.Vector3f
import org.joml.Vector4f

open class Frame(
    hwd: HardwareDimensions,
    color: Vector3f? = null
) : RichNode("focus") {

    protected var stageMin: Vector3f = Vector3f()
    protected var stageMax: Vector3f = Vector3f(1f)

    protected val pivot: RichNode
    protected val beams: List<Box>

    // to make the warnings happy
    @Transient
    final override var update: ArrayList<() -> Unit> = ArrayList()

    init {
        // this is needed so VRGrab applies the correct scaling to the translation
        pivot = RichNode("scalePivot")
        this.addChild(pivot)

        val beamBase = Vector3f(.1f, .1f, 1f)
        val distanceFromCenter = Vector3f(0.55f)
        // beams
        beams = listOf(
            Vector3f(0f, 1f, 0f),
            Vector3f(0f, -1f, 0f),
            Vector3f(-1f, 0f, 0f),
            Vector3f(1f, 0f, 0f)
        ).map { posNorm ->
            // position
            val pos = distanceFromCenter * posNorm
            val beamDir = Vector3f(1.1f, 1.1f, 0f) - posNorm.absolute(Vector3f()) * 1.1f
            val beam = Box(beamBase + beamDir)
            beam.spatial().position = pos
            color?.let { beam.material().diffuse = it }
            pivot.addChild(beam)
            beam
        }

        val positionLabel = TextBoard()
        positionLabel.text = "0,0,0"
        positionLabel.name = "FramePositionLabel"
        positionLabel.transparent = 0
        positionLabel.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        positionLabel.backgroundColor = Vector4f(100f, 100f, 100f, 1.0f)
        positionLabel.spatial {
            position = Vector3f(-distanceFromCenter.x, distanceFromCenter.y + beamBase.y, 0f)
            scale = Vector3f(0.15f, 0.15f, 0.15f)
        }
        pivot.addChild(positionLabel)

        this.update += {
            positionLabel.text = spatial().position.toReadableString()
        }

        applyHardwareDimensions(hwd)
    }


    fun applyHardwareDimensions(hwd: HardwareDimensions) {
        pivot.spatialOrNull()?.scale =
            Vector3f(hwd.imageSize.x.toFloat() * hwd.vertexDiameter, hwd.imageSize.y.toFloat() * hwd.vertexDiameter, 1f)
        stageMin = hwd.stageMin
        stageMax = hwd.stageMax
    }
}