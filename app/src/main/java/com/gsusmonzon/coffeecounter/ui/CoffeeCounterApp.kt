package com.gsusmonzon.coffeecounter.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.ui.home.HomeRoute
import com.gsusmonzon.coffeecounter.ui.settings.SettingsRoute
import com.gsusmonzon.coffeecounter.ui.theme.CoffeeCounterTheme

enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val testTag: String,
) {
    HOME(
        labelRes = R.string.home_label,
        iconRes = R.drawable.ic_coffee,
        testTag = UiTestTags.NAV_HOME,
    ),
    SETTINGS(
        labelRes = R.string.settings_label,
        iconRes = R.drawable.ic_more,
        testTag = UiTestTags.NAV_SETTINGS,
    ),
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CoffeeCounterApp() {
    var currentDestination by rememberSaveable { androidx.compose.runtime.mutableStateOf(TopLevelDestination.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = { Text(text = stringResource(currentDestination.labelRes)) },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(destination.testTag),
                        selected = destination == currentDestination,
                        onClick = { currentDestination = destination },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            modifier = Modifier.padding(innerPadding),
            contentAlignment = Alignment.TopStart,
            transitionSpec = {
                destinationTransition(
                    initialDestination = initialState,
                    targetDestination = targetState,
                )
            },
            label = "top_level_destination",
        ) { destination ->
            when (destination) {
                TopLevelDestination.HOME -> HomeRoute(modifier = Modifier)
                TopLevelDestination.SETTINGS -> SettingsRoute(modifier = Modifier)
            }
        }
    }
}

private fun destinationTransition(
    initialDestination: TopLevelDestination,
    targetDestination: TopLevelDestination,
): ContentTransform {
    val fadeSpec = tween<Float>(durationMillis = 180)
    val slideSpec = tween<IntOffset>(durationMillis = 180)
    val isForward = targetDestination.ordinal > initialDestination.ordinal
    val enterOffset: (Int) -> Int = { width -> if (isForward) width / 8 else -(width / 8) }
    val exitOffset: (Int) -> Int = { width -> if (isForward) -(width / 8) else width / 8 }

    return (
        fadeIn(animationSpec = fadeSpec) +
            slideInHorizontally(
                animationSpec = slideSpec,
                initialOffsetX = enterOffset,
            )
        ).togetherWith(
        fadeOut(animationSpec = fadeSpec) +
            slideOutHorizontally(
                animationSpec = slideSpec,
                targetOffsetX = exitOffset,
            )
    )
}

@Preview
@Composable
private fun CoffeeCounterAppPreview() {
    CoffeeCounterTheme {
        CoffeeCounterApp()
    }
}
