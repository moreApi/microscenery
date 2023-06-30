package microscenery.hardware.micromanagerConnection

interface MMStudioConnector {
    fun snap()
    fun startAcquisition()
    fun live(enabled: Boolean)
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
}