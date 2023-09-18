package microscenery.VRUI.Gui3D

import microscenery.MicroscenerySettings

class ValueEdit<T>(start:T,
                   plus: (T) -> T,
                   minus: (T) -> T,
                   plusPlus: ((T) -> T)? = null,
                   minusMinus: ((T) -> T)? = null,
                   toString: ((T) -> String)? = null
): Row(), Gui3DElement {

    var value : T = start
        set(value) {
            field = value
            valueText.text = valueToString()
        }
    val valueToString: () -> String

    val valueText: TextBox

    init {
        this.name = "ValueEdit"
        valueToString = toString?.let { {it(value)} } ?: {value.toString()}

        valueText = TextBox(valueToString())

        minusMinus?.let { addChild(Button("--") { value = it(value) }) }
        addChild(Button("-") { value = minus(value) })
        addChild(valueText)
        addChild(Button("+") { value = plus(value) })
        plusPlus?.let { addChild(Button("++") { value = it(value) }) }
        pack()
    }
    companion object{
        fun forFloatSetting(setting: String, factor: Float = 1f, withPlusPlus: Boolean = true): ValueEdit<Float>{
            val start = MicroscenerySettings.get(setting,0f)
            fun changeAndSave(value:Float, change: Float): Float{
                val t = value + change * factor
                MicroscenerySettings.set(setting,t)
                return t
            }

            return ValueEdit(
                start,
                { changeAndSave(it, 1f)},
                { changeAndSave(it, -1f)},
                if (withPlusPlus){ {changeAndSave(it, 10f)}} else null,
                if (withPlusPlus){ {changeAndSave(it, -10f)}} else null,
            )
        }

        fun forIntSetting(
            setting: String,
            factor: Int = 1,
            plusPlusButtons: Boolean = true,
            toString: ((Int) -> String)? = null
        ): ValueEdit<Int> {
            val start = MicroscenerySettings.get(setting, 0)
            fun changeAndSave(value: Int, change: Int): Int {
                val t = value + change * factor
                MicroscenerySettings.set(setting, t)
                return t
            }

            return ValueEdit(
                start,
                { changeAndSave(it, 1)},
                { changeAndSave(it, -1)},
                if (plusPlusButtons){ {changeAndSave(it, 10)}} else null,
                if (plusPlusButtons){ {changeAndSave(it, -10)}} else null,
                toString
            )
        }
    }
}