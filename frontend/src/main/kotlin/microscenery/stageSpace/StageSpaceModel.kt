package microscenery.stageSpace

import graphics.scenery.Node
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackerRole
import microscenery.PropertyChangeObservable
import microscenery.VRUI.VRHandTool
import microscenery.signals.HardwareDimensions
import microscenery.stageSpace.FocusManager.Mode

class StageSpaceModel : PropertyChangeObservable() {

    var hardwareDimensions: HardwareDimensions by propertyObservable(HardwareDimensions.EMPTY)
    var focusMode: FocusManager.Mode by propertyObservable(Mode.PASSIVE)

}
