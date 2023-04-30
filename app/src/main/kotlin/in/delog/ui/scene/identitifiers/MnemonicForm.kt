package `in`.delog.ui.scene.identitifiers

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.delog.R
import `in`.delog.ssb.BaseSsbService.Companion.TAG
import `in`.delog.ssb.Dict
import `in`.delog.ssb.WordList
import `in`.delog.ssb.mnemonicToSignature
import org.apache.tuweni.scuttlebutt.Identity
import org.bouncycastle.math.raw.Mod
import java.util.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MnemonicForm(callBack: (Identity?) -> Unit) {
    val context = LocalContext.current
    var filteringOptions: List<String>; // emptyList()
    val options = WordList(Locale.ENGLISH).words
    val phrase = remember { mutableStateListOf<String>() }

    Column(Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Please select your 24 words mnemonic")
        Spacer(modifier = Modifier.height(16.dp))
        Row {

            Box() {
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
                .weight(weight =1f, fill = false)
        ) {
            for (i in 0 until phrase.size) {
                Button(
                    modifier = Modifier.padding(all= 2.dp),
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
            val toastText = stringResource(R.string.smthg_went_wrong);
            Button(
                modifier = Modifier.fillMaxWidth().testTag("submit_mnemonic"),
                onClick = {
                    val dict = Dict(options.toTypedArray())
                    val signature = mnemonicToSignature(phrase, dict)
                    if (signature != null) {
                        callBack(Identity.fromKeyPair(signature))
                    } else {
                        Log.e(TAG, "error creating keypair")
                        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = phrase.size == 24,
                content = { Text("Restore from mnemonic") }
            )
        }
    }
}
