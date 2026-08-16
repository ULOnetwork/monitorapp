package eu.ulonetwork.monitorapp.ui.screens

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.PreferencesManager
import eu.ulonetwork.monitorapp.data.SmtpSettings
import eu.ulonetwork.monitorapp.mail.SmtpMailSender
import kotlinx.coroutines.launch

/**
 * Whether the in-app language picker is shown in Settings. For now the app only ever presents
 * English, but the underlying per-app language plumbing (values-nl, locales_config.xml, the
 * appcompat dependency, [AppCompatDelegate]) is kept fully intact so this can be flipped back to
 * `true` later to re-enable Dutch without any further rework.
 */
private const val LANGUAGE_PICKER_ENABLED = false

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val mailSender = remember { SmtpMailSender() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("587") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fromAddress by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }
    var useTls by remember { mutableStateOf(true) }
    var isSendingTest by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val settings = preferencesManager.getSmtpSettings()
        host = settings.host
        port = settings.port.toString()
        username = settings.username
        password = settings.password
        fromAddress = settings.fromAddress
        toAddress = settings.toAddress
        useTls = settings.useTls
    }

    fun currentSettings(): SmtpSettings = SmtpSettings(
        host = host.trim(),
        port = port.toIntOrNull() ?: 587,
        username = username.trim(),
        password = password,
        fromAddress = fromAddress.trim(),
        toAddress = toAddress.trim(),
        useTls = useTls
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(stringResource(R.string.settings_host)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.settings_port)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.settings_username)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.settings_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = fromAddress,
                onValueChange = { fromAddress = it },
                label = { Text(stringResource(R.string.settings_from)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = toAddress,
                onValueChange = { toAddress = it },
                label = { Text(stringResource(R.string.settings_to)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_use_tls))
                Switch(checked = useTls, onCheckedChange = { useTls = it })
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    preferencesManager.saveSmtpSettings(currentSettings())
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_saved)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_save))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    preferencesManager.saveSmtpSettings(currentSettings())
                    isSendingTest = true
                    scope.launch {
                        val result = mailSender.send(
                            settings = currentSettings(),
                            subject = context.getString(R.string.settings_test_email_subject),
                            body = context.getString(R.string.settings_test_email_body)
                        )
                        isSendingTest = false
                        val message = when (result) {
                            is SmtpMailSender.Result.Success -> context.getString(R.string.settings_test_success)
                            is SmtpMailSender.Result.Failure -> context.getString(R.string.settings_test_failure, result.message)
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                },
                enabled = !isSendingTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSendingTest) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(R.string.settings_test_email))
                }
            }

            if (LANGUAGE_PICKER_ENABLED) {
                Spacer(modifier = Modifier.height(32.dp))
                LanguageSection()
            }
        }
    }
}

/**
 * Lets the user switch the app's UI language between English and Dutch at runtime, using
 * Android's per-app language support ([AppCompatDelegate.setApplicationLocales]). The current
 * selection is read from [AppCompatDelegate.getApplicationLocales], which is the single source
 * of truth (no separate preference flag is kept). Option labels are intentionally shown in their
 * own language ("English" / "Nederlands"), not translated, so a user can find their way
 * regardless of the currently active UI language.
 */
@Composable
private fun LanguageSection() {
    val context = LocalContext.current
    val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isDutch = currentTag.startsWith("nl")

    fun selectLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        // Ensure an immediate visual refresh even on API levels where automatic recreation
        // of a plain ComponentActivity is not guaranteed.
        (context as? Activity)?.recreate()
    }

    Column {
        Text(text = stringResource(R.string.settings_language_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = !isDutch, onClick = { selectLanguage("en") }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = !isDutch, onClick = { selectLanguage("en") })
            Text(text = stringResource(R.string.language_option_english))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = isDutch, onClick = { selectLanguage("nl") }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isDutch, onClick = { selectLanguage("nl") })
            Text(text = stringResource(R.string.language_option_dutch))
        }
    }
}
