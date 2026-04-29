package com.example.simplekeystoredemo

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.simplekeystoredemo.ui.theme.SimpleKeyStoreDemoTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SimpleKeyStoreDemoTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                var requireUnlockedDeviceChecked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is MainUiEvent.KeySuccessfullyGenerated -> {
                                snackbarHostState.showSnackbar(
                                    message = "Key Successfully Generated",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            is MainUiEvent.KeySuccessfullyAccessed -> {
                                snackbarHostState.showSnackbar(
                                    message = "Key Successfully Accessed",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            is MainUiEvent.KeyDeleted -> {
                                snackbarHostState.showSnackbar(
                                    message = "Key Deleted",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            is MainUiEvent.Error -> {
                                snackbarHostState.showSnackbar(
                                    message = event.message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }

                SimpleKeyStoreDemoScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    requireUnlockedDeviceChecked = requireUnlockedDeviceChecked,
                    onRequireUnlockedDeviceChecked = {
                        requireUnlockedDeviceChecked = it
                    },
                    onGenerateNewKeyClick = {
                        viewModel.generateNewKey(requireUnlockedDeviceChecked)
                    },
                    onTryAccessingKeyClick = {
                        viewModel.tryUsingKey()
                    },
                    onDeleteKeyClick = {
                        viewModel.deleteKey()
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SimpleKeyStoreDemoScreen(
        uiState: MainUiState,
        snackbarHostState: SnackbarHostState,
        requireUnlockedDeviceChecked: Boolean,
        onRequireUnlockedDeviceChecked: ((Boolean) -> Unit)?,
        onGenerateNewKeyClick: () -> Unit,
        onTryAccessingKeyClick: () -> Unit,
        onDeleteKeyClick: () -> Unit,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Simple KeyStore Demo") }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column (
                modifier = Modifier.padding(innerPadding),
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
                    ) {
                        Switch(
                            enabled = uiState is MainUiState.InitialState,
                            checked = requireUnlockedDeviceChecked,
                            onCheckedChange = onRequireUnlockedDeviceChecked,
                        )
                        Text(
                            text = "Require Unlocked Device",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                }
                Button(
                    enabled = uiState is MainUiState.InitialState,
                    onClick = onGenerateNewKeyClick,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                ) {
                    Text("Generate New Key")
                }
                Button(
                    enabled = uiState is MainUiState.KeyGeneratedState,
                    onClick = onTryAccessingKeyClick,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                ) {
                    Text("Try Accessing Key")
                }
                Button(
                    enabled = uiState is MainUiState.KeyGeneratedState,
                    onClick = onDeleteKeyClick,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                ) {
                    Text("Delete Key")
                }
            }
        }
    }
}
