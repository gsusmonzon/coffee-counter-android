package com.gsusmonzon.coffeecounter.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.ui.home.HomeRoute
import com.gsusmonzon.coffeecounter.ui.settings.SettingsRoute
import com.gsusmonzon.coffeecounter.ui.theme.CoffeeCounterTheme

enum class TopLevelDestination(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(
        labelRes = R.string.home_label,
        iconRes = R.drawable.ic_home,
    ),
    SETTINGS(
        labelRes = R.string.settings_label,
        iconRes = R.drawable.ic_account_box,
    ),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CoffeeCounterApp() {
    var currentDestination by rememberSaveable { androidx.compose.runtime.mutableStateOf(TopLevelDestination.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(currentDestination.labelRes)) }
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentDestination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(text = stringResource(destination.labelRes)) },
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            TopLevelDestination.HOME -> HomeRoute(modifier = Modifier.padding(innerPadding))
            TopLevelDestination.SETTINGS -> SettingsRoute(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Preview
@Composable
private fun CoffeeCounterAppPreview() {
    CoffeeCounterTheme {
        CoffeeCounterApp()
    }
}
