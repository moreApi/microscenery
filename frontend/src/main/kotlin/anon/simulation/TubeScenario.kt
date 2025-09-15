package anon.simulation

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import fromScenery.utils.extensions.times
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.primitives.Cylinder
import graphics.scenery.volumes.TransferFunction
import anon.copy
import anon.nextVector3f
import anon.simulation.StageSimulation.Companion.hideMaterial
import anon.stageSpace.FocusManager
import anon.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class TubeScenario(val randomSeed: Long = 1337, val radius: Float = 350f, val roiHeight: Float = 600f) :
    StageSimulation.Scenario {
    val random = Random(randomSeed)
    val roiPos = random.nextVector3f()

    override val name = "many targets"

    override fun generate(stageSpaceManager: StageSpaceManager, stageSpaceSize: Float): List<Vector3f> {
        val stageRoot = stageSpaceManager.stageRoot

        val tf = TransferFunction.ramp(0.0f, 1.0f, 0.5f)
        tf.controlPoints().first().factor = 0.04f
        stageSpaceManager.sliceManager.transferFunctionManager.transferFunction = tf

        roiPos.y = roiPos.y * (stageSpaceSize - roiHeight) + roiHeight / 2
        roiPos.x = roiPos.x * stageSpaceSize * 0.2f + stageSpaceSize / 2
        roiPos.z = roiPos.z * stageSpaceSize * 0.2f + stageSpaceSize / 2

        val paddedStageSpaceSize = stageSpaceSize * 1.2f //to include padding because of estimated image size
        if (roiPos.y + roiHeight / 2 < paddedStageSpaceSize) {
            // add extension tube from the top
            val pos = roiPos.copy()
            pos.y = (paddedStageSpaceSize + roiPos.y + roiHeight / 2f) / 2
            val height = (paddedStageSpaceSize - pos.y) * 2
            tube(stageRoot, pos, radius, height)
        }

        tube(stageRoot, roiPos, radius, roiHeight)
        return generateTargetPositions(radius, roiHeight, roiPos, 15)
    }

    fun autoExplore(stageSpaceManager: StageSpaceManager, imageSize: Int) {
        (stageSpaceManager.hardware as? SimulationMicroscopeHardware)?.fastMode = true

        fun roundToTen(f: Float, up: Boolean): Float = ((f / 10f) + if (up) 1 else 0).toInt() * 10f

        val min = roiPos - Vector3f(radius, roiHeight / 2, radius) * 1.2f
        min.z = roundToTen(min.z, false)
        val max = roiPos + Vector3f(radius, roiHeight / 2, radius) * 1.2f
        max.z = roundToTen(max.z, true)

        stageSpaceManager.focusManager.mode = FocusManager.Mode.PASSIVE
        stageSpaceManager.stop()
        stageSpaceManager.sync()
        stageSpaceManager.exploreCubeStageSpace(min, max, Vector3f(imageSize * 1.2f, imageSize * 1.2f, 50f))
        stageSpaceManager.sync()
        stageSpaceManager.goLive()
        stageSpaceManager.focusManager.mode = FocusManager.Mode.STEERING
        (stageSpaceManager.hardware as? SimulationMicroscopeHardware)?.fastMode = false
    }

    private fun tube(stageRoot: HasSpatial, position: Vector3f, radius: Float , height: Float) {
        Cylinder(radius * 0.95f, height, 16).let { cy ->
            cy.name = "tube"
            CylinderSimulatable.addTo(cy, stageRoot.spatial()).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = position + Vector3f(0f, -height * 0.5f, 0f)
            stageRoot.addChild(cy)
        }

        Cylinder(radius, height, 16).let { cy ->
            cy.name = "tube"
            CylinderSimulatable.addTo(cy, stageRoot.spatial()).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = position + Vector3f(0f, -height * 0.5f, 0f)
            stageRoot.addChild(cy)
        }
    }

    private fun generateTargetPositions(
        radius: Float,
        height: Float,
        offset: Vector3f,
        amount: Int
    ): MutableList<Vector3f> {
        val targetPositions = mutableListOf<Vector3f>()
        while (targetPositions.size < amount) {
            val a = random.nextFloat()
            val r = radius * 0.5f + radius * random.nextFloat() * 0.3f
            val new = Vector3f(
                r * cos(a * 2f * Math.PI.toFloat()),
                (random.nextFloat() - 0.5f) * height,
                r * sin(a * 2f * Math.PI.toFloat())
            ) + offset

            val targetSize = 30f
            if (targetPositions.any { (it - new).length() < targetSize * 3 })
                continue
            else {
                targetPositions.add(new)
            }
        }
        return targetPositions
    }
}