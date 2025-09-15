package anon.scenes.stageStudy.Orchestration

import java.io.File

object WriteAConfig {
    @JvmStatic
    fun main(args: Array<String>) {
        val cases = listOf(
//            Case(Modality.ThreeD, Scenario(tube = Scenario.Tube(123))),
            Case(
                Modality.TwoD, Scenario(
                    axon = Scenario.Axon(
                        3824716,
                        listOf(0f, -0.5f, 0f).toFloatArray(),
                        350f,
                        3,
                        1,
                        3
                    )
                )
            ),
//            Case(
//                Modality.VR, Scenario(tube = Scenario.Tube(13))
//            ),
//            Case(
//                Modality.VR, Scenario(
//                    axon = Scenario.Axon(
//                        -740995428542201163,
//                        listOf(0f,-0.75f,0f).toFloatArray(),
//                        250f,
//                        4,
//                        1,
//                        2
//                    )
//                )
//            ),
//            Case(Modality.TwoD, Scenario(tube = Scenario.Tube(12))),
//            Case(
//                Modality.ThreeD, Scenario(
//                    axon = Scenario.Axon(
//                        -8036747134749984232,
//                        listOf(0f,-0.75f,0f).toFloatArray(),
//                        250f,
//                        4,
//                        1,
//                        2
//                    )
//                )
//            )
        )

                //val config = TrialConfig("test trial config", cases, timeLimitPerCaseMS = 2000)
        val config = TrialGenerator.generateConfig()
        println("Writing config to file")
        TrialCoordinator.writeConfig(config, File("trialConfig1.json"))
    }
}