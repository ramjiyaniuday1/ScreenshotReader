package com.alle.imagereader.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alle.imagereader.data.db.models.Screenshot

@Composable
fun ActionMenuView(
    selectedImage: State<Screenshot?>,
    action: (MainActivity.Action) -> Unit,
    infoOpened: MutableState<Boolean>
) {
    Row(modifier = with(Modifier) {
        fillMaxWidth()
            .background(Color.White)
    },
        horizontalArrangement = Arrangement.SpaceAround) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {
                    selectedImage.value?.let {
                        action(MainActivity.Action.Share(it))
                    }
                }
                .padding(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = "", tint = Color.Gray)
            Text(text = "Share", color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {
                    selectedImage.value?.let { action(MainActivity.Action.Info(it)) }
                }
                .padding(10.dp))
        {
            Icon(imageVector = Icons.Default.Info, contentDescription = "", tint = if (infoOpened.value) Color.Black else Color.Gray)
            Text(text = "Info", color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {
                    selectedImage.value?.let { action(MainActivity.Action.Delete(it)) }
                }
                .padding(10.dp))
        {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "", tint = Color.Gray)
            Text(text = "Delete", color = Color.Gray)
        }
    }
}