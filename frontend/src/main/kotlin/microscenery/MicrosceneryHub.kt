package microscenery

import graphics.scenery.DefaultNode
import graphics.scenery.Hub
import microscenery.UI.UIModel

/**
 * Currently most of this is initialized in the StageSpaceManager
 */
class MicrosceneryHub(hub: Hub, uiModel: UIModel = UIModel()): DefaultNode("MicrosceneryHub") {
    init {
        addShort(hub)
        addShort(uiModel)
    }
    // TODO move ms init into hub like this:
//    var isInit = false
//
//    fun initHub(scene: Scene, hardware: MicroscopeHardware, hub: Hub){
//
//        this.addAttribute(Scene::class.java, scene)
//        this.addAttribute(MicroscopeHardware::class.java,hardware)
//        this.addAttribute(Hub::class.java, hub)
//
//        val ssm = StageSpaceManager(
//            hardware,
//            scene,
//            hub,
//            layout = MicroscopeLayout.Default(MicroscopeLayout.Axis.Z)
////            layout = MicroscopeLayout.Scape(MicroscopeLayout.Axis.Y, 33f)
//        )
//        addShort(ssm)
//        val sliceManager = SliceManager(hardware, ssm.stageRoot, scene)
//        addShort(sliceManager)
//        val ablationManager = AblationManager(hardware,ssm,scene)
//        addShort(ablationManager)
//
//        isInit = true
//    }
//
    private fun <T: Any> addShort(a:T) {this.addAttribute(a.javaClass,a)}

}