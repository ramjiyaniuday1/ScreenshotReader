package com.alle.imagereader.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alle.imagereader.data.db.models.Collection
import com.alle.imagereader.data.db.models.Screenshot

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CollectionSheet(screenshot: State<Screenshot?>, collections: List<Collection>, action: (MainActivity.Action) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        contentColor = Color.Black,
        containerColor = Color.White,
        onDismissRequest = {
            action(MainActivity.Action.HideCollectionSheet)
        },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(min = 25.dp)
                    .border(width = 2.dp, color = Color.Gray, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                screenshot.value?.collections?.forEach { collection ->
                    Row(modifier = Modifier
                        .clickable {
                            action(
                                MainActivity.Action.TagCollection(
                                    collection,
                                    false
                                )
                            )
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFFCC))
                        .padding(10.dp)) {
                        Text(text = collection, color = Color.Black)

                        Icon(imageVector = Icons.Default.Close, contentDescription = "close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Select an option or create one", color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            ) {
                collections.forEach { collection ->
                    if (screenshot.value?.collections?.contains(collection.name) == false) {
                        Row(modifier = Modifier
                            .clickable {
                                action(
                                    MainActivity.Action.TagCollection(
                                        collection.name,
                                        true
                                    )
                                )
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = Color(0xFFFFFFCC))
                            .padding(10.dp)) {
                            Text(text = collection.name, color = Color.Black)

                            Icon(imageVector = Icons.Default.Add, contentDescription = "add")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}
