package microscenery.scenes.stageStudy.Orchestration

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphics.scenery.utils.lazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.scenes.stageStudy.StageViewerStudy2D
import microscenery.scenes.stageStudy.StageViewerStudy3D
import microscenery.scenes.stageStudy.StudySpatialLogger
import java.security.InvalidParameterException
import javax.swing.JOptionPane
import kotlin.system.exitProcess

class TrialCoordinator {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val mapper = jacksonObjectMapper()
    var studySpatialLogger: StudySpatialLogger? = null

    val configFile: File
    val trialConfig: TrialConfig
    val case: Case
    var timeLimitAgent: Agent? = null

    init {
        logger.info("Starting trial Coordinator")

        val configFilePath = MicroscenerySettings.get("Study.TrialConfigFile","trialConfig1.json")
        logger.info("Loading $configFilePath trial config")
        configFile = File(configFilePath)

        val json = configFile.readText()
        trialConfig = mapper.readValue<TrialConfig>(json)

        val c = trialConfig.cases.firstOrNull { !it.done }
        if (c == null){
            logger.warn("all cases are done")
            exitProcess(1)
        }
        case = c
    }

    fun loadCase(){
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

    fun startCase(studySpatialLogger: StudySpatialLogger){
        this.studySpatialLogger = studySpatialLogger

        if (trialConfig.timeLimitPerCaseMS > 0){
            timeLimitAgent = object : Agent(){
                init {
                    startAgent()
                }
                override fun onLoop() {
                    Thread.sleep(trialConfig.timeLimitPerCaseMS)
                    caseFinished(true)
                }
            }
        }
        studySpatialLogger.logEvent("StartCase", listOf(trialConfig.name,mapper.writeValueAsString(case)))
    }

    fun caseFinished(byTimeLimit: Boolean) {
        studySpatialLogger?.logEvent("case finished", listOf(if (byTimeLimit) "TimeLimit" else "AllHit"))
        JOptionPane.showMessageDialog(null,
            if (byTimeLimit) "Time Limit reached. Please continue to next case." else "Success! All targets marked!")
        studySpatialLogger?.close()?.join() // let it finish writing the log
        case.done = true
        writeConfig(trialConfig,configFile)
        exitProcess(0)
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            TrialCoordinator().loadCase()
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
        val case = Case(Modality.VR,Scenario(tube = Scenario.Tube(123)))
        val case2 = Case(Modality.TwoD,Scenario(axion= Scenario.Axion( 3824716,
            listOf(0f, -0.5f, 0f).toFloatArray(),
            350f,
            3,
            1,
            3)))
        val config = TrialConfig("test trial config", listOf(case2), timeLimitPerCaseMS = 10000)

        TrialCoordinator.writeConfig(config, File("trialConfig1.json") )
    }
}