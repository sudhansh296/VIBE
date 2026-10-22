package com.vibe.app.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.data.MeDemo
import com.vibe.app.data.SettingsDemo
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.SpinBorder
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

// same list the server allows (settings: allowed_languages); the server rejects anything else
private val EDIT_LANGUAGES = listOf("Hindi", "English", "Bengali", "Marathi", "Telugu", "Tamil", "Gujarati", "Kannada", "Malayalam", "Punjabi", "Urdu", "Odia")

/** Edit profile: name, city, bio and languages are saved on the server; gender and date of birth are locked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit, onHostMedia: () -> Unit) {
    val ctx = LocalContext.current
    // the saved profile comes from the server (stored after login); demo values only if there is none
    val me = remember { com.vibe.app.data.api.AuthStore.user }
    var name by remember { mutableStateOf(me?.optString("name")?.takeIf { it.isNotEmpty() && it != "null" } ?: "") }
    var city by remember { mutableStateOf(me?.optString("city")?.takeIf { it != "null" } ?: "") }
    var bio by remember { mutableStateOf(me?.optString("bio")?.takeIf { it != "null" } ?: "") }
    val langs = remember {
        mutableStateListOf<String>().apply {
            val a = me?.optJSONArray("languages")
            if (a != null) for (i in 0 until a.length()) add(a.getString(i)) else add("Hindi")
        }
    }
    var saving by remember { mutableStateOf(false) }
    val saveScope = androidx.compose.runtime.rememberCoroutineScope()
    val genderText = when (me?.optString("gender")) { "male" -> "Male"; "female" -> "Female"; "other" -> "Other"; else -> "Not set" }
    val dobText = me?.optString("dob")?.takeIf { it.length == 10 }?.let { d ->
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "${d.substring(8).toInt()} ${months[d.substring(5, 7).toInt() - 1]} ${d.substring(0, 4)}" + (me.optInt("age", -1).takeIf { it >= 0 }?.let { " · $it yrs" } ?: "")
    } ?: "Not set"
    val canSave = name.trim().length >= 2 && langs.isNotEmpty()

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Edit profile", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val dpScope = androidx.compose.runtime.rememberCoroutineScope()
                    val pickDp = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null) dpScope.launch {
                            try {
                                com.vibe.app.data.api.DpUpload.upload(ctx, uri)
                                com.vibe.app.ui.components.PickedMedia.copy(ctx, uri, "dp_", keepOnlyLatest = true)?.let { com.vibe.app.ui.components.MyProfile.dp = it }
                                ctx.toast("Profile photo updated")
                            } catch (e: com.vibe.app.data.api.ApiException) {
                                ctx.toast(e.message)
                            }
                        }
                    }
                    Box(Modifier.clickable { pickDp.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        SpinBorder(Modifier.size(104.dp)) {
                            Box(
                                Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFD9E6), Color(0xFFFFC2D8)))),
                                contentAlignment = Alignment.Center,
                            ) {
                                com.vibe.app.ui.components.MyDp(Modifier.fillMaxSize(), name.trim().take(1).ifEmpty { "S" }.uppercase(), 36.sp)
                            }
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(VibeBrush.Button).border(2.dp, VibeColors.Dark, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(CallIcons.Camera, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                    Text("Tap to change your photo", color = VibeColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                }

                SectionLabel("Name", Modifier.padding(top = 20.dp, bottom = 8.dp))
                VibeField(name, { name = it.take(24) }, "Your name")

                SectionLabel("Gender", Modifier.padding(top = 20.dp, bottom = 8.dp))
                LockedField(genderText)
                SectionLabel("Date of birth", Modifier.padding(top = 20.dp, bottom = 8.dp))
                LockedField(dobText)

                SectionLabel("City", Modifier.padding(top = 20.dp, bottom = 8.dp))
                VibeField(city, { city = it.take(30) }, "Your city")

                SectionLabel("About me", Modifier.padding(top = 20.dp, bottom = 8.dp))
                VibeField(bio, { bio = it.take(150) }, "Tell people a little about you", singleLine = false, minHeight = 90.dp)
                Text("${bio.length}/150", color = VibeColors.TextMute, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

                SectionLabel("Languages", Modifier.padding(top = 20.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EDIT_LANGUAGES.forEach { l -> PillChip(l, l in langs, { if (l in langs) langs.remove(l) else langs.add(l) }) }
                }

                val hShape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.padding(top = 24.dp, bottom = 24.dp).fillMaxWidth().clip(hShape).background(VibeColors.Gold.copy(alpha = 0.05f))
                        .border(1.dp, VibeColors.Gold.copy(alpha = 0.30f), hShape).clickable(onClick = onHostMedia).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(VibeColors.Gold.copy(alpha = 0.10f)).border(1.dp, VibeColors.Gold.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(CallIcons.Photo, null, tint = VibeColors.Gold, modifier = Modifier.size(22.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Photos and videos", color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Add, lock and manage your gallery.", color = VibeColors.TextDim, fontSize = 12.sp)
                    }
                    Text("›", color = Color(0xFFB093A2), fontSize = 18.sp)
                }
            }
            Box(Modifier.fillMaxWidth().background(VibeColors.Dark.copy(alpha = 0.92f)).navigationBarsPadding().padding(20.dp, 12.dp, 20.dp, 20.dp)) {
                NeonButton("Save changes", enabled = canSave && !saving, loading = saving, onClick = {
                    saving = true
                    saveScope.launch {
                        try {
                            val body = org.json.JSONObject().put("name", name.trim()).put("city", city.trim()).put("bio", bio.trim())
                                .put("languages", org.json.JSONArray(langs.toList()))
                            val res = com.vibe.app.data.api.Api.put("/me/profile", body)
                            com.vibe.app.data.api.AuthStore.saveUser(res.getJSONObject("user"))
                            ctx.toast("Profile saved")
                            onBack()
                        } catch (e: com.vibe.app.data.api.ApiException) {
                            ctx.toast(e.message)
                        }
                        saving = false
                    }
                })
            }
        }
    }
}

@Composable
private fun LockedField(text: String) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.5.dp, VibeColors.Line, shape).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = VibeColors.TextDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(CallIcons.Lock, null, tint = VibeColors.TextMute, modifier = Modifier.size(14.dp))
        Text("  Can't be changed", color = VibeColors.TextMute, fontSize = 11.sp)
    }
}
