package eu.ulonetwork.monitorapp.ui.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.db.AppDatabase
import eu.ulonetwork.monitorapp.data.db.KeywordRule
import eu.ulonetwork.monitorapp.data.db.MatchMode
import kotlinx.coroutines.launch

@Composable
fun RulesScreen(onAddRule: () -> Unit, onEditRule: (Long) -> Unit) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val rules by database.keywordRuleDao().observeAll().collectAsState(initial = emptyList())

    Scaffold(
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
                LazyColumn {
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
