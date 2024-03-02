package `in`.delog.ui.scene.identitifiers

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.delog.R
import `in`.delog.service.ssb.Dict
import `in`.delog.service.ssb.WordList
import `in`.delog.service.ssb.mnemonicToSignature
import org.apache.tuweni.scuttlebutt.Identity
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MnemonicForm(callBack: (Identity?) -> Unit) {
    val context = LocalContext.current
    var filteringOptions: List<String>
    val options = WordList(Locale.ENGLISH).words
    val phrase = remember { mutableStateListOf<String>() }

    Column(Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Please select your 24 words mnemonic")
        Spacer(modifier = Modifier.height(16.dp))
        Row {

            Box {
                var expanded by remember { mutableStateOf(false) }
                var selectedOptionText by remember { mutableStateOf("") }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    }
                ) {
                    TextField(
                        enabled = phrase.size < 24,
                        value = selectedOptionText,
                        onValueChange = {
                            selectedOptionText = it
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("input_key"),
                        singleLine = true,
                        label = { Text(stringResource(R.string.insert_key)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    if (selectedOptionText.length > 1) {
                        filteringOptions =
                            options.filter { it.contains(selectedOptionText, ignoreCase = true) }
                        if (selectedOptionText in filteringOptions) {
                            phrase.add(selectedOptionText)
                            selectedOptionText = ""
                            expanded = false
                        }
                        expanded = true
                    } else {
                        filteringOptions = listOf()
                    }
                    if (filteringOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            }
                        ) {
                            filteringOptions.forEach { option ->
                                DropdownMenuItem(
                                    modifier = Modifier.testTag("validate"),
                                    text = { Text(option) },
                                    onClick = {
                                        phrase.add(option)
                                        selectedOptionText = ""
                                        expanded = false
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = null,
                                            //modifier = Modifier.testTag("validate"),
                                        )
                                    })
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(weight = 1f, fill = false)
        ) {
            for (i in 0 until phrase.size) {
                Button(
                    modifier = Modifier.padding(all = 2.dp),
                    onClick = {
                        phrase.remove(phrase[i])
                    },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                )
                {
                    Text(
                        phrase[i]
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "delete",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            val toastText = stringResource(R.string.smthg_went_wrong)
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_mnemonic"),
                onClick = {
                    val dict = Dict(options.toTypedArray())
                    val signature = mnemonicToSignature(phrase, dict)
                    if (signature != null) {
                        callBack(Identity.fromKeyPair(signature))
                    } else {
                        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = phrase.size == 24,
                content = { Text("Restore from mnemonic") }
            )
        }
    }
}
