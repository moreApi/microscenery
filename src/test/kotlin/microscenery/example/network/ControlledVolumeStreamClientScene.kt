package microscenery.example.network

import microscenery.DefaultScene
import microscenery.StageSpaceManager
import microscenery.network.RemoteMicroscopeClient
import microscenery.network.SliceStorage
import org.joml.Vector3f
import org.zeromq.ZContext


class ControlledVolumeStreamClientScene : DefaultScene() {


    override fun init() {
        super.init()

        val zContext = ZContext()

        //val cvss = RemoteMicroscopeServer(,zContext = zContext)

        val cvsc = RemoteMicroscopeClient(SliceStorage(), zContext = zContext)

        val stageSpaceManager = StageSpaceManager(cvsc, scene)

        //lightSleepOnNull { cvsc.latestServerStatus }

        for (i in 40..60) {
            stageSpaceManager.snapSlice(Vector3f(0f, 0f, i * 1f))
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val b = ControlledVolumeStreamClientScene()
            b.main()
        }
    }
}