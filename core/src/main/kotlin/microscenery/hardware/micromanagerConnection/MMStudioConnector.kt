package microscenery.hardware.micromanagerConnection

import org.joml.Vector3f

interface MMStudioConnector {
    fun snap()
    fun startAcquisition()
    fun live(enabled: Boolean)
    fun alertUser(title:String, message:String)
    fun askForStageLimitErrorResolve(): StageLimitErrorResolves?
    fun addPositionToPositionList(label: String, position: Vector3f)

    enum class StageLimitErrorResolves{
        RESET_LIMITS,MOVE_STAGE,IGNORE
    }
}

class DummyMMStudioConnector : MMStudioConnector{

    override fun snap() {
        println("Dummy Snap")
    }

    override fun startAcquisition() {
        println("Dummy Start acqusition")
    }

    override fun live(enabled: Boolean) {
        println("Dummy live to $enabled")
    }

    override fun askForStageLimitErrorResolve(): MMStudioConnector.StageLimitErrorResolves {
        println("Dummy live to stage limit error resolve")
        return MMStudioConnector.StageLimitErrorResolves.IGNORE
    }
    override fun alertUser(title: String, message: String) {
        println("Dummy Alert User $title: $message")
    }

    override fun addPositionToPositionList(label: String, position: Vector3f) {
        println("Dummy Add Position $label: $position")
    }

}