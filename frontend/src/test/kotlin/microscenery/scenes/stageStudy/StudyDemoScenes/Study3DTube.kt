package microscenery.scenes.stageStudy.StudyDemoScenes

import microscenery.scenes.stageStudy.StageViewerStudy3D
import microscenery.simulation.TubeScenario

object Study3DTube{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy3D(TubeScenario()).main()
    }
}