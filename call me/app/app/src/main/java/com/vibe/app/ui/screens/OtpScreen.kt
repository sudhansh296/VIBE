package com.vibe.app.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.vibe.app.ui.components.findActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.StepDots
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class OtpStatus { Idle, Checking, Wrong, Ok }

@Composable
fun OtpScreen(phone: String, onBack: () -> Unit, onVerified: (com.vibe.app.data.api.LoginResult) -> Unit) {
    val ctx = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(OtpStatus.Idle) }
    var attemptsLeft by remember { mutableIntStateOf(3) }
    var secondsLeft by remember { mutableIntStateOf(30) }
    var resendKey by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val floatY by rememberFloat()

    LaunchedEffect(Unit) { focus.requestFocus() }

    val scope = rememberCoroutineScope()

    suspend fun verify(c: String?) {
        status = OtpStatus.Checking
        try {
            val result = com.vibe.app.data.api.AuthRepo.verifyOtp(c)
            status = OtpStatus.Ok
            keyboard?.hide()
            delay(400)
            onVerified(result)
        } catch (e: com.vibe.app.data.api.ApiException) {
            if (e.code == "otp_wrong") {
                attemptsLeft--
                status = OtpStatus.Wrong
                shake.animateTo(0f, keyframes {
                    durationMillis = 350
                    -14f at 70; 14f at 140; -7f at 210; 7f at 280
                })
            } else {
                status = OtpStatus.Idle
                ctx.toast(e.message)
            }
            code = ""
        }
    }

    // Android may verify the number by itself (SMS read automatically): then no code is needed
    LaunchedEffect(Unit) { if (com.vibe.app.data.api.AuthRepo.hasAutoCredential) verify(null) }

    // verify as soon as all 6 digits are in
    LaunchedEffect(code) {
        if (code.length == 6 && attemptsLeft > 0) verify(code)
    }

    // resend countdown
    LaunchedEffect(resendKey) {
        secondsLeft = 30
        while (secondsLeft > 0) { delay(1000); secondsLeft-- }
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 280.dp, Modifier.offset((-80).dp, (-90).dp))
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.BottomEnd).offset(90.dp, 40.dp))

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Box(Modifier.weight(1f).padding(end = 40.dp), contentAlignment = Alignment.Center) { StepDots(done = 2) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Image(
                    painterResource(R.drawable.ghost_otp), null,
                    Modifier.align(Alignment.CenterHorizontally).size(150.dp).graphicsLayer { translationY = floatY.dp.toPx() },
                )
                Text("Verify your number", color = VibeColors.Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Code sent to ", color = VibeColors.TextDim, fontSize = 14.sp)
                    Text("+91 ${phone.take(5)} ${phone.drop(5)}", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("  Change", color = VibeColors.Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onBack))
                }

                // 6 boxes driven by one hidden text field (handles typing, paste and backspace)
                Box(Modifier.padding(top = 32.dp)) {
                    BasicTextField(
                        value = code,
                        onValueChange = {
                            if (status == OtpStatus.Checking || status == OtpStatus.Ok) return@BasicTextField
                            code = it.filter(Char::isDigit).take(6)
                            if (status == OtpStatus.Wrong) status = OtpStatus.Idle
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        cursorBrush = SolidColor(Color.Transparent),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
                        modifier = Modifier.focusRequester(focus).size(1.dp).alpha(0f),
                    )
                    Row(
                        Modifier.fillMaxWidth()
                            .offset { IntOffset(shake.value.roundToInt(), 0) }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                focus.requestFocus(); keyboard?.show()
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        repeat(6) { i -> OtpBox(digit = code.getOrNull(i), active = i == code.length, status = status, modifier = Modifier.weight(1f)) }
                    }
                }

                Text(
                    when (status) {
                        OtpStatus.Wrong -> if (attemptsLeft > 0) "Wrong code. $attemptsLeft attempts left." else "Too many attempts. Request a new code."
                        OtpStatus.Ok -> "✓ Verified"
                        else -> ""
                    },
                    color = if (status == OtpStatus.Ok) VibeColors.Green else VibeColors.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp).height(16.dp),
                )

                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Didn't get the code?", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (secondsLeft > 0) {
                        Text("Resend in 00:%02d".format(secondsLeft), color = VibeColors.TextMute, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Resend code", color = VibeColors.Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    when (val r = com.vibe.app.data.api.AuthRepo.sendOtp(ctx.findActivity(), phone, resend = true)) {
                                        is com.vibe.app.data.api.AuthRepo.OtpStart.Failed -> ctx.toast(r.message)
                                        com.vibe.app.data.api.AuthRepo.OtpStart.AutoVerified -> verify(null)
                                        else -> { ctx.toast("New code sent"); attemptsLeft = 3; code = ""; status = OtpStatus.Idle; resendKey++ }
                                    }
                                }
                            })
                    }
                }

                Row(
                    Modifier.padding(top = 28.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VibeColors.Card)
                        .border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).padding(16.dp),
                ) {
                    Text("🛡️", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("VIBE will never ask for your OTP on chat or call. Don't share it with anyone.",
                        color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }

            NeonButton(
                text = if (status == OtpStatus.Ok) "Verified ✓" else "Verify & continue",
                enabled = code.length == 6 && status != OtpStatus.Wrong,
                loading = status == OtpStatus.Checking,
                onClick = {},
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun OtpBox(digit: Char?, active: Boolean, status: OtpStatus, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val border = when {
        status == OtpStatus.Wrong -> VibeColors.Red
        status == OtpStatus.Ok -> VibeColors.Green
        active -> VibeColors.Pink
        digit != null -> VibeColors.Cyan.copy(alpha = 0.6f)
        else -> VibeColors.Line
    }
    val bg = if (digit != null) Brush.verticalGradient(listOf(VibeColors.Cyan.copy(alpha = 0.08f), VibeColors.Card)) else SolidColor(VibeColors.Card)
    Box(
        modifier.aspectRatio(5f / 6f).clip(shape).background(bg).border(1.5.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(digit?.toString() ?: "", color = VibeColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    }
}
