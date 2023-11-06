package microscenery.stageSpace

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.lazyLogger
import microscenery.Agent
import microscenery.MicroscenerySettings
import microscenery.VRUI.Gui3D.Row
import microscenery.VRUI.Gui3D.TextBox
import microscenery.copy
import microscenery.hardware.MicroscopeHardware
import microscenery.setVector3fIfUnset
import microscenery.signals.*
import org.joml.*
import java.util.concurrent.TimeUnit

/**
 * Handles in and output events concerning the microscope.
 *
 * Constructor waits until microscope is ready.
 */
class StageSpaceManager(
    val hardware: MicroscopeHardware,
    val scene: Scene,
    val hub: Hub,
    val layout: MicroscopeLayout = MicroscopeLayout.Default()
) : Agent() {
    private val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    val stageRoot = RichNode("stage root")
    val scaleAndRotationPivot = RichNode("scaleAndRotationPivot")

    val focus: Frame
    var focusTarget: FrameGizmo? = null

    private val stageAreaBorders: Box
    var stageAreaCenter = Vector3f()
        private set

    private val microscopeStatusLabel = TextBox("microscope status")

    val sliceManager = SliceManager(hardware, stageRoot, scene)
    val ablationManager = AblationManager(hardware,this,scene)

    var stagePosition: Vector3f
        get() = hardware.status().stagePosition
        set(value) {
            hardware.stagePosition = value
        }

    init {
        MicroscenerySettings.setVector3fIfUnset("Stage.ExploreResolution", Vector3f(10f))
        MicroscenerySettings.setIfUnset("Stage.CameraDependendZSorting", true)
        MicroscenerySettings.setIfUnset("Stage.NextStackLive", false)

        scene.addChild(scaleAndRotationPivot)
        scaleAndRotationPivot.addChild(stageRoot)

        stageAreaBorders = Box(Vector3f(1f), insideNormals = true)
        stageAreaBorders.name = "stageAreaBorders"
        stageAreaBorders.material {
            ambient = Vector3f(0.6f, 0.6f, 0.6f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
            specular = Vector3f(0.0f, 0.0f, 0.0f)
            cullingMode = Material.CullingMode.Front
        }
        stageRoot.addChild(stageAreaBorders)
        BoundingGrid().node = stageAreaBorders

        val microscopeStatusLabelPivot = Row(microscopeStatusLabel)
        microscopeStatusLabelPivot.spatial {
            scale = Vector3f(0.3f)

            fun updateMSLabelRotation(viewOrientation: Quaternionf){
                rotation = Quaternionf(viewOrientation).conjugate().normalize()
            }
            val hmd = hub.get<OpenVRHMD>()
            microscopeStatusLabelPivot.update += {
                if (hmd != null) {
                    updateMSLabelRotation(hmd.getOrientation())
                } else {
                    scene.activeObserver?.let { updateMSLabelRotation(it.spatial().rotation) }
                }
            }
        }
        scene.addChild(microscopeStatusLabelPivot)

        focus = Frame(hardware.hardwareDimensions(), Vector3f(0.4f, 0.4f, 1f)).apply {
            spatial().position = hardware.stagePosition.copy()
            stageRoot.addChild(this)
            visible = !MicroscenerySettings.get(microscenery.Settings.StageSpace.HideFocusFrame,false)
        }

        if (!MicroscenerySettings.get(microscenery.Settings.StageSpace.HideFocusTargetFrame,false))
            focusTarget = FrameGizmo(this, hardware.hardwareDimensions()).apply {
                spatial().position = hardware.stagePosition.copy()
                stageRoot.addChild(this)
            }

        focusTarget?.children?.first()?.spatialOrNull()?.rotation = layout.sheetRotation()
        focus.children.first()?.spatialOrNull()?.rotation = layout.sheetRotation()

        startAgent()
    }

    override fun onLoop() {
        val signal = hardware.output.poll(200, TimeUnit.MILLISECONDS)
        signal?.let { logger.info("got a ${signal::class.simpleName} signal:\n$signal") }
        when (signal) {
            is Slice -> {
                sliceManager.handleSliceSignal(signal, layout)
            }
            is HardwareDimensions -> {
                handleHardwareDimensionsSignal(signal)
            }
            is MicroscopeStatus -> {
                focus.spatial().position = signal.stagePosition
                updateMicroscopeStatusLabel(signal)
            }
            is Stack -> {
                sliceManager.handleStackSignal(signal, hub)
            }
            is AblationResults -> {
                logger.info("Ablation took ${signal.totalTimeMillis}ms for ${signal.perPointTime.size} points " +
                        "(${signal.mean()}ms mean)")
            }
        }
    }

    private fun updateMicroscopeStatusLabel(signal: MicroscopeStatus) {
        microscopeStatusLabel.text = "Microscope: " + when(signal.state){
            ServerState.LIVE -> "sending data"
            ServerState.MANUAL -> "idle"
            ServerState.SHUTTING_DOWN -> "shutting down"
            ServerState.STACK -> "imaging"
            ServerState.STARTUP -> "startup"
            ServerState.ABLATION -> "ablating"
        }
        (microscopeStatusLabel.parent as? Row)?.pack()
    }

    private fun handleHardwareDimensionsSignal(signal: HardwareDimensions) {
        stageAreaCenter = (signal.stageMax + signal.stageMin).times(0.5f)

        stageRoot.spatial {
            // scale the space in such a way that is initially always the same visual size and easy to handle by GUI
            val xSize = (signal.stageMax.x - signal.stageMin.x + signal.imageSize.x * signal.vertexDiameter)
            scale = Vector3f((1 / xSize) * 3)
            position = Vector3f(-1f) * stageAreaCenter * scale
            updateWorld(true,false)
        }
        stageAreaBorders.spatial {
            position = stageAreaCenter
            scale = (signal.stageMax - signal.stageMin).apply {
                // extra space for images at the edge of stage space
                val imgPixSize = Vector2f(signal.imageSize) * signal.vertexDiameter
                val imageSize = when (layout.sheet) {
                    MicroscopeLayout.Axis.X -> Vector3f(0f, imgPixSize.y, imgPixSize.x)
                    MicroscopeLayout.Axis.Y -> Vector3f(imgPixSize.x, 0f, imgPixSize.y)
                    MicroscopeLayout.Axis.Z -> Vector3f(imgPixSize, 0f)
                }
                this.add(imageSize)
                this.mul(1.1f)
            }
        }
        microscopeStatusLabel.parent?.ifSpatial {
            // move it on top of the stage space
            position = stageRoot.spatial().worldPosition((Vector3f(stageAreaCenter.x, signal.stageMax.y + signal.imageSize.x * signal.vertexDiameter,stageAreaCenter.z)))
        }

        focusTarget?.applyHardwareDimensions(signal)
        focus.applyHardwareDimensions(signal)
    }

    fun stack(from: Vector3f, to: Vector3f, live: Boolean = MicroscenerySettings.get("Stage.NextStackLive", false)) {
        hardware.acquireStack(
            ClientSignal.AcquireStack(
                from,
                to,
                MicroscenerySettings.get("Stage.precisionZ", hardware.hardwareDimensions().vertexDiameter),
                live
            )
        )
    }

    fun snapSlice() {
        hardware.snapSlice()
    }

    fun goLive() {
        logger.info("going live")
        hardware.goLive()
    }

    fun stop() {
        logger.info("stopping")
        hardware.stop()
    }

    fun exploreCubeStageSpace(p1: Vector3f, p2: Vector3f, resolution: Vector3f = Vector3f(
        MicroscenerySettings.get("Stage.ExploreResolutionX", 10f),
        MicroscenerySettings.get("Stage.ExploreResolutionY", 10f),
        MicroscenerySettings.get("Stage.ExploreResolutionZ", 10f)
    )) {
        if (hardware.status().state != ServerState.MANUAL) {
            throw IllegalStateException("Can only start sampling stage space if server is in Manual state.")
        }

        val from = Vector3f(
            p1.x.coerceAtMost(p2.x),
            p1.y.coerceAtMost(p2.y),
            p1.z.coerceAtMost(p2.z),
        )
        val to = Vector3f(
            p1.x.coerceAtLeast(p2.x),
            p1.y.coerceAtLeast(p2.y),
            p1.z.coerceAtLeast(p2.z),
        )

        val positions = mutableListOf<Vector3f>()
        // I'm missing classic for loops, kotlin :,(
        var x = from.x
        while (x <= to.x) {
            var y = from.y
            while (y <= to.y) {
                var z = from.z
                while (z <= to.z) {
                    positions += Vector3f(x, y, z)
                    z += resolution.z
                }
                y += resolution.y
            }
            x += resolution.x
        }

        positions.forEach {
            this.stagePosition = it
            this.snapSlice()
        }
    }

    fun clearStage() {
        sliceManager.clearSlices()
    }

    fun worldToStageSpace(s: Spatial){
        s.position = worldToStageSpace(s.worldPosition())
        s.scale *= getInverseWorldScale()
    }

    fun worldToStageSpace(v: Vector3f,isPosition: Boolean = true): Vector3f =
        Matrix4f(stageRoot.spatial().world).invertAffine().transform(Vector4f().set(v, if(isPosition) 1f else 0f)).xyz()

    fun getInverseWorldScale(): Vector3f = Matrix4f(stageRoot.spatial().world).invertAffine().getScale(Vector3f())
}