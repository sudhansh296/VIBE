package com.vibe.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * The press feedback for every tappable thing in the app: the item shrinks a little while pressed and springs back.
 * It replaces the default ripple, which draws a SQUARE highlight on round buttons, round photos and pills.
 * Nothing is drawn behind the item, so no square can ever appear.
 */
object PressScale : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressScaleNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 7
}

private class PressScaleNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val scale = Animatable(1f)

    override fun onAttach() {
        coroutineScope.launch {
            var pressed = 0
            source.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> pressed++
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = (pressed - 1).coerceAtLeast(0)
                }
                val target = if (pressed > 0) 0.95f else 1f
                launch { scale.animateTo(target, spring(stiffness = 900f)) { invalidateDraw() } }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = scale.value
        if (s == 1f) drawContent() else scale(s) { this@draw.drawContent() }
    }
}
