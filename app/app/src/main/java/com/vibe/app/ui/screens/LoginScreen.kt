package com.vibe.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.ui.components.FieldBox
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.RingWave
import com.vibe.app.ui.components.rememberHeartbeat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val validPhone = Regex("^[6-9]\\d{9}$")

@Composable
fun LoginScreen(onOtpSent: (String) -> Unit, onGoogle: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var agree by remember { mutableStateOf(true) }
    var hindi by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    val beat by rememberHeartbeat()
    val valid = validPhone.matches(phone)

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 300.dp, Modifier.align(Alignment.TopEnd).offset(90.dp, (-80).dp))
        GlowBlob(VibeColors.Cyan, 260.dp, Modifier.offset((-100).dp, 240.dp))

        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            // top bar
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VIBE", style = TextStyle(brush = VibeBrush.Text, fontSize = 22.sp, fontWeight = FontWeight.Black))
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card)
                        .border(1.dp, VibeColors.Line, CircleShape).clickable { hindi = !hindi }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (hindi) "🌐 हिंदी" else "🌐 English", color = VibeColors.ChipText, fontSize = 13.sp) }
            }

            // hero mascot
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                RingWave(VibeColors.Pink.copy(alpha = 0.5f), 144.dp)
                RingWave(VibeColors.Cyan.copy(alpha = 0.5f), 144.dp, delayMs = 1000)
                Image(
                    painterResource(R.drawable.ghost_chat), null,
                    Modifier.size(160.dp).graphicsLayer { scaleX = beat; scaleY = beat },
                )
            }

            if (hindi) {
                Text("VIBE में आपका स्वागत है 👋", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                Row {
                    Text("Welcome to ", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("VIBE", style = TextStyle(brush = VibeBrush.Text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold))
                    Text(" 👋", fontSize = 26.sp)
                }
            }
            Text(
                if (hindi) "असली लोगों से वीडियो चैट करें। होस्ट बनें और कमाएँ, सबके लिए खुला।"
                else "Video chat with real people. Become a host and earn, open for everyone.",
                color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp),
            )

            // phone input
            Text(if (hindi) "मोबाइल नंबर" else "Mobile number", color = VibeColors.TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp))
            FieldBox(Modifier.fillMaxWidth(), focused = focused) {
                Text("🇮🇳 +91", color = Color.White, fontWeight = FontWeight.SemiBold)
                Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(24.dp).background(VibeColors.Line))
                BasicTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp, letterSpacing = 1.5.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = PhoneTransformation,
                    cursorBrush = SolidColor(VibeColors.Pink),
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        Box {
                            if (phone.isEmpty()) Text("Enter 10-digit number", color = VibeColors.TextMute, fontSize = 16.sp)
                            inner()
                        }
                    },
                )
                if (valid) Box(
                    Modifier.size(24.dp).clip(CircleShape).background(VibeColors.Green.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Text("✓", color = VibeColors.Green, fontSize = 13.sp) }
            }
            if (phone.length == 10 && !valid) {
                Text("Please enter a valid 10-digit mobile number", color = VibeColors.Red, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }

            NeonButton(
                text = if (hindi) "OTP भेजें" else "Send OTP",
                enabled = valid && agree,
                loading = sending,
                modifier = Modifier.padding(top = 20.dp),
                onClick = {
                    sending = true
                    scope.launch { delay(700); sending = false; onOtpSent(phone) }
                },
            )

            Row(Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(VibeColors.Line))
                Text(if (hindi) "  या इससे जारी रखें  " else "  or continue with  ", color = VibeColors.TextMute, fontSize = 12.sp)
                Box(Modifier.weight(1f).height(1.dp).background(VibeColors.Line))
            }

            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .clickable {
                        if (!agree) ctx.toast("Please accept the terms first")
                        else { ctx.toast("Google sign-in will open here"); onGoogle() }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                Text(if (hindi) "Google से जारी रखें" else "Continue with Google", color = Color(0xFF1F1F1F), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))

            // consent
            Row(Modifier.padding(vertical = 24.dp).clickable { agree = !agree }) {
                Box(
                    Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (agree) VibeColors.Pink else Color.Transparent)
                        .border(2.dp, if (agree) VibeColors.Pink else VibeColors.Line, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (agree) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(12.dp))
                Text(termsText(), fontSize = 11.sp, lineHeight = 16.sp, color = VibeColors.TextMute)
            }
        }
    }
}

private fun termsText(): AnnotatedString = buildAnnotatedString {
    append("I confirm I am ")
    withStyle(SpanStyle(color = Color(0xFFD1D5DB), fontWeight = FontWeight.Bold)) { append("18 or older") }
    append(" and agree to the ")
    withStyle(SpanStyle(color = VibeColors.Cyan)) { append("Terms of Service") }
    append(", ")
    withStyle(SpanStyle(color = VibeColors.Cyan)) { append("Privacy Policy") }
    append(" and ")
    withStyle(SpanStyle(color = VibeColors.Cyan)) { append("Community Rules") }
    append(".")
}

/** Shows 9876543210 as "98765 43210". */
private object PhoneTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val t = text.text
        val out = if (t.length > 5) t.substring(0, 5) + " " + t.substring(5) else t
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset > 5) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = (if (offset > 5) offset - 1 else offset).coerceIn(0, t.length)
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}
