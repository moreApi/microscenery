package microscenery.scenes.stageStudy.StudyDemoScenes

import microscenery.scenes.stageStudy.StageViewerStudyVR
import microscenery.simulation.AxonScenario

object StudyVRAxon{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudyVR(AxonScenario()).main()
    }
}