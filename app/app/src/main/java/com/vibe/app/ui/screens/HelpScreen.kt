package com.vibe.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.R
import com.vibe.app.data.SettingsDemo
import com.vibe.app.data.Ticket
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.CallSheetBg
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.PillChip
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.VibeField
import com.vibe.app.ui.components.rememberFloat
import com.vibe.app.ui.components.toast
import com.vibe.app.ui.theme.VibeColors

private class Topic(val title: String, val category: String, val icon: ImageVector, val tint: Color)

private val TOPICS = listOf(
    Topic("Payments & coins", "Coins", CallIcons.Coin, VibeColors.Gold),
    Topic("Calls & chat", "Calls", CallIcons.Camera, VibeColors.Cyan),
    Topic("Hosting & payouts", "Hosting", CallIcons.Mic, VibeColors.Pink),
    Topic("Account & safety", "Account", CallIcons.User, VibeColors.Green),
)
private val REASON_ICONS = mapOf(
    "Payment" to CallIcons.Coin, "A call" to CallIcons.Camera, "A user" to CallIcons.User,
    "App bug" to CallIcons.Flag, "Hosting" to CallIcons.Mic, "Other" to MeIcons.Help,
)

/** Help & support: search, topics, FAQ with feedback, your requests, contact, report sheet. DEMO content from the server in the real app. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val floatY by rememberFloat()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var openIdx by remember { mutableStateOf(-1) }
    val votes = remember { mutableStateMapOf<Int, Boolean>() }
    val tickets = remember { mutableStateListOf<Ticket>().apply { addAll(SettingsDemo.tickets) } }
    var showReport by remember { mutableStateOf(false) }

    val q = query.trim().lowercase()
    val list = SettingsDemo.faq.withIndex().filter { (_, f) ->
        if (q.isNotEmpty()) (f.question + f.answer).lowercase().contains(q) else if (category == "All") f.popular else f.category == category
    }

    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Green, 300.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-110).dp))
        GlowBlob(VibeColors.Pink, 240.dp, Modifier.align(Alignment.CenterEnd).offset(110.dp, (-40).dp))

        Column(Modifier.fillMaxSize()) {
            PageHeader("Help & support", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                // hero
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.ghost_chat), null, Modifier.size(96.dp).graphicsLayer { translationY = floatY.dp.toPx() })
                    Column {
                        Text(
                            buildAnnotatedString {
                                append("Hi, how can we ")
                                withStyle(SpanStyle(color = VibeColors.Pink, fontWeight = FontWeight.SemiBold)) { append("help") }
                                append("?")
                            },
                            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light, lineHeight = 28.sp,
                        )
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(VibeColors.Green))
                            Text("  Support is online · replies in about an hour", color = VibeColors.Green, fontSize = 11.sp)
                        }
                    }
                }

                // search
                Row(
                    Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Card)
                        .border(1.5.dp, VibeColors.Line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CallIcons.Search, null, tint = VibeColors.TextMute, modifier = Modifier.size(20.dp))
                    BasicTextField(
                        value = query, onValueChange = { query = it; openIdx = -1 }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = SolidColor(VibeColors.Pink),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search: payment, refund, withdraw…", color = VibeColors.TextMute, fontSize = 14.sp); inner() } },
                    )
                    if (query.isNotEmpty()) Text("×", color = VibeColors.TextMute, fontSize = 20.sp, modifier = Modifier.clickable { query = "" })
                }

                // topics
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0..1) {
                            val t = TOPICS[row * 2 + col]
                            TopicCard(t, selected = category == t.category, Modifier.weight(1f)) { category = if (category == t.category) "All" else t.category; query = ""; openIdx = -1 }
                        }
                    }
                }

                // faq
                Row(Modifier.padding(top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(if (q.isNotEmpty()) "Results (${list.size})" else if (category == "All") "Popular questions" else category)
                    if (q.isEmpty() && category != "All") Text("  Show popular", color = VibeColors.Pink, fontSize = 11.sp, modifier = Modifier.clickable { category = "All" })
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No answers found", color = VibeColors.TextDim, fontSize = 14.sp)
                    Text("Report a problem instead", color = VibeColors.Pink, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp).clickable { showReport = true })
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.forEach { (i, f) ->
                        FaqItem(f.question, f.answer, open = openIdx == i, vote = votes[i], onToggle = { openIdx = if (openIdx == i) -1 else i }, onVote = { good ->
                            votes[i] = good
                            ctx.toast(if (good) "Thanks for the feedback" else "Sorry. Try Chat with us")
                        })
                    }
                }

                // tickets
                SectionLabel("Your requests", Modifier.padding(top = 26.dp, bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { tickets.forEach { TicketRow(it) { ctx.toast("Ticket conversation opens here") } } }

                // contact
                SectionLabel("Talk to us", Modifier.padding(top = 26.dp, bottom = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ContactCard("Chat with us", "Fastest way to get help", MeIcons.Chat, VibeColors.Cyan, Modifier.weight(1f)) { ctx.toast("Support chat opens here") }
                    ContactCard("Report a problem", "Payment, call, user or bug", CallIcons.Flag, VibeColors.Red, Modifier.weight(1f)) { showReport = true }
                }
                val sShape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(sShape).background(VibeColors.Card).border(1.dp, Color.White.copy(alpha = 0.10f), sShape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CallIcons.Shield, null, tint = VibeColors.Gold, modifier = Modifier.size(20.dp))
                    Text("VIBE support will never ask for your OTP, password or UPI PIN. Do not share them with anyone.", color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Text(
                    "Support hours, answers and contact details are managed by VIBE.", color = Color(0xFF4B5563), fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }
    }

    if (showReport) ReportSheet(
        onDismiss = { showReport = false },
        onSend = { reason, text ->
            tickets.add(0, Ticket("$reason · ${text.trim().take(28)}", true, "Just now", false))
            showReport = false
            ctx.toast("Report sent · we will reply in your notifications")
        },
    )
}

@Composable
private fun TopicCard(t: Topic, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val count = SettingsDemo.faq.count { it.category == t.category }
    Column(
        modifier.clip(shape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(t.tint.copy(alpha = 0.14f), VibeColors.Card, VibeColors.Card)))
            .border(1.dp, if (selected) t.tint.copy(alpha = 0.6f) else VibeColors.Line, shape).clickable(onClick = onClick).padding(14.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.25f)).border(1.dp, t.tint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(t.icon, null, tint = t.tint, modifier = Modifier.size(19.dp)) }
        Text(t.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
        Text("$count answers", color = VibeColors.TextDim, fontSize = 11.sp)
    }
}

@Composable
private fun FaqItem(question: String, answer: String, open: Boolean, vote: Boolean?, onToggle: () -> Unit, onVote: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, if (open) VibeColors.Pink.copy(alpha = 0.45f) else VibeColors.Line, shape).animateContentSize(),
    ) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(question, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("›", color = if (open) VibeColors.Pink else VibeColors.TextMute, fontSize = 20.sp, modifier = Modifier.graphicsLayer { rotationZ = if (open) 90f else 0f })
        }
        if (open) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
            Text(answer, color = VibeColors.TextDim, fontSize = 13.sp, lineHeight = 20.sp)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Was this helpful?", color = VibeColors.TextMute, fontSize = 11.sp)
                VoteChip("Yes", MeIcons.ThumbUp, vote == true) { onVote(true) }
                VoteChip("No", MeIcons.ThumbDown, vote == false) { onVote(false) }
            }
        }
    }
}

@Composable
private fun VoteChip(label: String, icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    val c = if (on) VibeColors.Green else VibeColors.TextDim
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(if (on) VibeColors.Green.copy(alpha = 0.10f) else Color.Transparent)
            .border(1.dp, if (on) VibeColors.Green else Color(0xFF2C2C3C), CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = c, modifier = Modifier.size(14.dp))
        Text(label, color = c, fontSize = 12.sp)
    }
}

@Composable
private fun TicketRow(t: Ticket, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val tint = if (t.open) VibeColors.Gold else VibeColors.Green
    Row(
        Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.10f)).border(1.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(if (t.open) MeIcons.Clock else CallIcons.Check, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(t.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                buildAnnotatedString {
                    append(t.time)
                    if (t.newReply) withStyle(SpanStyle(color = VibeColors.Cyan)) { append(" · New reply") }
                },
                color = VibeColors.TextMute, fontSize = 11.sp,
            )
        }
        Box(Modifier.clip(CircleShape).background(tint.copy(alpha = 0.15f)).padding(horizontal = 9.dp, vertical = 3.dp)) {
            Text(if (t.open) "OPEN" else "RESOLVED", color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun ContactCard(title: String, sub: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(tint.copy(alpha = 0.12f), VibeColors.Card, VibeColors.Card)))
            .border(1.dp, tint.copy(alpha = 0.30f), shape).clickable(onClick = onClick).padding(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.25f)).border(1.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
        Text(sub, color = VibeColors.TextDim, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(onDismiss: () -> Unit, onSend: (reason: String, text: String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var shot by remember { mutableStateOf(false) }
    val extras = remember { mutableStateMapOf<String, String>() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CallSheetBg) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("Report a problem", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Pick what it is about so the right team sees it.", color = VibeColors.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsDemo.reportReasons.forEach { r -> PillChip(r, r == reason, { reason = r }) }
            }
            SettingsDemo.reportExtra[reason]?.forEach { hint ->
                VibeField(extras[hint].orEmpty(), { extras[hint] = it }, hint, modifier = Modifier.padding(top = 12.dp))
            }
            VibeField(text, { text = it.take(300) }, "Tell us what happened", singleLine = false, minHeight = 90.dp, modifier = Modifier.padding(top = 12.dp))
            val sShape = RoundedCornerShape(16.dp)
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(sShape).border(1.dp, if (shot) VibeColors.Green.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f), sShape)
                    .clickable { shot = !shot },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(CallIcons.Photo, null, tint = if (shot) VibeColors.Green else VibeColors.TextDim, modifier = Modifier.size(20.dp))
                Text(if (shot) "  Screenshot added ✓" else "  Add a screenshot (optional)", color = if (shot) VibeColors.Green else VibeColors.TextDim, fontSize = 14.sp)
            }
            Box(Modifier.padding(top = 16.dp)) {
                NeonButton("Send report", enabled = reason.isNotEmpty() && text.trim().length >= 5, onClick = { onSend(reason, text) })
            }
        }
    }
}
