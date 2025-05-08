package microscenery.simulation

import fromScenery.lazyLogger
import graphics.scenery.Origin
import microscenery.MicroscenerySettings
import microscenery.Settings
import microscenery.hardware.MicroscopeHardware
import microscenery.hardware.MicroscopeHardwareAgent
import microscenery.signals.*
import microscenery.toReadableString
import org.joml.Vector3f
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


/**
 * Wraps around a microscope and simulates ablation by subtracting pixel values aground ablation points, like a negative SDF rendering.
 */
class AblationSimulationMicroscope (
    val microscope: MicroscopeHardware,
    val actionAfterAblation: (AblationSimulationMicroscope) -> Unit = {it.startAcquisition()},
    var imgOrigin: Origin = Origin.Center
): MicroscopeHardwareAgent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))



    private var ablationPoints: List<MicroscopeControlSignal.AblationPoint> = emptyList()

    init {
        startAgent()
    }

    override fun ablatePoints(signal: MicroscopeControlSignal.AblationPoints) {
        logger.info("Adding ablation points to simulation: "+signal.points.map { it.position.toReadableString() })
        ablationPoints += signal.points
        actionAfterAblation(this)
    }

    override fun stop() {
        ablationPoints = emptyList()
        microscope.stop()
    }

    override fun onLoop() {
        val signal = microscope.output.poll(200, TimeUnit.MILLISECONDS) ?: return

        if (signal is MicroscopeSlice){
            if (ablationPoints.isEmpty()) {
                this.output.put(signal)
                return
            }

            //logger.info("Processing Slice ${signal.slice.Id}")

            val slice = signal.slice
            val hwd = hardwareDimensions
            val ablationRadius = MicroscenerySettings.get(Settings.Ablation.SizeUM,15f)

            val raw = slice.data ?: throw IllegalArgumentException("got slice signal without data buffer")
            if (raw.isReadOnly) throw IllegalArgumentException("read only data buffer currently not supported")
            raw.rewind()
            val data8 = raw
            val data16 = raw.asShortBuffer()

            for (imgY in 0 until hwd.imageSize.y) for (imgX in 0 until hwd.imageSize.x){
                val value = when(hwd.numericType){
                    NumericType.INT8 -> data8.get().toInt()
                    NumericType.INT16 -> data16.get().toInt()
                }

                var newValue = value
                val pixelStagePos = Vector3f(slice.stagePos)
                pixelStagePos.x += imgX * hwd.vertexDiameter
                pixelStagePos.y += imgY * hwd.vertexDiameter
                if (imgOrigin == Origin.Center){
                    pixelStagePos.x -= hwd.imageSize.x * 0.5f * hwd.vertexDiameter
                    pixelStagePos.y -= hwd.imageSize.y * 0.5f * hwd.vertexDiameter
                }

                ablationPoints.forEach { ablationPoint ->
                    val diff = ablationPoint.position.distance(pixelStagePos)
                    if (diff <= ablationRadius) {
                        newValue = (newValue * (diff / ablationRadius)).toInt()
                    }
                }
                if (newValue != value) {
                    when(hwd.numericType){
                        NumericType.INT8 -> {
                            data8.put(data8.position() - 1, (newValue).toByte())
                        }
                        NumericType.INT16 -> {
                            data16.put(data16.position() - 1, (newValue).toShort())
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