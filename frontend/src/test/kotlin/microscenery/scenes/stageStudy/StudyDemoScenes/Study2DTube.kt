package microscenery.scenes.stageStudy.StudyDemoScenes

import microscenery.scenes.stageStudy.StageViewerStudy2D
import microscenery.simulation.TubeScenario

object Study2DTube{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy2D(TubeScenario()).main()
    }
}

