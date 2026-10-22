package com.vibe.app.ui.screens

import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibe.app.R
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.FieldBox
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.SelectChip
import com.vibe.app.ui.components.StepDots
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors
import java.util.Calendar

private data class GenderOption(val code: String, val label: String, val image: Int, val color: Color)

private val genders = listOf(
    GenderOption("M", "Male", R.drawable.ghost_male, VibeColors.Cyan),
    GenderOption("F", "Female", R.drawable.ghost_female, VibeColors.Pink),
    GenderOption("O", "Other", R.drawable.ghost_other, VibeColors.Gold),
)
private val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val languages = listOf("Hindi", "English", "Bengali", "Marathi", "Telugu", "Tamil", "Gujarati", "Kannada", "Malayalam", "Punjabi", "Urdu", "Odia")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var photo by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var nameFocused by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf<String?>(null) }
    var day by remember { mutableStateOf<Int?>(null) }
    var month by remember { mutableStateOf<Int?>(null) }
    var year by remember { mutableStateOf<Int?>(null) }
    val langs = remember { mutableStateListOf("Hindi") }
    var showInvite by remember { mutableStateOf(false) }
    var invite by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) photo = uri }
    val thisYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember { (thisYear - 13 downTo thisYear - 80).toList() }
    val age = if (day != null && month != null && year != null) ageOf(day!!, month!!, year!!) else null

    val missing = buildList {
        if (name.isBlank()) add("name")
        if (gender == null) add("gender")
        if (age == null) add("date of birth")
        if (langs.isEmpty()) add("a language")
    }
    val ready = missing.isEmpty() && (age ?: 0) >= 18

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopEnd).offset(90.dp, (-90).dp))

        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Box(Modifier.weight(1f).padding(end = 40.dp), contentAlignment = Alignment.Center) { StepDots(done = 3) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Text("Create your profile", color = VibeColors.Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("This is how others will see you on VIBE", color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

                // photo
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Box(
                            Modifier.size(118.dp).clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(VibeColors.Gold, VibeColors.Pink, VibeColors.Cyan, VibeColors.Gold)))
                                .padding(3.dp).clip(CircleShape).background(VibeColors.Dark).padding(4.dp).clip(CircleShape)
                                .background(VibeColors.Card),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photo != null) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Image(painterResource(R.drawable.ghost_camera), null, Modifier.size(96.dp))
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(VibeColors.Pink, VibeColors.Violet)))
                                .border(3.dp, VibeColors.Dark, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    }
                    Text(
                        if (photo != null) "✓ Looking good! Tap to change" else "Add a clear photo of your face · profiles with photos get 5× more calls",
                        color = if (photo != null) VibeColors.Green else VibeColors.TextMute,
                        fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp),
                    )
                }

                // name
                Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp)) {
                    Label("Name", Modifier.weight(1f))
                    Text("${name.length}/20", color = VibeColors.TextMute, fontSize = 11.sp)
                }
                FieldBox(Modifier.fillMaxWidth(), focused = nameFocused) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(20) },
                        singleLine = true,
                        textStyle = TextStyle(color = VibeColors.Ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(VibeColors.Pink),
                        modifier = Modifier.weight(1f).onFocusChanged { nameFocused = it.isFocused },
                        decorationBox = { inner ->
                            Box {
                                if (name.isEmpty()) Text("What should people call you?", color = VibeColors.TextMute, fontSize = 15.sp)
                                inner()
                            }
                        },
                    )
                }

                // gender
                Label("I am", Modifier.padding(top = 24.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    genders.forEach { g ->
                        val on = gender == g.code
                        val shape = RoundedCornerShape(16.dp)
                        Column(
                            Modifier.weight(1f)
                                // soft outer glow drawn as fading strokes (an elevation shadow showed through the card as a dark patch)
                                .drawBehind {
                                    if (on) for (i in 1..4) drawRoundRect(
                                        g.color.copy(alpha = 0.16f / i), topLeft = Offset(-i * 2.dp.toPx(), -i * 2.dp.toPx()),
                                        size = Size(size.width + i * 4.dp.toPx(), size.height + i * 4.dp.toPx()),
                                        cornerRadius = CornerRadius((16 + i * 2).dp.toPx()), style = Stroke(2.dp.toPx()),
                                    )
                                }
                                .clip(shape)
                                // opaque fill (tint blended into the card colour) so nothing behind can show through
                                .background(if (on) Brush.verticalGradient(listOf(lerp(VibeColors.Card, g.color, 0.2f), VibeColors.Card)) else SolidColor(VibeColors.Card))
                                .border(1.5.dp, if (on) g.color else VibeColors.Line, shape)
                                .clickable { gender = g.code }
                                .padding(top = 8.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(painterResource(g.image), null, Modifier.size(64.dp))
                            Text(g.label, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text("Gender can't be changed later.", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))

                // date of birth
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
                    Label("Date of birth", Modifier.weight(1f))
                    if (age != null) Text("$age years", color = VibeColors.TextDim, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropField("Day", day?.toString(), (1..31).map { it.toString() }, Modifier.weight(1f)) { day = it + 1 }
                    DropField("Month", month?.let { months[it] }, months, Modifier.weight(1.4f)) { month = it }
                    DropField("Year", year?.toString(), years.map { it.toString() }, Modifier.weight(1.2f)) { year = years[it] }
                }
                if (age != null && age < 18) {
                    Text("You must be 18 or older to use VIBE.", color = VibeColors.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                // languages
                Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
                    Label("Languages I speak", Modifier.weight(1f))
                    Text("pick up to 3", color = VibeColors.TextMute, fontSize = 11.sp)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { l ->
                        SelectChip(l, l in langs, onClick = {
                            when {
                                l in langs -> langs.remove(l)
                                langs.size < 3 -> langs.add(l)
                                else -> ctx.toast("You can pick up to 3 languages")
                            }
                        })
                    }
                }

                // invite code: already there when the app came from a friend's invite link, else typed by hand
                val linkCode = com.vibe.app.data.InviteLink.pending
                if (linkCode != null) Text(
                    "✓ Invited by a friend (code $linkCode) · you both get coins on your first purchase",
                    color = VibeColors.Green, fontSize = 14.sp, modifier = Modifier.padding(top = 24.dp),
                ) else Text(
                    "🎁 Have an invite code? ${if (showInvite) "▾" else "›"}",
                    color = VibeColors.Cyan, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp).clickable { showInvite = !showInvite },
                )
                AnimatedVisibility(showInvite && linkCode == null) {
                    FieldBox(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        BasicTextField(
                            value = invite,
                            onValueChange = { invite = it.uppercase().filter(Char::isLetterOrDigit).take(10) },
                            singleLine = true,
                            textStyle = TextStyle(color = VibeColors.Ink, fontSize = 15.sp, letterSpacing = 2.sp),
                            cursorBrush = SolidColor(VibeColors.Pink),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (invite.isEmpty()) Text("Friend's code · coins for both on your first purchase", color = VibeColors.TextMute, fontSize = 14.sp)
                                    inner()
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // footer
            Column(
                Modifier.fillMaxWidth().background(VibeColors.Card.copy(alpha = 0.85f)).padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NeonButton(
                    text = "Continue",
                    enabled = ready && !saving,
                    loading = saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            try {
                                val body = org.json.JSONObject()
                                    .put("name", name.trim())
                                    .put("gender", when (gender) { "M" -> "male"; "F" -> "female"; else -> "other" })
                                    .put("dob", "%04d-%02d-%02d".format(year, (month ?: 0) + 1, day))
                                    .put("languages", org.json.JSONArray(langs.toList()))
                                val res = com.vibe.app.data.api.Api.put("/me/profile", body)
                                com.vibe.app.data.api.AuthStore.saveUser(res.getJSONObject("user"))
                                // the photo is optional: if it fails the profile is still saved, the user can add it later
                                photo?.let { p ->
                                    runCatching { com.vibe.app.data.api.DpUpload.upload(ctx, p) }
                                        .onSuccess { com.vibe.app.ui.components.PickedMedia.copy(ctx, p, "dp_", keepOnlyLatest = true)?.let { com.vibe.app.ui.components.MyProfile.dp = it } }
                                        .onFailure { ctx.toast("Profile saved. The photo could not be uploaded, add it again from Edit profile.") }
                                }
                                // a friend's invite code: both get the reward coins on this person's first purchase
                                val code = com.vibe.app.data.InviteLink.pending ?: invite
                                if (code.length >= 4) {
                                    com.vibe.app.data.InviteLink.clear(ctx)
                                    runCatching { com.vibe.app.data.api.Api.post("/invite/apply", org.json.JSONObject().put("code", code)) }
                                        .onSuccess { ctx.toast("Invite code added · you both get coins on your first purchase") }
                                        .onFailure { ctx.toast("Invite code: ${it.message} · you can add it later in Invite friends") }
                                }
                                ctx.toast("Welcome to VIBE, ${name.trim()}! 🎉")
                                onDone()
                            } catch (e: com.vibe.app.data.api.ApiException) {
                                ctx.toast(e.message)
                            }
                            saving = false
                        }
                    },
                )
                Text(
                    when {
                        missing.isNotEmpty() -> "Add your " + missing.joinToString(", ")
                        !ready -> "VIBE is only for adults (18+)"
                        else -> "All set! 🎉"
                    },
                    color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, color = VibeColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}

@Composable
private fun DropField(label: String, value: String?, options: List<String>, modifier: Modifier, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FieldBox(Modifier.fillMaxWidth().clickable { open = true }, focused = open) {
            Text(value ?: label, color = if (value == null) VibeColors.TextMute else VibeColors.Ink, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Text("▾", color = VibeColors.TextMute)
        }
        DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 320.dp).background(VibeColors.Card)) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o, color = VibeColors.Ink) }, onClick = { onPick(i); open = false })
            }
        }
    }
}

private fun ageOf(day: Int, month: Int, year: Int): Int {
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - year
    val birthdayPassed = now.get(Calendar.MONTH) > month ||
        (now.get(Calendar.MONTH) == month && now.get(Calendar.DAY_OF_MONTH) >= day)
    if (!birthdayPassed) age--
    return age
}
