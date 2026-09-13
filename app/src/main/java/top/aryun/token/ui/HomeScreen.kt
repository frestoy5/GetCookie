package top.aryun.token.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.aryun.token.data.CUSTOM_SITE_ACCENT
import top.aryun.token.data.CUSTOM_SITE_BADGE
import top.aryun.token.data.CustomSite
import top.aryun.token.data.SITE_PRESETS
import top.aryun.token.data.SiteTarget
import top.aryun.token.data.customSiteTarget
import top.aryun.token.data.hostOf

@Composable
fun HomeScreen(
    customs: List<CustomSite>,
    insets: PaddingValues,
    onConnect: (SiteTarget) -> Unit,
    onAddCustom: () -> Unit,
    onRemoveCustom: (CustomSite) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(insets),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hero") { HeroCard() }

        item(key = "preset-header") { SectionHeader("内置站点") }

        items(SITE_PRESETS, key = { it.id }) { preset ->
            SiteRow(
                badge = preset.badge,
                accent = preset.accent,
                title = preset.name,
                subtitle = hostOf(preset.url),
                onClick = { onConnect(preset) },
            )
        }

        item(key = "custom-header") {
            SectionHeader("自定义网站") {
                TextButton(onClick = onAddCustom) { Text("＋ 新增") }
            }
        }

        if (customs.isEmpty()) {
            item(key = "custom-empty") { EmptyCustomCard(onAddCustom) }
        } else {
            items(customs, key = { it.id }) { site ->
                SiteRow(
                    badge = CUSTOM_SITE_BADGE,
                    accent = CUSTOM_SITE_ACCENT,
                    title = site.name,
                    subtitle = hostOf(site.url),
                    onClick = { onConnect(customSiteTarget(site)) },
                    trailing = {
                        IconButton(onClick = { onRemoveCustom(site) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }

        item(key = "footer") { FooterHint() }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteBadge(badge = "饼", accent = 0xFFE8A33D, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "获取饼干",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "点击「连接」登录后自动抓取 Token",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SiteRow(
    badge: String,
    accent: Long,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteBadge(badge = badge, accent = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            trailing?.invoke()
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text("连接")
            }
        }
    }
}

@Composable
private fun EmptyCustomCard(onAddCustom: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("还没有自定义网站", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "填写备注和网页登录链接，就能用同样的方式获取 Token",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onAddCustom) { Text("＋ 新增网站") }
        }
    }
}

@Composable
private fun FooterHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("使用说明", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            listOf(
                "1. 点击「连接」打开内置浏览器",
                "2. 在页面里完成登录，可用右上角「电脑模式」切换桌面版网页",
                "3. 登录成功后自动返回，并弹出获取到的 Token",
                "4. 弹窗右下角可以复制或保存到本地，也能在「我的」里查看",
            ).forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}
