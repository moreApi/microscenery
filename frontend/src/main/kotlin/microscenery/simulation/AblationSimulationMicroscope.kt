package microscenery.simulation

import fromScenery.lazyLogger
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import org.joml.Vector3f
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class AblationSimulationMicroscope (
    val microscope: MicroscopeHardware,
    val actionAfterAblation: (AblationSimulationMicroscope) -> Unit = {it.startAcquisition()}
): MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))


    private var ablationPoints: List<MicroscopeControlSignal.AblationPoint> = emptyList()

    init {
        startAgent()
    }

    override fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints) {
        ablationPoints += signal.points
        actionAfterAblation(this)
    }


    override fun onLoop() {
        val signal = microscope.output.poll(200, TimeUnit.MILLISECONDS) ?: return

        if (signal is MicroscopeSlice){
            if (ablationPoints.isEmpty()) {
                this.output.put(signal)
                return
            }

            logger.info("Processing Slice ${signal.slice.Id}")

            val slice = signal.slice
            val hwd = hardwareDimensions
            val ablationRadius = MicroscenerySettings.get(Settings.Ablation.SizeUM,15f)

            val raw = slice.data ?: throw IllegalArgumentException("got slice signal without data buffer")
            if (raw.isReadOnly) throw IllegalArgumentException("read only data buffer currently not supported")
            raw.rewind()
            val data8 = raw
            val data16 = raw.asShortBuffer()

            for (y in 0 until hwd.imageSize.y) for (x in 0 until hwd.imageSize.x){
                val value = when(hwd.numericType){
                    NumericType.INT8 -> data8.get().toInt() + Byte.MAX_VALUE
                    NumericType.INT16 -> data16.get().toInt() + Short.MAX_VALUE
                }

                var newValue = value
                val pos = Vector3f(slice.stagePos)
                pos.x += x * hwd.vertexDiameter
                pos.y += y * hwd.vertexDiameter

                ablationPoints.forEach { ablationPoint ->
                    val diff = ablationPoint.position.distance(pos)
                    if (diff <= ablationRadius) {
                        newValue = (newValue * (diff / ablationRadius)).toInt()
                    }
                }
                if (newValue != value) {
                    when(hwd.numericType){
                        NumericType.INT8 -> {
                            data8.put(data8.position() - 1, (newValue - Byte.MAX_VALUE).toByte())
                        }
                        NumericType.INT16 -> {
                            data16.put(data16.position() - 1, (newValue - Short.MAX_VALUE).toShort())
                        }
                    }
                }
            }
            raw.rewind()
            // all changes where in-place therefore we just forward the signal
            this.output.put(signal)
        } else {
            this.output.put(signal)
        }
    }

    // ### wrap this.microscope for all other microscope functions ###

    override var stagePosition: Vector3f by microscope::stagePosition
    override var hardwareDimensions: HardwareDimensions
        get() = microscope.hardwareDimensions()
        set(_) {}

    override fun goLive() {
        microscope.goLive()
    }

    override fun stop() {
        ablationPoints = emptyList()
        microscope.stop()
    }

    override fun snapSlice() {
        microscope.snapSlice()
    }

    override fun shutdown() {
        this.close()
        microscope.shutdown()
    }

    override fun acquireStack(meta: MicroscopeControlSignal.AcquireStack) {
        microscope.acquireStack(meta)
    }

    override fun status(): MicroscopeStatus {
        return microscope.status()
    }

    override fun hardwareDimensions(): HardwareDimensions {
        return microscope.hardwareDimensions()
    }

    override fun moveStage(target: Vector3f) {
        microscope.stagePosition = target
    }

    override fun startAcquisition() {
        microscope.startAcquisition()
    }

    override fun sync(): Semaphore {
        return microscope.sync()
    }

}