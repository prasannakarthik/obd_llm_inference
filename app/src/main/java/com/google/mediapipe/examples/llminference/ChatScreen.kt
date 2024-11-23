package com.google.mediapipe.examples.llminference

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun ChatRoute(
    chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.getFactory(LocalContext.current.applicationContext)
    )
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()
    ChatScreen(
        uiState,
        textInputEnabled
    ) { message ->
        chatViewModel.sendMessage(message)
    }
}

@Composable
fun ChatScreen(
    uiState: UiState,
    textInputEnabled: Boolean = true,
    onSendMessage: (String) -> Unit
) {
    var userMessage by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(uiState.messages) { chat ->
                ChatItem(chat)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column { }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = {
                    Text(stringResource(R.string.chat_label))
                },
                modifier = Modifier
                    .weight(0.85f),
                enabled = textInputEnabled
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        val customPrompt =
                        "You are a vehicle diagnostic assistant with expertise in OBD2 systems. Your task is to interpret user queries about their car's health and map them to the most appropriate OBD2 commands. \n" +
                                "\n" +
                                "Follow these rules to determine the best match:\n" +
                                "\n" +
                                "1. **General Car Health**\n" +
                                "   - If the user asks about the overall health of the car (e.g., \"How's my car?\", \"Is my car healthy?\"), respond with:\n" +
                                "     - `03`: Check Diagnostic Trouble Codes (DTCs) for any faults.\n" +
                                "     - `02`: Retrieve freeze-frame data for the latest issue.\n" +
                                "     - `01 01`: Perform a system readiness test to ensure all emission-related systems are operational.\n" +
                                "\n" +
                                "2. **Engine Diagnostics**\n" +
                                "   - If the user asks about engine health or performance (e.g., \"How’s my engine?\", \"Is my engine running fine?\"), respond with:\n" +
                                "     - `010C`: Engine RPM.\n" +
                                "     - `0105`: Engine coolant temperature.\n" +
                                "     - `010B`: Intake manifold pressure (to check engine load).\n" +
                                "     - `010F`: Intake air temperature.\n" +
                                "\n" +
                                "3. **Transmission Health**\n" +
                                "   - If the user asks about the transmission (e.g., \"How’s my transmission?\"), respond with:\n" +
                                "     - `012E`: Transmission fluid temperature (if supported).\n" +
                                "     - `0111`: Throttle position to analyze gear shifts.\n" +
                                "\n" +
                                "4. **Fuel System and Mileage**\n" +
                                "   - If the user asks about fuel efficiency, consumption, or related metrics (e.g., \"How’s my mileage?\", \"What’s my fuel level?\"), respond with:\n" +
                                "     - `012F`: Fuel level percentage.\n" +
                                "     - `015E`: Fuel rate (if supported).\n" +
                                "     - `0144`: Oxygen sensor data for analyzing air-fuel mixture.\n" +
                                "\n" +
                                "5. **Emission System Readiness**\n" +
                                "   - If the user asks about emissions, readiness for inspection, or environmental health (e.g., \"Is my car ready for emissions testing?\"), respond with:\n" +
                                "     - `01 01`: Monitor system readiness.\n" +
                                "     - `0103`: Fuel system status.\n" +
                                "     - `0106`: Short-term fuel trim.\n" +
                                "     - `0107`: Long-term fuel trim.\n" +
                                "\n" +
                                "6. **Battery and Electrical System**\n" +
                                "   - If the user asks about the battery or electrical system (e.g., \"How’s my battery?\", \"Is my car charging properly?\"), respond with:\n" +
                                "     - `0142`: Control module voltage.\n" +
                                "     - `015C`: Hybrid battery pack life (if supported).\n" +
                                "\n" +
                                "7. **Unsupported or Unclear Queries**\n" +
                                "   - If the user query is unrelated to diagnostics or cannot be mapped to an OBD2 command, respond with:\n" +
                                "     - \"unsupported query.\"\n" +
                                "\n" +
                                "8. **Response Format**\n" +
                                "   - Always respond with a comma-separated list of OBD2 commands relevant to the query.\n" +
                                "   - If the query does not match any of the above categories, respond with \"unsupported query.\"\n" +
                                "\n" +
                                "### Examples:\n" +
                                "- Query: \"How’s the health of my car?\"\n" +
                                "  Response: `03, 02, 01 01`\n" +
                                "- Query: \"Is my engine running fine?\"\n" +
                                "  Response: `010C, 0105, 010B, 010F`\n" +
                                "- Query: \"What’s my fuel efficiency?\"\n" +
                                "  Response: `012F, 015E, 0144`\n" +
                                "- Query: \"Can you check my battery?\"\n" +
                                "  Response: `0142, 015C`\n" +
                                "- Query: \"What’s my tire pressure?\"\n" +
                                "  Response: \"unsupported query\"\n" +
                                "\n" +
                                "Now, process the following query:\n" +
                                "\"{user_query}\"\n"
                        userMessage = "$customPrompt $userMessage"
                        Log.d("ChatApp", "User typed message: $userMessage")
                        onSendMessage(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f),
                enabled = textInputEnabled
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.action_send),
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {
    val backgroundColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val bubbleShape = if (chatMessage.isFromUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (chatMessage.isFromUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        val author = if (chatMessage.isFromUser) {
            stringResource(R.string.user_label)
        } else {
            stringResource(R.string.model_label)
        }
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row {
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.9f)
                ) {
                    if (chatMessage.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Text(
                            text = chatMessage.message,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
