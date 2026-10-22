package com.vibe.app.ui.screens.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.app.ui.components.BackButton
import com.vibe.app.ui.components.CallIcons
import com.vibe.app.ui.components.GlowBlob
import com.vibe.app.ui.components.MeIcons
import com.vibe.app.ui.components.NeonButton
import com.vibe.app.ui.components.PageHeader
import com.vibe.app.ui.components.SectionLabel
import com.vibe.app.ui.components.TileIcon
import com.vibe.app.ui.theme.VibeBrush
import com.vibe.app.ui.theme.VibeColors

// DEMO TEXT. The real Terms, Privacy Policy, Guidelines and safety tips are managed from the admin panel (Content) and must be
// reviewed by a lawyer before launch.

private class LegalSection(val title: String, val body: String? = null, val bullets: List<String> = emptyList())

private val TERMS = listOf(
    LegalSection("1. Who can use VIBE", "You must be 18 years or older. We may ask you to prove your age and identity. Accounts of people under 18 are removed."),
    LegalSection("2. Your account", bullets = listOf("Give correct details and keep your login safe.", "One person, one account. Do not sell or share accounts.", "You are responsible for what happens on your account.")),
    LegalSection("3. Calls, chat and coins", "Calls are charged per minute and chat can be charged per message at the rates shown before you start. Coins are bought with real money, are used only inside VIBE and are not refundable, except when a payment or call failed because of an error on our side. Prices and offers can change."),
    LegalSection("4. Hosts and earnings", "Anyone 18 or older can apply to be a host. Hosts are reviewed by VIBE and earn a share of what users spend, as set by VIBE. Earnings can be withdrawn after verification, above the minimum amount, after fees and applicable tax. Hosts are not employees of VIBE."),
    LegalSection("5. Photos, videos and statuses", "You own your content but give VIBE permission to show it in the app. Locked photos and videos are unlocked with coins, and the host earns a share. We check content and may remove anything that breaks the Community guidelines."),
    LegalSection("6. What is not allowed", bullets = listOf("Nudity, sexual content or sexual behaviour.", "Abuse, threats, hate or harassment.", "Asking for money, phone numbers, UPI or social ids outside VIBE.", "Fake profiles, scams, spam or recording calls without consent."), body = "Breaking these rules can lead to a warning, a temporary ban or a permanent ban, and unpaid earnings may be held."),
    LegalSection("7. Ending your account", "You can delete your account from Settings at any time. Unused coins and unpaid earnings are lost when you delete, so withdraw first. We may suspend or remove accounts that break these terms."),
    LegalSection("8. Our responsibility", "VIBE is provided as is. We try to keep it safe and working but cannot promise it will never fail or that every user behaves well. Please use the Block and Report tools."),
    LegalSection("9. Contact", "Questions? Open Help & support in Settings."),
)

private val PRIVACY = listOf(
    LegalSection("What we collect", bullets = listOf("Account details: phone number, name, date of birth, gender, city, languages, photos and videos you add.", "For hosts: selfie, ID and PAN, and UPI or bank details for payouts.", "Activity: calls, messages, gifts, coin and payment history, visitors and statuses.", "Device information, crash logs and app usage to keep the app working and safe.")),
    LegalSection("Why we use it", bullets = listOf("To run calls, chat, wallet, payouts and gifts.", "To verify age and identity, stop fraud and keep people safe.", "To review reports and enforce the Community guidelines.", "To send you notifications you asked for.")),
    LegalSection("Camera, microphone and storage", "Camera and microphone are used only for calls, your selfie check, videos and photos you choose to add. Calls are not recorded by VIBE."),
    LegalSection("Who sees your information", "Other users see your name, age, city, photos, status and online state according to your privacy settings. We share data only with services that help us run VIBE, such as payment, SMS and cloud providers, or when the law requires."),
    LegalSection("Your choices", bullets = listOf("Hide your online status and visitor trail in Settings.", "Block and report anyone.", "Edit your profile or delete your account at any time.")),
    LegalSection("How long we keep data", "We keep data while your account is active. After you delete it we remove or anonymise your data, except records we must keep for payments, tax, safety or the law."),
    LegalSection("Contact", "For privacy questions open Help & support in Settings."),
)

@Composable
private fun LegalPage(title: String, other: String, onOther: () -> Unit, onBack: () -> Unit, intro: String, sections: List<LegalSection>, topBox: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack)
                Text(title, color = VibeColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    Modifier.height(36.dp).clip(CircleShape).background(VibeColors.Card).border(1.dp, VibeColors.Line, CircleShape).clickable(onClick = onOther).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("$other ›", color = VibeColors.Ink, fontSize = 12.sp) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text("Last updated: 15 October 2026 · Version 1.0", color = VibeColors.TextMute, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                topBox?.invoke()
                val shape = RoundedCornerShape(16.dp)
                Text(intro, color = VibeColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(16.dp))
                sections.forEach { s ->
                    Text(s.title, color = VibeColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
                    if (s.body != null && s.bullets.isEmpty()) Text(s.body, color = VibeColors.TextDim, fontSize = 13.sp, lineHeight = 21.sp)
                    s.bullets.forEach { Text("•  $it", color = VibeColors.TextDim, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp)) }
                    if (s.body != null && s.bullets.isNotEmpty()) Text(s.body, color = VibeColors.TextDim, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Text(title.let { "VIBE · $it v1.0" }, color = Color(0xFFB093A2), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
            }
        }
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit, onPrivacy: () -> Unit) = LegalPage(
    "Terms of Service", "Privacy", onPrivacy, onBack,
    "This is a short summary page. The full, legally reviewed text is managed by VIBE and can be updated at any time. By using VIBE you agree to these terms.", TERMS,
)

@Composable
fun PrivacyScreen(onBack: () -> Unit, onTerms: () -> Unit) = LegalPage(
    "Privacy Policy", "Terms", onTerms, onBack,
    "This is a summary page. The full, legally reviewed policy is managed by VIBE and can be updated at any time.", PRIVACY,
    topBox = {
        val shape = RoundedCornerShape(16.dp)
        Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(shape).background(VibeColors.Green.copy(alpha = 0.05f)).border(1.dp, VibeColors.Green.copy(alpha = 0.3f), shape).padding(horizontal = 16.dp, vertical = 6.dp)) {
            listOf("We never sell your personal data.", "Your selfie and ID are seen only by the VIBE verification team.", "You can delete your account and data from Settings.").forEach {
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("✓", color = VibeColors.Green, fontSize = 16.sp)
                    Text(it, color = Color(0xFF5A3F4D), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    },
)

// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun GuidelinesScreen(onBack: () -> Unit, onReport: () -> Unit, onBlocked: () -> Unit) {
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Pink, 280.dp, Modifier.align(Alignment.TopEnd).offset(100.dp, (-100).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Community guidelines", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(
                    "VIBE is for friendly, respectful conversations between people who are 18 or older. These rules apply to everyone: users and hosts.",
                    color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp),
                )

                RuleBox("Please do", VibeColors.Green, "✓", listOf(
                    "Be kind and respect the other person's no.", "Use your own real photos and your real age.",
                    "Keep calls and payments inside VIBE.", "Block and report anything that feels wrong.",
                ))
                RuleBox("Never do", VibeColors.Red, "✕", listOf(
                    "Nudity or sexual content, in photos, videos, calls or chat.", "Abuse, threats or hate against anyone.",
                    "Asking for money, phone numbers, UPI or social ids outside VIBE.", "Fake profiles, stolen photos, scams or spam.",
                    "Anyone under 18. We remove these accounts.", "Recording or sharing a call or someone's photo without their consent.",
                ))

                SectionLabel("What happens if rules are broken", Modifier.padding(top = 28.dp, bottom = 16.dp))
                LadderStep("1", "Warning", "The content is removed and you get a message", VibeColors.Gold, last = false)
                LadderStep("2", "Temporary ban", "You can't use calls, chat or your host status for some days", Color(0xFFFB923C), last = false)
                LadderStep("3", "Permanent ban", "The account is closed. Serious cases skip straight to this. Unpaid earnings may be held", VibeColors.Red, last = true)

                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton("Report someone", Modifier.weight(1f), onReport)
                    GhostButton("Blocked users", Modifier.weight(1f), onBlocked)
                }
                Text(
                    "The rules and how we act on them are managed by VIBE and may be updated.", color = Color(0xFFB093A2), fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun RuleBox(title: String, color: Color, mark: String, rules: List<String>) {
    SectionLabel(title, Modifier.padding(top = 24.dp, bottom = 8.dp))
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(color.copy(alpha = 0.05f)).border(1.dp, color.copy(alpha = 0.25f), shape).padding(horizontal = 16.dp)) {
        rules.forEachIndexed { i, r ->
            Row(Modifier.padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Text(mark, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                Text(r, color = Color(0xFF3D2230), fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            }
            if (i < rules.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.06f)))
        }
    }
}

@Composable
private fun LadderStep(n: String, title: String, sub: String, color: Color, last: Boolean) {
    Row(Modifier.padding(bottom = if (last) 12.dp else 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.10f)).border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(n, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Column {
            Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun GhostButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Card).border(1.dp, VibeColors.Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

// ---------------------------------------------------------------------------------------------------------------------

@Composable
fun SafetyCenterScreen(
    onBack: () -> Unit, onReport: () -> Unit, onBlocked: () -> Unit, onGuidelines: () -> Unit,
    onSettings: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit, onSupport: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(VibeColors.Dark)) {
        GlowBlob(VibeColors.Cyan, 300.dp, Modifier.align(Alignment.TopStart).offset((-100).dp, (-110).dp))
        Column(Modifier.fillMaxSize()) {
            PageHeader("Safety center", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(VibeColors.Cyan.copy(alpha = 0.10f)).border(1.dp, VibeColors.Cyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(CallIcons.Shield, null, tint = VibeColors.Cyan, modifier = Modifier.size(36.dp)) }
                    Text("Your safety comes first. Here are the tools and tips to stay safe on VIBE.", color = VibeColors.TextDim, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                }

                SectionLabel("Your tools", Modifier.padding(top = 28.dp, bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToolCard("Report", "Tell our team what went wrong", CallIcons.Flag, VibeColors.Red, Modifier.weight(1f), onReport)
                        ToolCard("Blocked users", "See or undo your blocks", CallIcons.Ban, VibeColors.Pink, Modifier.weight(1f), onBlocked)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToolCard("Guidelines", "What is and isn't allowed", MeIcons.Doc, VibeColors.Gold, Modifier.weight(1f), onGuidelines)
                        ToolCard("Privacy settings", "Hide online status and visits", CallIcons.Online, VibeColors.Green, Modifier.weight(1f), onSettings)
                    }
                }

                SectionLabel("Stay safe", Modifier.padding(top = 28.dp, bottom = 4.dp))
                val shape = RoundedCornerShape(16.dp)
                Column(Modifier.fillMaxWidth().clip(shape).background(VibeColors.Card).border(1.dp, VibeColors.Line, shape).padding(horizontal = 16.dp)) {
                    Tip(CallIcons.Lock, VibeColors.Red, "Never share your OTP or UPI PIN", "VIBE staff will never ask for them.")
                    Tip(CallIcons.Coin, VibeColors.Gold, "Don't pay outside VIBE", "If someone asks for money on UPI or another app, it is a scam. Report them.")
                    Tip(MeIcons.Phone, VibeColors.Cyan, "Keep your number private", "Stay on VIBE calls. Do not give phone numbers or social ids to people you just met.")
                    Tip(CallIcons.User, VibeColors.Pink, "Be careful meeting anyone", "We do not recommend meeting people from the app. If you do, tell a friend and choose a public place.")
                    Tip(CallIcons.Flag, VibeColors.Green, "Report anything that feels wrong", "Reports are private and reviewed by our team, usually within 24 hours.", last = true)
                }

                Row(
                    Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(VibeColors.Red.copy(alpha = 0.05f)).border(1.dp, VibeColors.Red.copy(alpha = 0.3f), shape).padding(16.dp),
                ) {
                    Text("In danger? ", color = VibeColors.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Call your local emergency number first. For women's helpline in India dial 181.", color = Color(0xFF5A3F4D), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                }

                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton("Terms", Modifier.weight(1f), onTerms)
                    GhostButton("Privacy Policy", Modifier.weight(1f), onPrivacy)
                }
                Box(Modifier.padding(top = 12.dp, bottom = 32.dp)) { NeonButton("Contact support", onSupport) }
            }
        }
    }
}

@Composable
private fun ToolCard(title: String, sub: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(Brush.linearGradient(listOf(tint.copy(alpha = 0.14f), VibeColors.Card, VibeColors.Card)))
            .border(1.dp, VibeColors.Line, shape).clickable(onClick = onClick).padding(14.dp),
    ) {
        TileIcon(icon, tint, 40.dp)
        Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
        Text(sub, color = VibeColors.TextDim, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun Tip(icon: ImageVector, tint: Color, title: String, sub: String, last: Boolean = false) {
    Row(Modifier.padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TileIcon(icon, tint, 36.dp)
        Column {
            Text(title, color = VibeColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = VibeColors.TextMute, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(VibeColors.Ink.copy(alpha = 0.06f)))
}
