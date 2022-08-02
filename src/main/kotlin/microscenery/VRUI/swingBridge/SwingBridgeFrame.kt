package microscenery.VRUI.swingBridge

import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.SwingUtilities

open class SwingBridgeFrame(title: String) : JFrame(title) {

    fun click(x: Int, y: Int) {
        SwingUtilities.invokeLater {
            val target = SwingUtilities.getDeepestComponentAt(this.contentPane, x, y)
            val compPoint = SwingUtilities.convertPoint(
                this.contentPane, x, y, target
            )
            println("SwingUI: simulating Click at ${compPoint.x},${compPoint.y} on ${(target as? JButton)?.text}")

            // entered
            target.dispatchEvent(
                MouseEvent(
                    target, 504, System.currentTimeMillis() - 100, 0, compPoint.x, compPoint.y, 0, false, 0
                )
            )
            // pressed
            target.dispatchEvent(
                MouseEvent(
                    target, 501, System.currentTimeMillis() - 75, 1040, compPoint.x, compPoint.y, 1, false, 1
                )
            )
            // released
            target.dispatchEvent(
                MouseEvent(
                    target, 502, System.currentTimeMillis() - 50, 16, compPoint.x, compPoint.y, 1, false, 1
                )
            )
            // clicked
            target.dispatchEvent(
                MouseEvent(
                    target, 500, System.currentTimeMillis() - 25, 16, compPoint.x, compPoint.y, 1, false, 1
                )
            )
            // exited
            target.dispatchEvent(
                MouseEvent(
                    target, 505, System.currentTimeMillis(), 0, compPoint.x, compPoint.y, 0, false, 0
                )
            )
        }
    }

    fun getScreen(): BufferedImage {
        return getScreenShot(this.contentPane)
    }

    private fun getScreenShot(
        component: Component
    ): BufferedImage {
        val image = BufferedImage(
            component.width, component.height, BufferedImage.TYPE_INT_RGB
        )
        // call the Component's paint method, using
        // the Graphics object of the image.
        component.paint(image.graphics) // alternately use .printAll(..)
        return image
    }
}