package microscenery.scenes.stageStudy.StudyDemoScenes

import microscenery.scenes.stageStudy.StageViewerStudy2D
import microscenery.simulation.AxonScenario

object Study2DAxon{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy2D(AxonScenario()).main()
    }
}