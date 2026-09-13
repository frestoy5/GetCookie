package top.aryun.token.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.aryun.token.data.CUSTOM_SITE_ACCENT
import top.aryun.token.data.SITE_PRESETS
import top.aryun.token.data.TokenRecord
import top.aryun.token.data.TokenStore
import top.aryun.token.data.hostOf

@Composable
fun MineScreen(
    records: List<TokenRecord>,
    insets: PaddingValues,
    onCopy: (TokenRecord) -> Unit,
    onSave: (TokenRecord) -> Unit,
    onDelete: (TokenRecord) -> Unit,
    onOpenDetail: (TokenRecord) -> Unit,
    onExportAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (records.isEmpty()) {
        EmptyState(modifier = modifier.fillMaxSize().padding(insets))
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(insets),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("已获取的 Token", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "共 ${records.size} 条，点击卡片查看完整内容",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = onExportAll) { Text("导出全部") }
                }
            }
        }

        items(records, key = { it.id }) { record ->
            RecordCard(
                record = record,
                onCopy = { onCopy(record) },
                onSave = { onSave(record) },
                onDelete = { onDelete(record) },
                onOpenDetail = { onOpenDetail(record) },
            )
        }
    }
}

@Composable
private fun RecordCard(
    record: TokenRecord,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenDetail,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SiteBadge(badge = badgeFor(record.site), accent = accentFor(record.site), size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.site,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${TokenStore.formatTime(record.createdAt)} · ${hostOf(record.url)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "获取的 Token",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "字段：${record.key}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            TokenValueBox(value = record.value, maxLines = 3)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSave) { Text("保存到本地") }
                Spacer(Modifier.width(4.dp))
                Button(onClick = onCopy) { Text("复制") }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SiteBadge(badge = "饼", accent = 0xFFE8A33D, size = 64.dp)
            Spacer(Modifier.height(18.dp))
            Text("还没有获取到的 Token", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "回到首页点击「连接」登录一次，获取到的 Token 就会出现在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

fun accentFor(site: String): Long =
    SITE_PRESETS.firstOrNull { it.name == site }?.accent ?: CUSTOM_SITE_ACCENT

fun badgeFor(site: String): String =
    SITE_PRESETS.firstOrNull { it.name == site }?.badge ?: site.take(1).ifBlank { "自" }
