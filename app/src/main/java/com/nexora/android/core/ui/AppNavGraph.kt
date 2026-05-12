package com.nexora.android.core.ui

import androidx.compose.runtime.Composable
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexora.android.ui.auth.AuthMessageScreen
import com.nexora.android.ui.auth.AuthNavigationTarget
import com.nexora.android.ui.auth.LoginScreen
import com.nexora.android.ui.auth.SignupScreen
import com.nexora.android.ui.auth.VerifyEmailOtpScreen
import com.nexora.android.ui.context.ContextPickerScreen
import com.nexora.android.ui.owner.CreateOwnerTenantScreen
import com.nexora.android.ui.splash.SplashScreen
import com.nexora.android.ui.workspace.AddContactScreen
import com.nexora.android.ui.workspace.ArchivedContactsScreen
import com.nexora.android.ui.workspace.ContactDetailScreen
import com.nexora.android.ui.workspace.CrmWorkspaceTab
import com.nexora.android.ui.workspace.EditContactScreen
import com.nexora.android.ui.workspace.TenantWorkspaceScreen
import com.nexora.android.ui.welcome.WelcomeScreen

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val Login = "login"
    const val Signup = "signup"
    const val VerifyEmailOtp = "verify_email_otp"
    const val ContextPicker = "context_picker"
    const val AuthMessage = "auth_message"
    const val OwnerOnboarding = "owner_onboarding"
    const val Workspace = "workspace"
    const val AddContact = "add_contact"
    const val ArchivedContacts = "archived_contacts"
    const val ContactDetail = "contact_detail"
    const val EditContact = "edit_contact"

    fun verifyEmailOtp(email: String): String = "$VerifyEmailOtp/${Uri.encode(email)}"
    fun workspace(
        contextId: String,
        role: String,
        tenantId: String,
        tenantName: String,
        initialTab: String = CrmWorkspaceTab.Dashboard.name
    ): String =
        "$Workspace/${Uri.encode(contextId)}/${Uri.encode(role)}/${Uri.encode(tenantId)}/${Uri.encode(tenantName)}?initialTab=${Uri.encode(initialTab)}"

    fun addContact(tenantId: String, tenantName: String): String =
        "$AddContact/${Uri.encode(tenantId)}/${Uri.encode(tenantName)}"

    fun archivedContacts(tenantId: String, tenantName: String): String =
        "$ArchivedContacts/${Uri.encode(tenantId)}/${Uri.encode(tenantName)}"

    fun contactDetail(tenantId: String, tenantName: String, contactId: String): String =
        "$ContactDetail/${Uri.encode(tenantId)}/${Uri.encode(tenantName)}/${Uri.encode(contactId)}"

    fun editContact(tenantId: String, tenantName: String, contactId: String): String =
        "$EditContact/${Uri.encode(tenantId)}/${Uri.encode(tenantName)}/${Uri.encode(contactId)}"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val activity = LocalContext.current as? Activity

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                onFinished = { destination ->
                    navController.navigateAndClearRoot(destination.toRoute())
                }
            )
        }
        composable(Routes.Welcome) {
            BackHandler {
                activity?.moveTaskToBack(true)
            }
            WelcomeScreen(
                onLogin = { navController.navigateWithinAuth(Routes.Login) },
                onSignup = { navController.navigateWithinAuth(Routes.Signup) },
                onContinueVerification = { email ->
                    navController.navigateWithinAuth(Routes.verifyEmailOtp(email))
                }
            )
        }
        composable(Routes.Login) {
            LoginScreen(
                onBack = { navController.navigateUp() },
                onSignup = { navController.navigateWithinAuth(Routes.Signup) },
                onVerifyEmail = { email -> navController.navigateWithinAuth(Routes.verifyEmailOtp(email)) },
                onAuthenticated = { navController.navigateAndClearRoot(Routes.ContextPicker) }
            )
        }
        composable(Routes.Signup) {
            SignupScreen(
                onBack = { navController.navigateUp() },
                onLogin = { navController.navigateWithinAuth(Routes.Login) },
                onVerifyEmail = { email -> navController.navigateWithinAuth(Routes.verifyEmailOtp(email)) }
            )
        }
        composable(
            route = "${Routes.VerifyEmailOtp}/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) {
            VerifyEmailOtpScreen(
                onBack = { navController.navigateWithinAuth(Routes.Login) },
                onVerified = { navController.navigateWithinAuth(Routes.Login) }
            )
        }
        composable(Routes.AuthMessage) {
            AuthMessageScreen(
                onLogin = { navController.navigateWithinAuth(Routes.Login) }
            )
        }
        composable(Routes.ContextPicker) {
            BackHandler {
                activity?.moveTaskToBack(true)
            }
            ContextPickerScreen(
                onCreateCompany = { navController.navigate(Routes.OwnerOnboarding) },
                onOpenOwnerWorkspace = { context ->
                    navController.navigate(
                        Routes.workspace(
                            contextId = context.contextId,
                            role = context.role.name,
                            tenantId = context.tenantId.orEmpty(),
                            tenantName = context.tenantName ?: "Nexora workspace"
                        )
                    )
                },
                onLoggedOut = { navController.navigateAfterLogout(Routes.Welcome) }
            )
        }
        composable(Routes.OwnerOnboarding) {
            CreateOwnerTenantScreen(
                onBack = { navController.navigateUp() },
                onCreated = { navController.navigateAndClearRoot(Routes.ContextPicker) }
            )
        }
        composable(
            route = "${Routes.Workspace}/{contextId}/{role}/{tenantId}/{tenantName}?initialTab={initialTab}",
            arguments = listOf(
                navArgument("contextId") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
                navArgument("tenantId") { type = NavType.StringType },
                navArgument("tenantName") { type = NavType.StringType },
                navArgument("initialTab") {
                    type = NavType.StringType
                    defaultValue = CrmWorkspaceTab.Dashboard.name
                }
            )
        ) { backStackEntry ->
            val tenantId = backStackEntry.arguments?.getString("tenantId").orEmpty()
            val tenantName = backStackEntry.arguments?.getString("tenantName") ?: "Nexora workspace"
            TenantWorkspaceScreen(
                contextId = backStackEntry.arguments?.getString("contextId").orEmpty(),
                role = backStackEntry.arguments?.getString("role") ?: "Owner",
                tenantId = tenantId,
                tenantName = tenantName,
                initialTab = backStackEntry.arguments?.getString("initialTab") ?: CrmWorkspaceTab.Dashboard.name,
                onAddContact = { tenantId, tenantName ->
                    navController.navigate(Routes.addContact(tenantId, tenantName))
                },
                onOpenArchivedContacts = { tenantId, tenantName ->
                    navController.navigate(Routes.archivedContacts(tenantId, tenantName))
                },
                onOpenContact = { selectedTenantId, selectedTenantName, contactId ->
                    navController.navigate(Routes.contactDetail(selectedTenantId, selectedTenantName, contactId))
                },
                onBack = { navController.navigateUp() }
            )
        }
        composable(
            route = "${Routes.AddContact}/{tenantId}/{tenantName}",
            arguments = listOf(
                navArgument("tenantId") { type = NavType.StringType },
                navArgument("tenantName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            AddContactScreen(
                tenantId = backStackEntry.arguments?.getString("tenantId").orEmpty(),
                tenantName = backStackEntry.arguments?.getString("tenantName") ?: "Nexora workspace",
                onBack = { navController.navigateUp() },
                onCreated = { navController.navigateUp() }
            )
        }
        composable(
            route = "${Routes.ArchivedContacts}/{tenantId}/{tenantName}",
            arguments = listOf(
                navArgument("tenantId") { type = NavType.StringType },
                navArgument("tenantName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ArchivedContactsScreen(
                tenantId = backStackEntry.arguments?.getString("tenantId").orEmpty(),
                tenantName = backStackEntry.arguments?.getString("tenantName") ?: "Nexora workspace",
                onBack = { navController.navigateUp() }
            )
        }
        composable(
            route = "${Routes.ContactDetail}/{tenantId}/{tenantName}/{contactId}",
            arguments = listOf(
                navArgument("tenantId") { type = NavType.StringType },
                navArgument("tenantName") { type = NavType.StringType },
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tenantId = backStackEntry.arguments?.getString("tenantId").orEmpty()
            val tenantName = backStackEntry.arguments?.getString("tenantName") ?: "Nexora workspace"
            val contactId = backStackEntry.arguments?.getString("contactId").orEmpty()
            ContactDetailScreen(
                tenantId = tenantId,
                tenantName = tenantName,
                contactId = contactId,
                onBack = { navController.navigateUp() },
                onEdit = { navController.navigate(Routes.editContact(tenantId, tenantName, contactId)) },
                onArchived = {
                    navController.navigate(
                        Routes.workspace(
                            contextId = "owner:$tenantId",
                            role = "Owner",
                            tenantId = tenantId,
                            tenantName = tenantName,
                            initialTab = CrmWorkspaceTab.Contacts.name
                        )
                    ) {
                        popUpTo(Routes.ContextPicker) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "${Routes.EditContact}/{tenantId}/{tenantName}/{contactId}",
            arguments = listOf(
                navArgument("tenantId") { type = NavType.StringType },
                navArgument("tenantName") { type = NavType.StringType },
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditContactScreen(
                tenantId = backStackEntry.arguments?.getString("tenantId").orEmpty(),
                contactId = backStackEntry.arguments?.getString("contactId").orEmpty(),
                onBack = { navController.navigateUp() },
                onSaved = { navController.navigateUp() }
            )
        }
    }
}

private fun AuthNavigationTarget.toRoute(): String {
    return when (this) {
        AuthNavigationTarget.ContextPicker -> Routes.ContextPicker
        AuthNavigationTarget.CheckEmail -> Routes.AuthMessage
        AuthNavigationTarget.VerifyEmailOtp -> Routes.VerifyEmailOtp
        AuthNavigationTarget.Login -> Routes.Login
        AuthNavigationTarget.Welcome -> Routes.Welcome
    }
}

private fun NavHostController.navigateAndClearRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateWithinAuth(route: String) {
    navigate(route) {
        popUpTo(Routes.Welcome) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateAfterLogout(route: String) {
    navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
