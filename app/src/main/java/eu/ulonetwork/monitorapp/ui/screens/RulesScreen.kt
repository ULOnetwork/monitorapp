package eu.ulonetwork.monitorapp.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.KeywordRulesCodec
import eu.ulonetwork.monitorapp.data.db.AppDatabase
import eu.ulonetwork.monitorapp.data.db.KeywordRule
import eu.ulonetwork.monitorapp.data.db.MatchMode
import kotlinx.coroutines.launch

/**
 * Name of the file the export code is also written to (app-private internal storage), mirroring
 * the same fallback used for Mailjet settings in SettingsScreen.kt. Useful when clipboard sync
 * doesn't reach the host (e.g. driving the device remotely over scrcpy); retrieve with
 * `adb shell run-as eu.ulonetwork.monitorapp cat files/keyword-rules-export.txt`.
 */
private const val RULES_EXPORT_FILE_NAME = "keyword-rules-export.txt"

private fun writeRulesExportFile(context: Context, exportCode: String) {
    try {
        context.openFileOutput(RULES_EXPORT_FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(exportCode.toByteArray(Charsets.UTF_8))
        }
    } catch (e: Exception) {
        Log.w("RulesScreen", "Failed to write rules export file: ${e.message}", e)
    }
}

@Composable
fun RulesScreen(onAddRule: () -> Unit, onEditRule: (Long) -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rules by database.keywordRuleDao().observeAll().collectAsState(initial = emptyList())

    var exportCode by remember { mutableStateOf("") }
    var importInput by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.rule_add))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            Text(text = stringResource(R.string.rules_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            if (rules.isEmpty()) {
                Text(text = stringResource(R.string.rules_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            onClick = { onEditRule(rule.id) },
                            onDelete = {
                                scope.launch { database.keywordRuleDao().delete(rule) }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.rules_transfer_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.rules_transfer_explanation), style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    exportCode = KeywordRulesCodec.encode(rules)
                    clipboardManager.setText(AnnotatedString(exportCode))
                    writeRulesExportFile(context, exportCode)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_export_copied)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rules_export_button))
            }
            if (exportCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exportCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_export_label)) },
                    supportingText = { Text(stringResource(R.string.settings_export_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_export_file_hint, RULES_EXPORT_FILE_NAME),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = importInput,
                onValueChange = { importInput = it },
                label = { Text(stringResource(R.string.settings_import_label)) },
                placeholder = { Text(stringResource(R.string.rules_import_placeholder)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val decoded = KeywordRulesCodec.decode(importInput)
                    if (decoded == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_import_invalid)) }
                    } else {
                        scope.launch {
                            for (rule in decoded) {
                                database.keywordRuleDao().upsert(rule)
                            }
                            importInput = ""
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.rules_import_success, decoded.size)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_import_button))
            }
        }
    }
}

@Composable
private fun RuleRow(rule: KeywordRule, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
                    Text(text = rule.keyword, style = MaterialTheme.typography.titleMedium)
                    val modeLabel = if (rule.matchMode == MatchMode.CONTAINS) {
                        stringResource(R.string.rule_match_contains)
                    } else {
                        stringResource(R.string.rule_match_not_contains)
                    }
                    val appLabel = rule.appPackageFilter ?: stringResource(R.string.rule_row_all_apps)
                    val statusLabel = if (rule.enabled) {
                        stringResource(R.string.rule_row_status_active)
                    } else {
                        stringResource(R.string.rule_row_status_disabled)
                    }
                    Text(
                        text = stringResource(R.string.rule_row_summary, modeLabel, appLabel, statusLabel),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.rule_delete))
            }
        }
    }
}
