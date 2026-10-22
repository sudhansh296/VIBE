package com.vibe.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

/** On/off switch (pink gradient when on). */
@Composable
fun VibeSwitch(on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val x by animateDpAsState(if (on) 23.dp else 3.dp, label = "thumb")
    Box(
        modifier.width(50.dp).height(30.dp).clip(CircleShape)
            .then(if (on) Modifier.background(VibeBrush.Button) else Modifier.background(Color(0xFFFFD6E3)))
            .clickable(onClick = onToggle),
    ) { Box(Modifier.offset(x, 3.dp).size(24.dp).clip(CircleShape).background(Color.White)) }
}

/** Text input in the app's field style (border glows pink on focus). */
@Composable
fun VibeField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    caps: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight).clip(shape).background(VibeColors.Card)
            .border(1.5.dp, if (focused) VibeColors.Pink else VibeColors.Line, shape).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = singleLine,
            textStyle = TextStyle(color = VibeColors.Ink, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboard,
                capitalization = if (caps) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
            ),
            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = VibeColors.TextMute, fontSize = 14.sp); inner() } },
        )
        if (trailing != null) {
            Box(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Small selectable pill (rate chips, language chips, filters). */
@Composable
fun PillChip(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, locked: Boolean = false, gold: Boolean = false) {
    val accent = if (gold) VibeColors.Gold else VibeColors.Pink
    Row(
        modifier.height(36.dp).clip(CircleShape)
            .background(if (on) accent.copy(alpha = 0.16f) else VibeColors.Card)
            .border(1.5.dp, if (on) accent else VibeColors.Line, CircleShape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (locked) {
            Icon(CallIcons.Lock, null, tint = Color(0xFFB093A2), modifier = Modifier.size(14.dp))
            Box(Modifier.width(4.dp))
        }
        Text(
            text, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            color = if (on) (if (gold) VibeColors.Gold else VibeColors.Pink) else if (locked) Color(0xFFB093A2) else VibeColors.ChipText,
        )
    }
}
