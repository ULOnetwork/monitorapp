package eu.ulonetwork.monitorapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.PreferencesManager

@Composable
fun ConsentScreen(onAccept: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.consent_title),
            style = MaterialTheme.typography.headlineSmall
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
        Text(
            text = stringResource(R.string.consent_body),
            style = MaterialTheme.typography.bodyMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
        Button(
            onClick = {
                PreferencesManager(context).setConsentAccepted()
                onAccept()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.consent_agree))
        }
    }
}
