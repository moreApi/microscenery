package anon.scenes.stageStudy.StudyDemoScenes

import anon.scenes.stageStudy.StageViewerStudy2D
import anon.simulation.AxonScenario

object Study2DAxon{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy2D(AxonScenario()).main()
    }
}