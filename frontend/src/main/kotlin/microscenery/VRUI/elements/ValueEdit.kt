package microscenery.VRUI.elements

import graphics.scenery.RichNode

class ValueEdit<T>(start:T,
                   plus: (T) -> T,
                   minus: (T) -> T,
                   plusPlus: ((T) -> T)? = null,
                   minusMinus: ((T) -> T)? = null,
                   toString: ((T) -> String)? = null
): RichNode("ValueEdit"), Ui3DElement {

    var value : T = start
        set(value) {
            field = value
            valueText.text = valueToString()
        }
    val valueToString: () -> String

    val row = Row()
    val valueText: TextBox

    override val width by row::width

    init {
        valueToString = toString?.let { {it(value)} } ?: {value.toString()}

        valueText = TextBox(valueToString())

        minusMinus?.let { row.addChild(Button("--") { value = it(value) }) }
        row.addChild(Button("-") { value = minus(value) })
        row.addChild(valueText)
        row.addChild(Button("+") { value = plus(value) })
        plusPlus?.let { row.addChild(Button("++") { value = it(value) }) }

        this.addChild(row)
    }

}