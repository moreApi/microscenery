package microscenery.scenes.stageStudy.Orchestration

import microscenery.simulation.AxionScenario
import microscenery.simulation.TubeScenario
import org.joml.Vector3f


data class TrialConfig(val name: String, var cases: List<Case>, val timeLimitPerCaseMS: Long = 1000*60*3){
}


data class Case(val modality: Modality, val scenario: Scenario, var done: Boolean = false)

enum class Modality{VR, ThreeD, TwoD}


data class Scenario(val tube: Tube? = null, val axion: Axion? = null){

    data class Tube(val seed: Long){
         fun toScenario() = TubeScenario(seed)
    }

    data class Axion(
        val seed: Long,
        val dir: FloatArray,
        val stepSize: Float,
        val iterations: Int,
        val childrenPerIterationFrom: Int,
        val childrenPerIterationTo: Int){

        fun toScenario() = AxionScenario(seed, Vector3f(dir),stepSize,iterations,childrenPerIterationFrom..childrenPerIterationTo)
    }
}


data class TrialLog(val name: String){

    var log: List<Event> = emptyList()

    enum class EventType{ Start, Finish}


    data class Event(val caseIndex: Int, val eventType: EventType, val time: Long = System.currentTimeMillis())
}
