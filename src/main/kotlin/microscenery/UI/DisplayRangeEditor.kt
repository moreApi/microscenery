package microscenery.UI

import bdv.tools.brightness.ConverterSetup
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import kotlin.math.max

class DisplayRangeEditor(var converter: ConverterSetup): JFrame("DisplayRangeEditor") {

    val minMinText = JTextField("0", 5)
    val minSlider = JSlider(JSlider.HORIZONTAL)
    val minMaxText = JTextField(max(converter.displayRangeMin.toInt(), 100).toString(), 5)
    val minValueLabel = JLabel("0")

    val maxMinText = JTextField("100", 5)
    val maxSlider = JSlider(JSlider.HORIZONTAL)
    val maxMaxText = JTextField(max(converter.displayRangeMax.toInt(), 100).toString(), 5)
    val maxValueLabel = JLabel("0")

    init {
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