package microscenery.example

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun parseXmlDocument(xmlFilePath: String): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(xmlFilePath)

    // Filter out line breaks
    filterLineBreaks(document)

    document.documentElement.normalize()
    return document
}


fun NodeList.asList() : List<Node> {
    if (this.length == 0) return emptyList()
    return (0 until this.length).map { this.item(it) }
}

fun modifyXmlDocument2(document: Document) {
    val rootElement = document.documentElement

    val target = rootElement.getElementsByTagName("title").asList().firstOrNull{it.textContent == "To Kill a Mockingbird"}?.parentNode

    val book = document.createElement("book")
    val author = document.createElement("author")
    author.appendChild(document.createTextNode("bums rums"))
    val title = document.createElement("title")
    title.appendChild(document.createTextNode("bumsblatt"))
    book.appendChild(title)
    book.appendChild(author)

    target?.let {
        it.parentNode?.insertBefore(book,it)
    }

}
fun writeXmlDocument(document: Document, outputFilePath: String) {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()

    // Configure to use explicit closing tags
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")

    // Disable indentation
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")

    val source = DOMSource(document)
    val result = StreamResult(outputFilePath)
    transformer.transform(source, result)
}

private fun filterLineBreaks(node: Node) {
    if (node.nodeType == Node.TEXT_NODE) {
        val textNode = node as Text
        val filteredText = textNode.nodeValue.replace("\\r|\\n".toRegex(), "")
        if (filteredText.isBlank()) {
            node.parentNode.removeChild(node)
        }
    } else {
        val childNodes = node.childNodes
        for (i in childNodes.length - 1 downTo 0) {
            val childNode = childNodes.item(i)
            filterLineBreaks(childNode)
        }
    }
}

fun main() {
    val xmlFilePath = "input.xml"
//    val xmlFilePath = """C:\Users\JanCasus\Zeiss\20230419_Test3_stack.czexp"""
    val outputFilePath = "output.xml"

    // Parse the XML document
    val document = parseXmlDocument(xmlFilePath)

    // Modify the XML document
    modifyXmlDocument2(document)

    // Write the modified XML document to a file
    writeXmlDocument(document, outputFilePath)

    println("XML document processing completed.")
}
