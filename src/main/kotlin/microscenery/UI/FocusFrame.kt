package microscenery.UI

import graphics.scenery.Box
import graphics.scenery.RichNode
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.*
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import microscenery.MicroscenerySettings
import microscenery.StageSpaceManager
import microscenery.nowMillis
import microscenery.signals.HardwareDimensions
import microscenery.toReadableString
import org.joml.Vector3f
import org.joml.Vector4f

class FocusFrame(
    val stageSpaceManager: StageSpaceManager,
    hwd: HardwareDimensions,
    var stageSteeringActive: Boolean = false
) : RichNode("focus") {

    private var stageMin: Vector3f = Vector3f()
    private var stageMax: Vector3f = Vector3f(1f)

    private val pivot: RichNode

    init {
        // this is needed so VRGrab applies the correct scaling to the translation
        pivot = RichNode("scalePivot")
        this.addChild(pivot)

        val beamBase = Vector3f(.1f, .1f, 1f)
        val distanceFromCenter = Vector3f(0.55f)
        // beams
        listOf(
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
            pivot.addChild(beam)

            // ui interaction
            beam.addAttribute(Grabable::class.java, Grabable(target = this, lockRotation = true))
            beam.addAttribute(Touchable::class.java, Touchable())
            beam.addAttribute(
                Pressable::class.java,
                PerButtonPressable(mapOf(OpenVRHMD.OpenVRButton.Trigger to SimplePressable(onRelease = {
                    stageSpaceManager.snapSlice()
                })))
            )
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

        var lastUpdate = 0L
        this.update += {

            spatial {
                val coerced = Vector3f()
                position.min(stageMax, coerced)
                coerced.max(stageMin)

                if (position != coerced) position = coerced

                if (stageSteeringActive
                    && position != stageSpaceManager.stagePosition
                    && lastUpdate + MicroscenerySettings.get("Stage.PositionUpdateRate",500) < nowMillis()
                ) {
                    stageSpaceManager.stagePosition = position
                    lastUpdate = nowMillis()
                }

                positionLabel.text = position.toReadableString()
            }
        }
        applyHardwareDimensions(hwd)
    }



    fun applyHardwareDimensions(hwd: HardwareDimensions) {
        pivot.spatialOrNull()?.scale = Vector3f(hwd.imageSize.x.toFloat(), hwd.imageSize.y.toFloat(), 1f)
        stageMin = hwd.stageMin
        stageMax = hwd.stageMax
    }
}