package microscenery.scenes.stageStudy.Orchestration

import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

object TrialGenerator {

    fun generateConfig(): TrialConfig {
        val axonParams: List<Scenario.Axon> = listOf(
            Scenario.Axon(
                3824716,
                listOf(0f, -0.5f, 0f).toFloatArray(),
                350f,
                3,
                1,
                3
            ),
            Scenario.Axon(
                -8036747134749984232,
                listOf(0f, -0.75f, 0f).toFloatArray(),
                250f,
                4,
                1,
                2
            ),
            Scenario.Axon(
                -740995428542201163,
                listOf(0f, -0.75f, 0f).toFloatArray(),
                250f,
                4,
                1,
                2
            ),
            Scenario.Axon(
                1426669727756891990,
                listOf(0f, -0.75f, 0f).toFloatArray(),
                250f,
                4,
                1,
                2
            ),
            Scenario.Axon(
                1426669727756891990,
                listOf(0f, -0.75f, 0f).toFloatArray(),
                250f,
                4,
                1,
                2
            ),
            Scenario.Axon(
                1426669727756891990,
                listOf(0f, -0.75f, 0f).toFloatArray(),
                250f,
                4,
                1,
                2
            )
            //todo replace last 2
        ).shuffled()

        val tubeParams: List<Scenario.Tube> = listOf(
            Scenario.Tube(123),
            Scenario.Tube(37),
            Scenario.Tube(42),
            Scenario.Tube(69),
            Scenario.Tube(-1),
            Scenario.Tube(314159265358979)
        ).shuffled()

        val sdf = SimpleDateFormat("yyyy-MM-dd--hh-mm-ss")
        val currentDate = sdf.format(Date())

        return TrialConfig("trial of $currentDate", cases =
        generateRun(axonParams.take(3),tubeParams.take(3))
                + generateRun(axonParams.takeLast(3),tubeParams.takeLast(3)))
    }

    private enum class ScenarioType { Tube, Axon }

    fun generateRun(axonParams: List<Scenario.Axon>, tubeParams: List<Scenario.Tube>): List<Case> {
        val leftOverAxons = axonParams.shuffled().iterator()
        val leftOverTubes = tubeParams.shuffled().iterator()

        return listOf(
            Modality.TwoD to ScenarioType.Tube,
            Modality.TwoD to ScenarioType.Axon,
            Modality.ThreeD to ScenarioType.Tube,
            Modality.ThreeD to ScenarioType.Axon,
            Modality.VR to null
        )
            .shuffled()
            .flatMap { c ->
                if (c.first != Modality.VR) return@flatMap listOf(c)
                // expand VR into two cases
                val tubeFirst = Random.nextBoolean()
                if (tubeFirst) {
                    return@flatMap listOf(Modality.VR to ScenarioType.Tube,Modality.VR to ScenarioType.Axon)
                } else {
                    return@flatMap listOf(Modality.VR to ScenarioType.Axon,Modality.VR to ScenarioType.Tube)
                }
            }.map {
                val (modality, scenario) = it
                when (scenario) {
                    ScenarioType.Tube -> Case(modality, Scenario(tube = leftOverTubes.next()))
                    ScenarioType.Axon -> Case(modality, Scenario(axon = leftOverAxons.next()))
                    null -> throw IllegalStateException()
                }
            }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        generateConfig().cases.forEach {
            println(it.modality.toString()+it.scenario)
        }
    }
}