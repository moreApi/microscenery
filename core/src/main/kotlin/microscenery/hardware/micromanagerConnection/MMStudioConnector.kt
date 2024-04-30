package microscenery.hardware.micromanagerConnection

import org.joml.Vector3f

interface MMStudioConnector {
    fun snap()
    fun startAcquisition()
    fun live(enabled: Boolean)
    fun alertUser(title:String, message:String)
    fun addPositionToPositionList(label: String, position: Vector3f)
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

    override fun alertUser(title: String, message: String) {
        println("Dummy Alert User $title: $message")
    }

    override fun addPositionToPositionList(label: String, position: Vector3f) {
        println("Dummy Add Position $label: $position")
    }
}