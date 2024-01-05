package microscenery.VRUI.behaviors

import graphics.scenery.Node
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.utils.extensions.times
import org.joml.Vector3f

/**
 * Scaling implemented as a pressable. Both controllers need to start touching the node.
 */
class VRScaleNode(
    node: Node,
    private val setScale: (Float) -> Unit = { node.ifSpatial { scale *= Vector3f(it) } }
) {

    val firstLock: Any = Object()
    var first = true

    var mainHandSpatial: Spatial? = null
    var offHandSpatial: Spatial? = null

    init {
        node.addAttribute(Pressable::class.java, VRScalePressable(this))
    }

    fun init() {
        val mhs = mainHandSpatial
        val offhs = offHandSpatial
        if (mhs != null && offhs != null)
            bothInit(mhs, offhs)
    }

    fun drag() {
        val mhs = mainHandSpatial
        val offhs = offHandSpatial

        if (mhs != null && offhs != null)
            bothDrag(mhs, offhs)
    }

    // this part is donated by [VRScale]
    var lastDistance: Float = 0f
    private fun bothInit(mhs: Spatial, offhs: Spatial) {
        lastDistance = mhs.worldPosition()
            .distance(offhs.worldPosition())
    }

    private fun bothDrag(mhs: Spatial, offhs: Spatial) {
        val newDistance = mhs.worldPosition()
            .distance(offhs.worldPosition())
        val scale = newDistance / lastDistance
        lastDistance = newDistance
        setScale(scale)
    }

    class VRScalePressable(val main: VRScaleNode) : SimplePressable() {
        private var mainhand = false
        override val onPress: ((Spatial, TrackedDevice) -> Unit) = { it,_ ->
            mainhand = synchronized(main.firstLock) {
                if (main.first) {
                    main.first = false
                    main.mainHandSpatial = it
                    main.init()
                    true
                } else {
                    main.offHandSpatial = it
                    main.init()
                    false
                }
            }
        }

        override val onHold: ((Spatial, TrackedDevice) -> Unit) = { _,_ -> if (mainhand) main.drag() }

        override val onRelease: ((Spatial, TrackedDevice) -> Unit) = { _,_ ->
            if (mainhand) {
                synchronized(main.firstLock) {
                    main.first = true
                }
            }
        }
    }
}
