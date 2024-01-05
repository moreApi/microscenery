package microscenery.UI

import graphics.scenery.Node
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackerRole
import microscenery.PropertyChangeObservable
import microscenery.VRUI.VRHandTool

class UIModel : PropertyChangeObservable() {
    var selected: Node? by propertyObservable(null)
    fun updateSelected() {
        selected = selected
    }


    var inLeftHand: VRHandTool? by propertyObservable(null)
    var inRightHand: VRHandTool? by propertyObservable(null)
    fun inHand(trackerRole: TrackerRole) = when (trackerRole) {
        TrackerRole.Invalid -> throw IllegalArgumentException()
        TrackerRole.LeftHand -> inLeftHand
        TrackerRole.RightHand -> inRightHand
    }

    fun putInHand(trackerRole: TrackerRole, tool: VRHandTool?) = when (trackerRole) {
        TrackerRole.Invalid -> throw IllegalArgumentException()
        TrackerRole.LeftHand -> inLeftHand = tool
        TrackerRole.RightHand -> inRightHand = tool
    }

    var leftVRController: TrackedDevice? = null
    var rightVRController: TrackedDevice? = null

}
