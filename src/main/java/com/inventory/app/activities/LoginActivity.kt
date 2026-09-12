package com.inventory.app.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.inventory.app.R
import com.inventory.app.database.DatabaseHelper
import com.inventory.app.models.User
import com.inventory.app.utils.SessionManager

/**
 * LoginActivity.kt - Handles user authentication and account creation.
 *
 * This activity provides:
 * - User login with database credential verification
 * - New account creation for first-time users
 * - Session management for persistent login state
 * - Input validation with user feedback
 * - Smooth animations for visual appeal
 *
 * @author Daniel Richmond
 * @version 2.0
 */
class LoginActivity : AppCompatActivity() {

    // ==================== UI Components ====================
    private lateinit var ivAppLogo: ImageView
    private lateinit var tvAppTitle: TextView
    private lateinit var tvAppSubtitle: TextView
    private lateinit var cardLogin: CardView
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnCreateAccount: MaterialButton

    // ==================== DB ====================
    /** Database helper instance for user operations */
    private lateinit var databaseHelper: DatabaseHelper

    // ==================== Lifecycle Methods ====================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check if user is already logged in
        if (SessionManager.isLoggedIn(this)) {
            // skip login screen and go directly to main activity
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        // init db helper
        databaseHelper = DatabaseHelper.getInstance(this)

        // init UI components
        initializeViews()

        // set click listeners
        setupClickListeners()

        // entrance animations
        applyEntranceAnimations()
    }

    // ==================== init Methods ====================
    /**
     * Initialize all view references from the layout.
     * Called once during onCreate.
     */
    private fun initializeViews() {
        ivAppLogo = findViewById(R.id.ivAppLogo)
        tvAppTitle = findViewById(R.id.tvAppTitle)
        tvAppSubtitle = findViewById(R.id.tvAppSubtitle)
        cardLogin = findViewById(R.id.cardLogin)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnLogin = findViewById(R.id.btnLogin)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
    }

    /**
     * Setup click listeners for all interactive elements.
     * Handles login, account creation, and error clearing.
     */
    private fun setupClickListeners() {
        // login btn
        btnLogin.setOnClickListener { attemptLogin() }

        // create account btn
        btnCreateAccount.setOnClickListener { attemptCreateAccount() }

        // clear errors when typing in username field
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideError()
                tilUsername.error = null
            }
        }

        // clear errors when typing in password field
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideError()
                tilPassword.error = null
            }
        }
    }

    /**
     * Apply smooth entrance animations to UI elements.
     * Creates a cascading fade-in effect for visual polish.
     */
    private fun applyEntranceAnimations() {
        // fade logo with delay
        ivAppLogo.alpha = 0f
        ivAppLogo.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(100)
            .start()

        // fade title
        tvAppTitle.alpha = 0f
        tvAppTitle.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(200)
            .start()

        // fade subtitle
        tvAppSubtitle.alpha = 0f
        tvAppSubtitle.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()

        // slide up and fade in the login card
        cardLogin.alpha = 0f
        cardLogin.translationY = 50f
        cardLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(400)
            .start()
    }

    // ==================== Authentication Methods ====================
    /**
     * Attempts to log in with the provided credentials.
     *
     * Process:
     * 1. Validates input fields are not empty
     * 2. Queries database to verify credentials
     * 3. Creates session on success
     * 4. Shows error message on failure
     */
    private fun attemptLogin() {
        // input values and trim whitespace
        val username = getInputText(etUsername)
        val password = getInputText(etPassword)

        // check that all fields are filled
        if (!validateInputs(username, password)) {
            return
        }

        // clear any previous errors
        hideError()

        // attempt to auth user against db
        val user: User? = databaseHelper.authenticateUser(username, password)

        if (user != null) {
            // successful
            onLoginSuccess(user)
        } else {
            // failed
            showError(getString(R.string.login_failed))
        }
    }

    /**
     * Attempts to create a new user account.
     *
     * Process:
     * 1. Validates input fields
     * 2. Checks password meets minimum length
     * 3. Attempts to create user in database
     * 4. Fails if username already exists
     * 5. Creates session on success
     */
    private fun attemptCreateAccount() {
        // input values and trim whitespace
        val username = getInputText(etUsername)
        val password = getInputText(etPassword)

        // check basic requirements
        if (!validateInputs(username, password)) {
            return
        }

        // check minimum password length for security
        if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            showError(getString(R.string.empty_fields))
            return
        }

        hideError()

        // create new user in db
        val user: User? = databaseHelper.createUser(username, password)

        if (user != null) {
            // created successfully
            Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show()
            onLoginSuccess(user)
        } else {
            // username already exists
            showError(getString(R.string.account_exists))
            tilUsername.error = "Username already taken"
        }
    }

    /**
     * Called when login or account creation succeeds.
     * Creates session and navigates to main activity.
     *
     * @param user The authenticated user object
     */
    private fun onLoginSuccess(user: User) {
        // make persistent session
        SessionManager.createSession(this, user.id, user.username)

        // success feedback
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()

        // nav to main inventory screen
        navigateToMain()
    }

    // ==================== Validation Methods ====================
    /**
     * Validates that username and password fields are filled.
     * Sets appropriate error messages on invalid fields.
     *
     * @param username The username input
     * @param password The password input
     * @return true if both fields are valid, false otherwise
     */
    private fun validateInputs(username: String?, password: String?): Boolean {
        var isValid = true

        // check username is not empty
        if (TextUtils.isEmpty(username)) {
            tilUsername.error = "Username is required"
            isValid = false
        } else {
            tilUsername.error = null
        }

        // check password is not empty
        if (TextUtils.isEmpty(password)) {
            tilPassword.error = "Password is required"
            isValid = false
        } else {
            tilPassword.error = null
        }

        // show general error if validation failed
        if (!isValid) {
            showError(getString(R.string.empty_fields))
        }

        return isValid
    }

    // ==================== UI Helper Methods ====================
    /**
     * Gets trimmed text from an EditText field.
     * Handles null safety.
     * @param editText The input field
     * @return Trimmed string or empty string if null
     */
    private fun getInputText(editText: TextInputEditText): String {
        return editText.text?.toString()?.trim() ?: ""
    }

    /**
     * Shows an error message with fade animation.
     * @param message The error message to display
     */
    private fun showError(message: String?) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE

        // fade-in animation for attention
        tvErrorMessage.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )
    }

    /**
     * Hides the error message.
     */
    private fun hideError() {
        tvErrorMessage.visibility = View.GONE
    }

    /**
     * Navigates to the main inventory screen.
     * Finishes this activity so user can't navigate back to login.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // apply smooth transition animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        // finish login activity to prevent back navigation
        finish()
    }
}