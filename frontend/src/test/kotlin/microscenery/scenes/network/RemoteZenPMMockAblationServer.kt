package microscenery.scenes.network

import fromScenery.SettingsEditor
import graphics.scenery.Origin
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.network.RemoteMicroscopeServer
import microscenery.signals.MicroscopeControlSignal
import microscenery.simulation.AblationSimulationMicroscope
import microscenery.zenSysConCon.ZenBlueTCPConnector
import microscenery.zenSysConCon.ZenSysConMicroscope
import microscenery.zenSysConCon.sysCon.SysConConnection
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.zeromq.ZContext

class RemoteZenPMMockAblationServer {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MicroscenerySettings.set(Settings.Ablation.SizeUM, 30f)
            SettingsEditor(MicroscenerySettings)

//        val id = """C:\Users\JanCasus\Zeiss\Experiment-19.czi"""
            val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring.czi"""
            val id2 = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring2.czi"""
//        val id = """C:\Nextcloud\Zeiss\sd3\20230712_488_square_ring3.czi"""
            //val id = """C:\Nextcloud\Zeiss\marina-sd3-drosophila2.czi"""
            val initalExperimentFile = """zenSysConCon/src/test/resources/OriginalTestExperiment.czexp"""
            val marinaMac = """/Users/hzdr/ViingsCloud/Zeiss/marina-sd3-drosophila1.czi"""

            val sd3transfomFixCzi = "volumes/20250813 avp ablation - everything is a bit off/Experiment-808.czi"
            val sd3transfomFixExp = "volumes/20250813 avp ablation - everything is a bit off/JanT_RAPP_488_zStack2.czexp"

            val zenBlue: ZenBlueTCPConnector = Mockito.mock(ZenBlueTCPConnector::class.java)
            val sysCon: SysConConnection = Mockito.mock(SysConConnection::class.java)

            whenever(zenBlue.getCurrentDocument()).thenReturn(sd3transfomFixCzi)
            whenever(zenBlue.saveExperimentAndGetFilePath()).thenReturn(sd3transfomFixExp)

            val zenMicroscope = ZenSysConMicroscope(zenBlue, sysCon)

            RemoteMicroscopeServer(AblationSimulationMicroscope(zenMicroscope, imgOrigin = Origin.FrontBottomLeft), ZContext(), acquireOnConnect = true)
        }
    }
}