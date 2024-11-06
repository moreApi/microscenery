package microscenery.scenes.stageStudy.Orchestration

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import microscenery.scenes.stageStudy.StageViewerStudy2D
import microscenery.scenes.stageStudy.StageViewerStudy3D
import java.security.InvalidParameterException
import kotlin.system.exitProcess

class TrialCoordinator {
    val mapper = jacksonObjectMapper()
    init {
        val config = readConfig()
        val case = config.cases.firstOrNull { !it.done }
        if (case == null){
            println("all cases are done")
            exitProcess(1)
        } else {
            startCase(case)
        }
    }

    fun writeConfig(config: TrialConfig) {
        val f = File("trialConfig1.json")
        val writer = BufferedWriter(FileWriter(f))
        writer.write(mapper.writeValueAsString(config))
        writer.close()
    }

    private fun readConfig(): TrialConfig{
        val f = File("trialConfig1.json")
        val json = f.readText()

        return mapper.readValue<TrialConfig>(json)
    }

    fun quitCase() {
        exitProcess(0)
    }

    fun startCase(case: Case){
        val scenario = when{
            case.simulation.tube != null -> case.simulation.tube.toScenario()
            case.simulation.axion != null -> case.simulation.axion.toScenario()
            else -> throw InvalidParameterException("simulation param null")
        }
        when (case.modality){
            Modality.VR -> StageViewerStudy3D(scenario,vr = true,this)
            Modality.ThreeD -> StageViewerStudy3D(scenario,vr = false,this)
            Modality.TwoD -> StageViewerStudy2D(scenario,this)
        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            TrialCoordinator()
        }
    }

}

object WriteAConfig{
    @JvmStatic
    fun main(args: Array<String>) {
        val case = Case(Modality.VR,Simulation(tube = Simulation.Tube(123)))
        val case2 = Case(Modality.TwoD,Simulation(axion= Simulation.Axion( 3824716,
            listOf(0f, -0.5f, 0f).toFloatArray(),
            350f,
            3,
            1,
            3)))
        val config = TrialConfig("test trial config", listOf(case,case2))

        TrialCoordinator().writeConfig(config)
    }
}