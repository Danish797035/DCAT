package com.example.gemini.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.data.agent.AgenticBrowserBootstrap
import com.example.gemini.theme.AccentPalette
import com.example.gemini.theme.ClaudeTerracotta
import com.example.gemini.theme.isAppInDarkTheme

/** D~C@T: Settings → Appearance → Accent colour. */
@Composable
internal fun AccentColorPicker() {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val selected = AccentPalette.presetId
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Accent Colour", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Buttons, highlights and switches use this colour. Kitten is black & white like the logo.",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccentPalette.presets.forEach { preset ->
                val isSel = preset.id == selected
                val swatch = if (isDark) preset.dark else preset.light
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { AccentPalette.select(context, preset.id) }.padding(4.dp)
                ) {
                    Box(
                        Modifier.size(40.dp)
                            .border(
                                BorderStroke(if (isSel) 3.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                                CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(swatch),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) Text("✓", color = if (isDark) preset.onDark else preset.onLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preset.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** D~C@T: Settings → Automation → "Agentic browser (auto-enable browser + Flow MCP)". */
@Composable
internal fun AgenticBrowserToggleCard(cardBg: Color, cardBorder: BorderStroke) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AgenticBrowserBootstrap.isEnabled(context)) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = if (enabled) BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.4f)) else cardBorder
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Agentic browser (auto-enable browser + Flow MCP)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Adds the in-app Browser Automation (browser_mcp) and Google Flow (google_flow_mcp) servers to both " +
                        "Antigravity and Claude Code after sign-in. Servers you remove or disable are left alone; " +
                        "turning this on again re-adds them.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    AgenticBrowserBootstrap.setEnabled(context, it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = ClaudeTerracotta)
            )
        }
    }
}
