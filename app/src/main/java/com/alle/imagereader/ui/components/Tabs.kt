package com.alle.imagereader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Tabs(val route : String) {
    object Share : Tabs("share_route")
    object Info : Tabs("info_route")
    object Delete : Tabs("delete_route")
}

data class BottomNavigationItem(
    val label : String = "",
    val icon : ImageVector = Icons.Filled.Share,
    val route : String = ""
) {

    //function to get the list of bottomNavigationItems
    fun bottomNavigationItems() : List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                label = "Share",
                icon = Icons.Filled.Share,
                route = Tabs.Share.route
            ),
            BottomNavigationItem(
                label = "Info",
                icon = Icons.Filled.Info,
                route = Tabs.Info.route
            ),
            BottomNavigationItem(
                label = "Delete",
                icon = Icons.Filled.Delete,
                route = Tabs.Delete.route
            ),
        )
    }
}