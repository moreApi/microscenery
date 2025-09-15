package anon.scenes.stageStudy.StudyDemoScenes

import anon.scenes.stageStudy.StageViewerStudy3D
import anon.simulation.AxonScenario

object Study3DAxon{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy3D(AxonScenario()).main()
    }
}