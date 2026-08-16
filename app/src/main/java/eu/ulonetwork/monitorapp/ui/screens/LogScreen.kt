package eu.ulonetwork.monitorapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.db.AlertLogEntry
import eu.ulonetwork.monitorapp.data.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val entries by database.alertLogDao().observeAll().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.log_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Text(text = stringResource(R.string.log_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(entries, key = { it.id }) { entry ->
                    LogRow(entry)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: AlertLogEntry) {
    val formatter = remember { SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = formatter.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(R.string.log_entry_title, entry.matchedKeyword, entry.appPackage),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = entry.textSnippet, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            val notificationChannelLabel = stringResource(R.string.log_channel_notification)
            val emailChannelLabel = stringResource(R.string.log_channel_email)
            val noneLabel = stringResource(R.string.log_channel_none)
            val channels = buildList {
                if (entry.notifiedLocal) add(notificationChannelLabel)
                if (entry.notifiedEmail) add(emailChannelLabel)
            }.joinToString(", ").ifBlank { noneLabel }
            Text(text = stringResource(R.string.log_channels_label, channels), style = MaterialTheme.typography.bodySmall)
        }
    }
}
