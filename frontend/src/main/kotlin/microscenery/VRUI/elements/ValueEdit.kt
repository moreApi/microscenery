package microscenery.VRUI.elements

import graphics.scenery.RichNode
import graphics.scenery.controls.behaviours.Pressable
import graphics.scenery.controls.behaviours.SimplePressable
import graphics.scenery.controls.behaviours.Touchable

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

        minusMinus?.let { row.addChild(generateButton("--",it)) }
        row.addChild(generateButton("-",minus))
        row.addChild(valueText)
        row.addChild(generateButton("+",plus))
        plusPlus?.let { row.addChild(generateButton("++",it)) }

        this.addChild(row)
    }

    private fun generateButton(text: String,function: (T) -> T): TextBox {
        return TextBox(text).also { box ->
            box.addAttribute(Touchable::class.java, Touchable())
            box.addAttribute(Pressable::class.java, SimplePressable(onPress = { value = function(value) }))
        }
    }

}