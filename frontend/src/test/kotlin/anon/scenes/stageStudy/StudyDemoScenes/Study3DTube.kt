package anon.scenes.stageStudy.StudyDemoScenes

import anon.scenes.stageStudy.StageViewerStudy3D
import anon.simulation.TubeScenario

object Study3DTube{
    @JvmStatic
    fun main(args: Array<String>) {
        StageViewerStudy3D(TubeScenario()).main()
    }
}