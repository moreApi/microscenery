package microscenery.scenes.stageStudy.StudyDemoScenes

import microscenery.scenes.stageStudy.StageViewerStudy3D
import microscenery.simulation.AxonScenario

object Study3DAxon{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy3D(AxonScenario()).main()
    }
}