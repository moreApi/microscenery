package microscenery.simulation

import fromScenery.utils.extensions.minus
import fromScenery.utils.extensions.plus
import graphics.scenery.attribute.spatial.HasSpatial
import graphics.scenery.primitives.Cylinder
import microscenery.copy
import microscenery.nextVector3f
import microscenery.simulation.StageSimulation.Companion.hideMaterial
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class TubeScenario(val randomSeed: Long = 1337, val radius: Float = 150f, val roiHeight: Float = 600f) :
    StageSimulation.Scenario {
    val random = Random(randomSeed)

    override fun generate(stageSpaceManager: StageSpaceManager, stageSpaceSize: Float): List<Vector3f> {
        val stageRoot = stageSpaceManager.stageRoot

        val roiPos = random.nextVector3f()
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
        return generateTargetPositions(radius, roiHeight, roiPos,5)
    }

    private fun tube(stageRoot: HasSpatial, position: Vector3f, radius: Float = 200f, height: Float = 400f) {
        Cylinder(radius * 0.95f, height, 16).let { cy ->
            CylinderSimulatable.addTo(cy, stageRoot.spatial()).maxIntensity = 3000
            cy.hideMaterial()
            cy.spatial().position = position + Vector3f(0f, -height * 0.5f, 0f)
            stageRoot.addChild(cy)
        }

        Cylinder(radius, height, 16).let { cy ->
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
            val new = Vector3f(
                radius * 0.7f * cos(a * 2f * Math.PI.toFloat()),
                (random.nextFloat() - 0.5f) * height,
                radius * 0.7f * sin(a * 2f * Math.PI.toFloat())
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