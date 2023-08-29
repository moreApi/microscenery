package microscenery

object Settings {
    object VRToolbox {
        const val SlicingEnabled: String = "VRToolBox.SlicingEnabled"
        const val DrawPointsEnabled: String = "DrawPointsEnabled"
        const val DrawLineEnabled: String = "VRToolBox.DrawLineEnabled"
        const val PathAblationEnabled = "VRToolBox.PathAblationEnabled"
        const val PointAblationEnabled: String = "VRToolBox.PointAblationEnabled"
        const val BubblesEnabled: String= "VRToolBox.BubblesEnabled"
        const val OptionsEnabled: String = "VRToolBox.OptionsEnabled"
        const val ColorChooserEnabled: String = "VRToolBox.ColorChooserEnabled"
    }

    object Ablation {
        const val Enabled = "Ablation.Enabled"
        const val Precision = "Ablation.precision" //Vec3f
        const val Size = "Ablation.Size" //Vec3f
    }
}