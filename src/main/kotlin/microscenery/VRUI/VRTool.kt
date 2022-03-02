package microscenery.VRUI

import graphics.scenery.Node
import graphics.scenery.controls.behaviours.Grabable
import graphics.scenery.controls.behaviours.Pressable

fun Node.addVRToolFunctionality(
    onPickup: () -> Unit = {},
    whileHeld: () -> Unit = {},
    onDrop: () -> Unit = {},
    onActivate: () -> Unit = {},
    whileActivated: () -> Unit = {},
    onDeactivate: () -> Unit = {}
) {
    this.addAttribute(Grabable::class.java, Grabable(onPickup,whileHeld,onDrop,false))
    this.addAttribute(Pressable::class.java,Pressable(onActivate,whileActivated,onDeactivate))
}