package ir.ehsannarmani.encryption

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope

class AppState(
    val navController: NavHostController,
    val context:Context,
    val scope:CoroutineScope,
    val sharedPreferences: SharedPreferences
) {
}

@Composable
fun rememberAppState():AppState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    return remember {
        AppState(
            context = context,
            scope = scope,
            navController = navController,
            sharedPreferences = context.getSharedPreferences("main",Context.MODE_PRIVATE)
        )
    }
}
val LocalAppState = staticCompositionLocalOf<AppState> { error("No State Provided Yet!") }