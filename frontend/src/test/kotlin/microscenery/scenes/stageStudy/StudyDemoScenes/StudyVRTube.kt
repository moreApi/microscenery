package microscenery.scenes.stageStudy.StudyDemoScenes

import microscenery.scenes.stageStudy.StageViewerStudyVR
import microscenery.simulation.TubeScenario

object StudyVRTube{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudyVR(TubeScenario()).main()
    }
}