package microscenery.VRUI.Gui3D

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
    }

}