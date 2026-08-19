package eu.ulonetwork.monitorapp.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.MailjetSettings
import eu.ulonetwork.monitorapp.data.MailjetSettingsCodec
import eu.ulonetwork.monitorapp.data.PreferencesManager
import eu.ulonetwork.monitorapp.data.TelegramSettings
import eu.ulonetwork.monitorapp.data.TelegramSettingsCodec
import eu.ulonetwork.monitorapp.mail.MailjetMailSender
import eu.ulonetwork.monitorapp.telegram.TelegramSender
import kotlinx.coroutines.launch

/**
 * Whether the in-app language picker is shown in Settings. For now the app only ever presents
 * English, but the underlying per-app language plumbing (values-nl, locales_config.xml, the
 * appcompat dependency, [AppCompatDelegate]) is kept fully intact so this can be flipped back to
 * `true` later to re-enable Dutch without any further rework.
 */
private const val LANGUAGE_PICKER_ENABLED = false

/**
 * Name of the file the export code is also written to (app-private internal storage, via
 * [Context.openFileOutput]). Clipboard sync doesn't always reach the host when the device is
 * being driven remotely (e.g. over scrcpy), so this gives a fallback way to retrieve the code
 * with `adb shell run-as eu.ulonetwork.monitorapp cat files/mailjet-export.txt` — no storage
 * permission needed since it's the app's own private internal storage.
 */
private const val EXPORT_FILE_NAME = "mailjet-export.txt"

/** Same rationale as [EXPORT_FILE_NAME], for the Telegram settings export below. */
private const val TELEGRAM_EXPORT_FILE_NAME = "telegram-export.txt"

private fun writeExportFile(context: Context, fileName: String, exportCode: String) {
    try {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(exportCode.toByteArray(Charsets.UTF_8))
        }
    } catch (e: Exception) {
        Log.w("SettingsScreen", "Failed to write export file: ${e.message}", e)
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val mailSender = remember { MailjetMailSender() }
    val telegramSender = remember { TelegramSender() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var apiKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var fromAddress by remember { mutableStateOf("") }
    var fromName by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }
    var isSendingTest by remember { mutableStateOf(false) }
    var exportCode by remember { mutableStateOf("") }
    var importInput by remember { mutableStateOf("") }

    var telegramBotToken by remember { mutableStateOf("") }
    var telegramChatId by remember { mutableStateOf("") }
    var isSendingTelegramTest by remember { mutableStateOf(false) }
    var telegramExportCode by remember { mutableStateOf("") }
    var telegramImportInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val settings = preferencesManager.getMailjetSettings()
        apiKey = settings.apiKey
        secretKey = settings.secretKey
        fromAddress = settings.fromAddress
        fromName = settings.fromName
        toAddress = settings.toAddress

        val telegramSettings = preferencesManager.getTelegramSettings()
        telegramBotToken = telegramSettings.botToken
        telegramChatId = telegramSettings.chatId
    }

    fun currentSettings(): MailjetSettings = MailjetSettings(
        apiKey = apiKey.trim(),
        secretKey = secretKey.trim(),
        fromAddress = fromAddress.trim(),
        fromName = fromName.trim(),
        toAddress = toAddress.trim()
    )

    fun currentTelegramSettings(): TelegramSettings = TelegramSettings(
        botToken = telegramBotToken.trim(),
        chatId = telegramChatId.trim()
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.settings_explanation), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.settings_api_key)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                label = { Text(stringResource(R.string.settings_secret_key)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = fromAddress,
                onValueChange = { fromAddress = it },
                label = { Text(stringResource(R.string.settings_from)) },
                supportingText = { Text(stringResource(R.string.settings_from_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = fromName,
                onValueChange = { fromName = it },
                label = { Text(stringResource(R.string.settings_from_name)) },
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

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    preferencesManager.saveMailjetSettings(currentSettings())
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_saved)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_save))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    preferencesManager.saveMailjetSettings(currentSettings())
                    isSendingTest = true
                    scope.launch {
                        val result = mailSender.send(
                            settings = currentSettings(),
                            subject = context.getString(R.string.settings_test_email_subject),
                            body = context.getString(R.string.settings_test_email_body)
                        )
                        isSendingTest = false
                        val message = when (result) {
                            is MailjetMailSender.Result.Success -> context.getString(R.string.settings_test_success)
                            is MailjetMailSender.Result.Failure -> context.getString(R.string.settings_test_failure, result.message)
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

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(R.string.settings_transfer_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.settings_transfer_explanation), style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    exportCode = MailjetSettingsCodec.encode(currentSettings())
                    clipboardManager.setText(AnnotatedString(exportCode))
                    writeExportFile(context, EXPORT_FILE_NAME, exportCode)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_export_copied)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_export_button))
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
                    text = stringResource(R.string.settings_export_file_hint, EXPORT_FILE_NAME),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = importInput,
                onValueChange = { importInput = it },
                label = { Text(stringResource(R.string.settings_import_label)) },
                placeholder = { Text(stringResource(R.string.settings_import_placeholder)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val decoded = MailjetSettingsCodec.decode(importInput)
                    if (decoded == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_import_invalid)) }
                    } else {
                        apiKey = decoded.apiKey
                        secretKey = decoded.secretKey
                        fromAddress = decoded.fromAddress
                        fromName = decoded.fromName
                        toAddress = decoded.toAddress
                        importInput = ""
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_import_success)) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_import_button))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(R.string.settings_telegram_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.settings_telegram_explanation), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = telegramBotToken,
                onValueChange = { telegramBotToken = it },
                label = { Text(stringResource(R.string.settings_telegram_bot_token)) },
                supportingText = { Text(stringResource(R.string.settings_telegram_bot_token_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = telegramChatId,
                onValueChange = { telegramChatId = it },
                label = { Text(stringResource(R.string.settings_telegram_chat_id)) },
                supportingText = { Text(stringResource(R.string.settings_telegram_chat_id_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    preferencesManager.saveTelegramSettings(currentTelegramSettings())
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_saved)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_save))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    preferencesManager.saveTelegramSettings(currentTelegramSettings())
                    isSendingTelegramTest = true
                    scope.launch {
                        val result = telegramSender.send(
                            settings = currentTelegramSettings(),
                            text = context.getString(R.string.settings_telegram_test_message)
                        )
                        isSendingTelegramTest = false
                        val message = when (result) {
                            is TelegramSender.Result.Success -> context.getString(R.string.settings_test_success)
                            is TelegramSender.Result.Failure -> context.getString(R.string.settings_test_failure, result.message)
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                },
                enabled = !isSendingTelegramTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSendingTelegramTest) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(R.string.settings_telegram_test))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(R.string.settings_transfer_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.settings_telegram_transfer_explanation), style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    telegramExportCode = TelegramSettingsCodec.encode(currentTelegramSettings())
                    clipboardManager.setText(AnnotatedString(telegramExportCode))
                    writeExportFile(context, TELEGRAM_EXPORT_FILE_NAME, telegramExportCode)
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_export_copied)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_export_button))
            }
            if (telegramExportCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = telegramExportCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_export_label)) },
                    supportingText = { Text(stringResource(R.string.settings_export_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_export_file_hint, TELEGRAM_EXPORT_FILE_NAME),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = telegramImportInput,
                onValueChange = { telegramImportInput = it },
                label = { Text(stringResource(R.string.settings_import_label)) },
                placeholder = { Text(stringResource(R.string.settings_telegram_import_placeholder)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val decoded = TelegramSettingsCodec.decode(telegramImportInput)
                    if (decoded == null) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_import_invalid)) }
                    } else {
                        telegramBotToken = decoded.botToken
                        telegramChatId = decoded.chatId
                        telegramImportInput = ""
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_import_success)) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_import_button))
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
