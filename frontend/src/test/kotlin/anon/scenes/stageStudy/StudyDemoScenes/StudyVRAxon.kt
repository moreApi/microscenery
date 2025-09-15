package anon.scenes.stageStudy.StudyDemoScenes

import anon.scenes.stageStudy.StageViewerStudyVR
import anon.simulation.AxonScenario

object StudyVRAxon{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudyVR(AxonScenario()).main()
    }
}