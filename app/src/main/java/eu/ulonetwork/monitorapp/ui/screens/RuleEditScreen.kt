package eu.ulonetwork.monitorapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.db.AppDatabase
import eu.ulonetwork.monitorapp.data.db.KeywordRule
import eu.ulonetwork.monitorapp.data.db.MatchMode
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(ruleId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val isNew = ruleId <= 0
    var existingRule by remember { mutableStateOf<KeywordRule?>(null) }

    var keyword by remember { mutableStateOf("") }
    var matchMode by remember { mutableStateOf(MatchMode.CONTAINS) }
    var caseSensitive by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(true) }
    var notifyLocal by remember { mutableStateOf(true) }
    var notifyEmail by remember { mutableStateOf(false) }
    var appPackageFilter by remember { mutableStateOf("") }
    var cooldownMinutes by remember { mutableStateOf("10") }
    var showValidationError by remember { mutableStateOf(false) }
    var matchModeMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) {
        if (!isNew) {
            val rule = database.keywordRuleDao().getById(ruleId)
            existingRule = rule
            if (rule != null) {
                keyword = rule.keyword
                matchMode = rule.matchMode
                caseSensitive = rule.caseSensitive
                enabled = rule.enabled
                notifyLocal = rule.notifyLocal
                notifyEmail = rule.notifyEmail
                appPackageFilter = rule.appPackageFilter ?: ""
                cooldownMinutes = rule.cooldownMinutes.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(if (isNew) R.string.rule_add else R.string.rule_edit),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text(stringResource(R.string.rule_keyword)) },
            supportingText = { Text(stringResource(R.string.rule_keyword_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = matchModeMenuExpanded,
            onExpandedChange = { matchModeMenuExpanded = it }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = if (matchMode == MatchMode.CONTAINS) {
                    stringResource(R.string.rule_match_contains)
                } else {
                    stringResource(R.string.rule_match_not_contains)
                },
                onValueChange = {},
                label = { Text(stringResource(R.string.rule_match_mode)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = matchModeMenuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(
                expanded = matchModeMenuExpanded,
                onDismissRequest = { matchModeMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rule_match_contains)) },
                    onClick = {
                        matchMode = MatchMode.CONTAINS
                        matchModeMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rule_match_not_contains)) },
                    onClick = {
                        matchMode = MatchMode.NOT_CONTAINS
                        matchModeMenuExpanded = false
                    }
                )
            }
        }
        if (matchMode == MatchMode.NOT_CONTAINS) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.rule_not_contains_requires_app),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = appPackageFilter,
            onValueChange = { appPackageFilter = it },
            label = { Text(stringResource(R.string.rule_app_filter)) },
            placeholder = { Text(stringResource(R.string.rule_app_filter_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = cooldownMinutes,
            onValueChange = { cooldownMinutes = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.rule_cooldown)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        SwitchRow(label = stringResource(R.string.rule_enabled), checked = enabled, onCheckedChange = { enabled = it })
        SwitchRow(label = stringResource(R.string.rule_case_sensitive), checked = caseSensitive, onCheckedChange = { caseSensitive = it })
        SwitchRow(label = stringResource(R.string.rule_notify_local), checked = notifyLocal, onCheckedChange = { notifyLocal = it })
        SwitchRow(label = stringResource(R.string.rule_notify_email), checked = notifyEmail, onCheckedChange = { notifyEmail = it })

        if (showValidationError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.rule_not_contains_requires_app),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.rule_cancel))
            }
            Button(
                onClick = {
                    val trimmedFilter = appPackageFilter.trim().ifBlank { null }
                    if (matchMode == MatchMode.NOT_CONTAINS && trimmedFilter == null) {
                        showValidationError = true
                        return@Button
                    }
                    if (keyword.isBlank()) {
                        return@Button
                    }
                    showValidationError = false

                    val rule = KeywordRule(
                        id = existingRule?.id ?: 0,
                        keyword = keyword.trim(),
                        matchMode = matchMode,
                        caseSensitive = caseSensitive,
                        enabled = enabled,
                        notifyLocal = notifyLocal,
                        notifyEmail = notifyEmail,
                        appPackageFilter = trimmedFilter,
                        cooldownMinutes = cooldownMinutes.toIntOrNull() ?: 10,
                        lastTriggeredAt = existingRule?.lastTriggeredAt
                    )
                    scope.launch {
                        database.keywordRuleDao().upsert(rule)
                        onDone()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.rule_save))
            }
        }

        if (!isNew) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val rule = existingRule
                    if (rule != null) {
                        scope.launch {
                            database.keywordRuleDao().delete(rule)
                            onDone()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rule_delete))
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
