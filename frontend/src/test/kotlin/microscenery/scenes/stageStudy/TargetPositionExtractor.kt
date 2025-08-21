package microscenery.scenes.stageStudy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import microscenery.DefaultScene
import microscenery.MicrosceneryHub
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.scenes.stageStudy.Orchestration.TrialGenerator
import microscenery.simulation.StageSimulation
import microscenery.stageSpace.StageSpaceManager
import org.joml.Vector3f
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


class TargetPositionExtractor(
) : DefaultScene(name= "Study", withSwingUI = !true, width = 1200, height = 1200, VR = false) {
    val msHub = MicrosceneryHub(hub)
    lateinit var stageSpaceManager: StageSpaceManager

    lateinit var stageSimulation: StageSimulation
    lateinit var studyLogger: StudySpatialLogger

    init {
        MicroscenerySettings.set("Stage.precisionXY", 1f)
        MicroscenerySettings.set("Stage.precisionZ", 3f)
        MicroscenerySettings.set(Settings.UI.ShowSelectionIndicator, false)
        MicroscenerySettings.set(Settings.VRUI.LockRotationDefault, true)
        MicroscenerySettings.set(Settings.StageSpace.ShowFocusPositionLabel, false)
    }

    override fun init() {
        super.init()
        logger.info("Starting demo hw scene")
        cam.spatial().position = Vector3f(0f, -0f, 2f)

        stageSimulation = StageSimulation()
        stageSpaceManager = stageSimulation.setupStage(msHub, scene)

        studyLogger = StudySpatialLogger(cam, msHub, null)

        class ScenarioToParams(val scenario: Any, val targets: List<Vector3f>)
        class StudyTargetPosition(val axonTargets : List<ScenarioToParams>, val tubeTargets: List<ScenarioToParams>)

        val axonTargets = TrialGenerator.baseAxonParams.map { axonP ->
            val targetPos = axonP.toScenario().generate(stageSpaceManager, stageSimulation.stageSpaceSize)
            ScenarioToParams(axonP , targetPos)
        }

        val tubeTargets = TrialGenerator.baseTubeParams.map { p ->
            val targetPos = p.toScenario().generate(stageSpaceManager, stageSimulation.stageSpaceSize)
            ScenarioToParams(p , targetPos)
        }


        val mapper = jacksonObjectMapper()
        val writer = BufferedWriter(FileWriter(File("scenariosToTargets.json")))
        writer.write(mapper.writeValueAsString(StudyTargetPosition(axonTargets,tubeTargets)))
        writer.close()

        System.exit(0)
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TargetPositionExtractor().main()
        }
    }
}



