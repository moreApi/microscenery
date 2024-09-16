package microscenery

object Settings {
    object VRToolbox {
        const val AblationInkMoverEnabled: String = "VRToolBox.AblationInkMoverEnabled"
        const val CroppingEnabled: String = "VRToolBox.CroppingEnabled"
        const val PathAblationEnabled = "VRToolBox.PathAblationEnabled"
        const val PointAblationEnabled: String = "VRToolBox.PointAblationEnabled"
        const val BubblesEnabled: String= "VRToolBox.BubblesEnabled"
        const val OptionsEnabled: String = "VRToolBox.OptionsEnabled"
        const val ColorChooserEnabled: String = "VRToolBox.ColorChooserEnabled"
        const val MeasuringEnabled: String = "VRToolBox.MeasuringEnabled"
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
        object MM {
            /** string */ const val Shutter: String = "Ablation.Shutter" //todo rename
        }
    }

    object StageSpace{
        /** float */const val RandomSliceOffset = "StageSpace.randomSliceOffset"
        /** bool */ const val viewMode = "StageSpace.viewMode"
        /** bool */ const val HideFocusFrame = "StageSpace.hideFocusFrame"
        /** bool */ const val HideFocusTargetFrame = "StageSpace.hideFocusTargetFrame"
        /** bool */ const val HideStageSpaceLabel = "StageSpace.hideStageSpaceLabel"
        /** bool */ const val ShowHullbox = "StageSpace.showHullbox"
        /** bool */ const val ShowStageAreaBorders = "StageSpace.showStageAreaBorders"
        /** string */ const val ColorMap = "StageSpace.colorMap"
        /** bool */ const val CameraDependendZSorting = "StageSpace.cameraDependendZSorting"
    }

    object Stage{

        /** float*/ val PrecisionXY: String = "Stage.precisionXY"
        /** float*/ val PrecisionZ: String = "Stage.precisionZ"
        /** vec3f */ val ExploreResolution: String = "Stage.exploreResolution"
        /** bool */ val NextStackLive: String = "Stage.nextStackLive"

        object Limits{
            val OriginMoveProtection: String = "MMConnection.OriginMoveProtection"  //todo rename
            /** bool
             * Grow stage limits if stage is moved past it, using direct control or mm.
             * */
            const val AutoGrowStageLimits = "Stage.Limits.AutoGrowStageLimits"
            /** vec3f */ const val Min = "Stage.min" //todo rename
            /** vec3f */ const val Max = "Stage.max" //todo rename
        }
    }

    object ZenMicroscope {
        /** bool */ const val MockSysCon = "ZenMicroscope.mockSysCon"
        /** float */ const val exposureTime = "ZenMicroscope.exposureTime"
    }

    object UI {
        const val FlySpeed = "UI.flySpeed"
        const val ShowSelectionIndicator = "UI.showSelectionIndicator"
        /** bool */ const val ShowBorderOfSelected = "StageSpace.showBorderOfSelected"
    }

    object Network {
        /** int */ const val sliceOffset = "Network.sliceOffset"
    }

    object MMMicroscope {
        /** float*/ const val VertexDiameter: String = "MMConnection.vertexDiameter" //todo rename
        /** int */ const val PollStagePositionFrequencyMS = "MMMicroscope.PollStagePositionFrequencyMS"
        /** bool */ const val IsMicromanagerMicroscope = "MMMicroscope.IsMicromanagerMicroscope"
        /** bool */ const val UseImageMetadataPosition = "MMMicroscope.useImageMetadataPosition"

        object Stream{
            /** int */ const val ImageRateLimitPerSec = "MMMicroscope.Stream.imageRateLimitPerSec"
            /** int */ const val Camera = "MMMicroscope.Stream.camera"
        }
    }
}