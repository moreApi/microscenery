package anon.scenes.stageStudy.Orchestration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphics.scenery.Camera
import graphics.scenery.utils.lazyLogger
import anon.Agent
import anon.MicroscenerySettings
import anon.scenes.stageStudy.StageViewerStudy2D
import anon.scenes.stageStudy.StageViewerStudy3D
import anon.scenes.stageStudy.StageViewerStudyVR
import anon.scenes.stageStudy.StudySpatialLogger
import anon.showMessage2
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.security.InvalidParameterException
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.system.exitProcess

class TrialCoordinator {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val mapper = jacksonObjectMapper()

    var studySpatialLogger: StudySpatialLogger? = null
    var userPrompter: FinishMessageDisplayer? = null

    val configFile: File
    val trialConfig: TrialConfig
    val case: Case
    var timeLimitAgent: Agent? = null

    var caseSuccess = false

    init {
        logger.info("Starting trial Coordinator")

        val configFilePath = MicroscenerySettings.get("Study.TrialConfigFile", "trialConfig1.json")
        logger.info("Loading $configFilePath trial config")
        configFile = File(configFilePath)

        val json = configFile.readText()
        trialConfig = mapper.readValue<TrialConfig>(json)

        val c = trialConfig.cases.firstOrNull { !it.done }
        if (c == null) {
            logger.warn("all cases are done")
            exitProcess(1)
        }
        case = c
    }

    fun loadCase() {
        logger.info("starting case $case")
        val scenario = when {
            case.scenario.tube != null -> case.scenario.tube.toScenario()
            case.scenario.axon != null -> case.scenario.axon.toScenario()
            else -> throw InvalidParameterException("simulation param null")
        }
        when (case.modality) {
            Modality.VR -> StageViewerStudyVR(scenario, this).main()
            Modality.ThreeD -> StageViewerStudy3D(scenario, this).main()
            Modality.TwoD -> StageViewerStudy2D(scenario, this).main()
        }
    }

    fun startCase(studySpatialLogger: StudySpatialLogger, userPrompter: FinishMessageDisplayer) {
        this.studySpatialLogger = studySpatialLogger
        this.userPrompter = userPrompter

        if (trialConfig.timeLimitPerCaseMS > 0) {
            timeLimitAgent = object : Agent() {
                var startTime = System.currentTimeMillis()
                init {
                    startAgent()
                }

                override fun onLoop() {
                    Thread.sleep(200)
                    if (caseSuccess) caseFinished(false)

                    if (startTime + trialConfig.timeLimitPerCaseMS < System.currentTimeMillis()){
                        caseFinished(true)
                    }

                }
            }
        }
        studySpatialLogger.logEvent("StartCase", listOf(trialConfig.name, mapper.writeValueAsString(case)))
    }

    fun caseAllHit(){
        caseSuccess = true
    }

    fun caseFinished(byTimeLimit: Boolean) {
        studySpatialLogger?.logEvent("case finished", listOf(if (byTimeLimit) "TimeLimit" else "AllHit"))
        val indexOfCase = trialConfig.cases.indexOf(case)
        val nextCase = trialConfig.cases.getOrNull(indexOfCase + 1)
        userPrompter?.displayFinishDialog(byTimeLimit, nextCase?.modality)
        studySpatialLogger?.close()?.join() // let it finish writing the log
        case.done = true
        writeConfig(trialConfig, configFile)
        exitProcess(0)
    }

    /**
     * Used to display finish message to user
     */
    class FinishMessageDisplayer(val cam: Camera, val distance: Float = 0.5f) {

        fun displayFinishDialog(becauseTimeLimit: Boolean, nextModality: Modality?) {
            cam.showMessage2(
                listOf(
                    if (becauseTimeLimit) "Time Limit reached." else "Success! All targets marked",
                    when (nextModality) {
                        Modality.VR -> "Loading next case in VR mode"
                        Modality.ThreeD -> "Loading next case in 3D desktop mode"
                        Modality.TwoD -> "Loading next case in 2D desktop mode"
                        null -> "Finished Session! Good Bye."
                    }
                ),
                duration = 5000, distance = distance
            )
            Thread.sleep(5000)
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                TrialCoordinator().loadCase()
            } catch (e: Error){
                e.printStackTrace()
                // The dialog box should show up in the task bar. That's why we create a frame for it.
                val frame = JFrame("Ready Check")
                frame.isUndecorated = true
                frame.isVisible = true
                frame.setLocationRelativeTo(null)
                frame.setAlwaysOnTop(true)
                JOptionPane.showMessageDialog(
                    frame,
                    "Please activate VR headset, establish Quest Link and open SteamVR."
                )
                exitProcess(0)
            }
        }

        fun writeConfig(config: TrialConfig, file: File) {
            val mapper = jacksonObjectMapper()
            val writer = BufferedWriter(FileWriter(file))
            writer.write(mapper.writeValueAsString(config))
            writer.close()
        }
    }

}

