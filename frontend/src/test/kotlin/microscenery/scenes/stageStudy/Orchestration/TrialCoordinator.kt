package microscenery.scenes.stageStudy.Orchestration

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphics.scenery.utils.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.scenes.stageStudy.StageViewerStudy2D
import microscenery.scenes.stageStudy.StageViewerStudy3D
import microscenery.scenes.stageStudy.StudySpatialLogger
import java.security.InvalidParameterException
import kotlin.system.exitProcess

class TrialCoordinator {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val mapper = jacksonObjectMapper()
    var studySpatialLogger: StudySpatialLogger? = null

    val configFile: File
    val config: TrialConfig
    val case: Case

    init {
        logger.info("Starting trial Coordinator")

        val configFilePath = MicroscenerySettings.get("Study.TrialConfigFile","trialConfig1.json")
        logger.info("Loading $configFilePath trial config")
        configFile = File(configFilePath)

        val json = configFile.readText()
        config = mapper.readValue<TrialConfig>(json)

        val c = config.cases.firstOrNull { !it.done }
        if (c == null){
            logger.warn("all cases are done")
            exitProcess(1)
        }
        case = c
    }

    fun startExperiment(studySpatialLogger: StudySpatialLogger){
        this.studySpatialLogger = studySpatialLogger

        studySpatialLogger.logEvent("StartCase", listOf(config.name,mapper.writeValueAsString(case)))
    }

    fun caseFinished() {
        studySpatialLogger?.close()?.join() // let it finish writing the log
        case.done = true
        writeConfig(config,configFile)
        exitProcess(0)
    }

    fun startCase(){
        logger.info("starting case $case")
        val scenario = when{
            case.scenario.tube != null -> case.scenario.tube.toScenario()
            case.scenario.axion != null -> case.scenario.axion.toScenario()
            else -> throw InvalidParameterException("simulation param null")
        }
        when (case.modality){
            Modality.VR -> StageViewerStudy3D(scenario,vr = true,this).main()
            Modality.ThreeD -> StageViewerStudy3D(scenario,vr = false,this).main()
            Modality.TwoD -> StageViewerStudy2D(scenario,this).main()
        }
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            TrialCoordinator().startCase()
        }

        fun writeConfig(config: TrialConfig, file: File) {
            val mapper = jacksonObjectMapper()
            val writer = BufferedWriter(FileWriter(file))
            writer.write(mapper.writeValueAsString(config))
            writer.close()
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
        val config = TrialConfig("test trial config", listOf(case2))

        TrialCoordinator.writeConfig(config, File("trialConfig1.json") )
    }
}