package anon.scenes.stageStudy.StudyDemoScenes

import anon.scenes.stageStudy.StageViewerStudyVR
import anon.simulation.TubeScenario

object StudyVRTube{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudyVR(TubeScenario()).main()
    }
}