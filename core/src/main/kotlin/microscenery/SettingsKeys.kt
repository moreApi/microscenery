package microscenery

object Settings {
    object VRToolbox {
        const val AblationInkMoverEnabled: String = "VRToolBox.AblationInkMoverEnabled"
        const val SlicingEnabled: String = "VRToolBox.SlicingEnabled"
        const val DrawPointsEnabled: String = "DrawPointsEnabled"
        const val DrawLineEnabled: String = "VRToolBox.DrawLineEnabled"
        const val PathAblationEnabled = "VRToolBox.PathAblationEnabled"
        const val PointAblationEnabled: String = "VRToolBox.PointAblationEnabled"
        const val BubblesEnabled: String= "VRToolBox.BubblesEnabled"
        const val OptionsEnabled: String = "VRToolBox.OptionsEnabled"
        const val ColorChooserEnabled: String = "VRToolBox.ColorChooserEnabled"
    }

    object VRUI {
        const val LeftHandMenuFixedPosition: String = "VRUI.leftHandMenuFixedPosition"
        const val TeleportEnabled = "VRUI.teleportEnabled"
    }

    object Ablation {
        object PointTool {
            const val MinDistUm = "Ablation.PointTool.MinDistUm"
        }

        /** bool */ const val Enabled = "Ablation.Enabled"
        /** Vec3f UM*/ const val PrecisionUM = "Ablation.precisionUM"
        /** Float */ const val SizeUM = "Ablation.SizeUM"

        /** long */ const val DwellTimeMicroS: String = "Ablation.dwellTimeMicroS"
        /** float */ const val LaserPower: String = "Ablation.laserPower"
        /** bool, count time it takes to move towards next point to that points dwell time */
        const val CountMoveTime: String = "Ablation.countMoveTime"
        /** bool */ const val PauseLaserOnMove: String = "Ablation.pauseLaserOnMove"
        /** bool */ const val DryRun: String = "Ablation.dryRun"
        /** int */ const val Repetitions: String = "Ablation.repetitions"
        /** bool */ const val StartAcquisitionAfter: String = "Ablation.startAcquisitionAfter"
        /** int */ const val StepSizeUm: String = "Ablation.stepSize"
        object SysCon {
            /** bool */ const val ScanModeFast = "Ablation.SysCon.scanModeFast"
            /** string */ const val LightSourceId = "Ablation.SysCon.lightSourceId"
            /** string */ const val TriggerPort = "Ablation.SysCon.triggerPort"
        }
    }

    object StageSpace{
        /** bool */ const val HideFocusFrame = "StageSpace.hideFocusFrame"
        /** bool */ const val HideFocusTargetFrame = "StageSpace.hideFocusTargetFrame"
        /** string */ const val ColorMap = "StageSpace.colorMap"
    }

    object ZenMicroscope {
        /** bool */ const val MockSysCon = "ZenMicroscope.mockSysCon"
        /** float */ const val exposureTime = "ZenMicroscope.exposureTime"
    }

    object UI {
        const val FlySpeed = "UI.flySpeed"
        const val ShowSelectionIndicator = "UI.showSelectionIndicator"
    }

    object Network {
        /** int */ const val sliceOffset = "Network.sliceOffset"
    }
}