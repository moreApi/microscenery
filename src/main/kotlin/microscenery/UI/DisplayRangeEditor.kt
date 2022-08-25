package microscenery.UI

import bdv.tools.brightness.ConverterSetup
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import kotlin.math.max

class DisplayRangeEditor(var converter: ConverterSetup, visibility: Boolean = true): JFrame("DisplayRangeEditor") {


    val minMinText: JTextField
    val minSlider: JSlider
    val minMaxText: JTextField
    val minValueLabel: JLabel

    val maxMinText: JTextField
    val maxSlider: JSlider
    val maxMaxText: JTextField
    val maxValueLabel: JLabel

    init {

        val initMinValue = max(converter.displayRangeMin.toInt(), 100)
        minMinText = JTextField("0", 5)
        minSlider = JSlider(JSlider.HORIZONTAL,0,(initMinValue*1.5).toInt(),initMinValue)
        minMaxText = JTextField((initMinValue*1.5).toInt().toString(), 5)
        minValueLabel = JLabel(initMinValue.toString())

        val initMaxValue = max(converter.displayRangeMax.toInt(), 100)
        maxMinText = JTextField((initMinValue*1.5).toInt().toString(), 5)
        maxSlider = JSlider(JSlider.HORIZONTAL,(initMinValue*1.5).toInt(),initMaxValue,initMaxValue)
        maxMaxText = JTextField(initMaxValue.toString(), 5)
        maxValueLabel = JLabel(initMaxValue.toString())

        val contentPanel = JPanel()
        val padding = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        contentPanel.border = padding
        //this.contentPane = contentPanel

        this.layout = GridBagLayout()
        val constrains = GridBagConstraints()
        constrains.insets = Insets(1, 1, 1, 1)

        minMinText.addActionListener { updateSliderRange() }
        minMaxText.addActionListener { updateSliderRange() }
        minSlider.addChangeListener { updateConverter() }

        constrains.gridy=0
        listOf(JLabel("min:"),minMinText,minSlider,minMaxText).forEachIndexed { index, it ->
            constrains.gridx = index
            this.add(it,constrains)
        }

        constrains.gridx = 2
        constrains.gridy = 1
        this.add(minValueLabel,constrains)


        maxMinText.addActionListener { updateSliderRange() }
        maxMaxText.addActionListener { updateSliderRange() }
        maxSlider.addChangeListener { updateConverter() }

        constrains.gridy=2
        listOf(JLabel("max:"),maxMinText,maxSlider,maxMaxText).forEachIndexed { index, it ->
            constrains.gridx = index
            this.add(it,constrains)
        }

        constrains.gridx = 2
        constrains.gridy = 3
        this.add(maxValueLabel,constrains)


        updateSliderRange()
        this.pack()
        this.isVisible = visibility
    }

    private fun updateSliderRange(){
        val minMin = minMinText.toInt()
        val minMax = minMaxText.toInt()
        if (minMax != null && minMin != null){
            minSlider.minimum = minMin
            minSlider.maximum = minMax
        }

        val maxMin = maxMinText.toInt()
        val maxMax = maxMaxText.toInt()
        if (maxMax != null && maxMin != null){
            maxSlider.minimum = maxMin
            maxSlider.maximum = maxMax
        }

        updateConverter()
    }

    private fun updateConverter(){
        minValueLabel.text = minSlider.value.toString()
        maxValueLabel.text = maxSlider.value.toString()

        converter.setDisplayRange(minSlider.value.toDouble(),maxSlider.value.toDouble())
    }

    private fun JTextField.toInt() = text.toIntOrNull()

}