package fromScenery


import net.miginfocom.swing.MigLayout
import org.joml.*
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.table.DefaultTableModel

/**
 * @author Konrad Michel <konrad.michel@mailbox.tu-dresden.de>
 *
 * A Swing UI, that allows the user to load setting in order to manipulate and add settings.
 * Allows saving and loading of settings into a .properties file
 * Allows refreshing of the settings table in order to show changes to settings values that happened during runtime.
 */
class SettingsEditor @JvmOverloads constructor(var settings : Settings, private val mainFrame : JFrame = JFrame("SettingsEditor"), width : Int = 480, height : Int = 500) {

    private val settingsTable : JTable
    private var tableContents : DefaultTableModel

    private val addSettingLabel : JLabel
    private val addTextfield : JTextField
    private val addValuefield : JTextField

    private val saveButton : JButton
    private val loadButton : JButton

    private val refreshButton : JButton

    init {

        mainFrame.size = Dimension(width, height)
        mainFrame.preferredSize = Dimension(width, height)
        mainFrame.minimumSize = Dimension(width, height)
        mainFrame.layout = MigLayout()
        mainFrame.isVisible = true


        //settings table

        tableContents = DefaultTableModel()
        settingsTable = JTable(tableContents)
        settingsTable.autoCreateRowSorter = true
        mainFrame.add(JScrollPane(settingsTable), "cell 0 0 12 8")

        updateSettingsTable()

        settingsTable.putClientProperty("terminateEditOnFocusLost", true)
        tableContents.addTableModelListener { e ->
            if(e.type != TableModelEvent.DELETE  && e.column != TableModelEvent.ALL_COLUMNS)
            {
                changeSettingsTableAt(e.firstRow)
            }
        }

        //add setting
        addSettingLabel = JLabel("Add setting: ")
        mainFrame.add(addSettingLabel, "cell 0 9 1 1")

        addTextfield = JTextField(6)
        mainFrame.add(addTextfield, "cell 1 9 1 1")

        addValuefield = JTextField(6)
        mainFrame.add(addValuefield, "cell 2 9 1 1")

        addValuefield.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {}
            override fun keyPressed(e: KeyEvent) {}
            override fun keyReleased(e: KeyEvent) {
                if(e.keyCode == KeyEvent.VK_ENTER)
                {

                    val castValue = try {
                        settings.parseType(addValuefield.text)
                    } catch (e: IllegalArgumentException) {
                        addValuefield.text = null
                        JOptionPane.showMessageDialog(mainFrame, e.message, "Type Error", JOptionPane.ERROR_MESSAGE)
                        return
                    } catch (e: NumberFormatException) {
                        addValuefield.text = null
                        JOptionPane.showMessageDialog(mainFrame, e.message, "Type Error", JOptionPane.ERROR_MESSAGE)
                        return
                    }
                    settings.setIfUnset(addTextfield.text.toString(), castValue)
                    addTextfield.text = null
                    addValuefield.text = null

                    updateSettingsTable()
                }
            }
        })

        //loading
        loadButton = JButton("Load")
        loadButton.setSize(50, 20)
        mainFrame.add(loadButton, "cell 3 9 1 1")
        loadButton.addActionListener {
            loadSettings()
        }

        //saving
        saveButton = JButton("Save")
        saveButton.setSize(50, 20)
        mainFrame.add(saveButton, "cell 4 9 1 1")
        saveButton.addActionListener {
            saveSettings()
        }

        //refreshing
        refreshButton = JButton("Refresh")
        refreshButton.setSize(50, 20)
        mainFrame.add(refreshButton, "cell 5 9 1 1")
        refreshButton.addActionListener {
            refreshSettings()
        }

        mainFrame.pack()
    }

    /**
     * Loads a .properties file as settings
     */
    private fun loadSettings()
    {
        val fileInspector = JFileChooser()
        val filter = FileNameExtensionFilter("Properties: ", "properties")
        fileInspector.fileFilter = filter
        fileInspector.fileSelectionMode = JFileChooser.FILES_ONLY
        fileInspector.currentDirectory = File(File("").absolutePath)

        fileInspector.selectedFile

        val returnVal = fileInspector.showOpenDialog(loadButton)
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            settings.loadProperties(fileInspector.selectedFile.inputStream())

            refreshSettings()
        }
    }

    /**
     * Refreshes the settings, e.g. in case a .properties file change happened or somewhere outside the editor
     *
     * Careful: Currently, changes to the .settings file during runtime overwrite runtime changes upon refresh, if not saved before!
     */
    private fun refreshSettings()
    {
        updateSettingsTable()
    }

    /**
     * saves the current settings into a .properties file
     */
    private fun saveSettings()
    {
        val fileInspector = JFileChooser()
        val filter = FileNameExtensionFilter("Properties: ", "properties")
        fileInspector.fileFilter = filter
        fileInspector.fileSelectionMode = JFileChooser.FILES_ONLY
        fileInspector.currentDirectory = File(File("").absolutePath)
        val returnVal = fileInspector.showSaveDialog(saveButton)
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            var path = fileInspector.selectedFile

            if(path.extension != "properties")
                path = File( path.absolutePath + ".properties")

            settings.saveProperties(path.absolutePath)
        }
    }

    /**
     * Updates the settings table [settingsTable], with the actually set settings coming from the Settings-object [settings] defined in the editor
     */
    private fun updateSettingsTable()
    {
        val settingKeys = settings.getAllSettings()
        tableContents.rowCount = 0
        tableContents.columnCount = 0
        tableContents.addColumn("Property")
        tableContents.addColumn("Value")


        for(key in settingKeys)
        {
            @Suppress("RemoveSingleExpressionStringTemplate")
            // Casting to string does not work otherwise. There seems to be some wonky behavior with the casting
            var entry = "${settings.get<String>(key)}"
            val value = settings.get<Any>(key)
            when(value::class.java.typeName) {
                Vector2f::class.java.typeName ->
                {
                    value as Vector2f
                    entry = "("+ trimAndFormatEntry(value.x) +", "+ trimAndFormatEntry(value.y) + ")"
                }
                Vector3f::class.java.typeName ->
                {
                    value as Vector3f
                    entry = "("+ trimAndFormatEntry(value.x) +", "+ trimAndFormatEntry(value.y) +", "+ trimAndFormatEntry(value.z) + ")"
                }
                Vector4f::class.java.typeName ->
                {
                    value as Vector4f
                    entry = "("+ trimAndFormatEntry(value.x) +", "+ trimAndFormatEntry(value.y) +", "+ trimAndFormatEntry(value.z) +", "+ trimAndFormatEntry(value.w) + ")"
                }
            }
            tableContents.addRow(arrayOf(key, entry))
        }
        settingsTable.rowSorter.toggleSortOrder(0)
    }

    private fun trimAndFormatEntry(value : Float) : String {
        val format = "%f"
        val locale = Locale.US

        return String.format(locale, format, value).trim('0').trim('.')
    }

    /**
     *Changes the value at [row], no need to define the setting
     */
    private fun changeSettingsTableAt(row : Int) {
        val setting = tableContents.getValueAt(row, 0)
        val value = tableContents.getValueAt(row, 1)
        val settingString = setting as String
        val oldValue = settings.getProperty<Any>(settingString)

        val castValue = try {
            settings.parseType("$value")
        } catch (e: NumberFormatException) {
            JOptionPane.showMessageDialog(mainFrame, e.message, "Type Error", JOptionPane.ERROR_MESSAGE)
            return
        } //also throws IllegalArgumentException if there are too little or too many Arguments in the vector, but this is caught earlier

        if(castValue::class.java.typeName != oldValue::class.java.typeName) {
            JOptionPane.showMessageDialog(mainFrame,
                "Wrong Type! Expected ${oldValue::class.java.typeName}, inserted ${castValue::class.java.typeName}",
                "Type Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        settings.set("$setting", castValue)
    }

}

