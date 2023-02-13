package microscenery.hardware.micromanagerConnection

interface MMStudioConnector {
    fun startAcquisition()
}

class DummyMMStudioConnector : MMStudioConnector{
    override fun startAcquisition() {
        println("Dummy Start acqusition")
    }

}