package com.example.util

/**
 * Normalizes network, database, and authentication errors into clean, human-readable messages
 * without ever exposing raw HTTP status codes, PostgREST error codes, stack traces, or URLs.
 */
object ErrorSanitizer {

    private val httpStatusRegex = Regex("""\b(?:anilist\s+)?http(?:\s+error|\s+status|\s+code)?\s*[:#-]?\s*\d{3}\b""", RegexOption.IGNORE_CASE)
    private val statusParenRegex = Regex("""\(\s*(?:http\s*)?\d{3}\s*\)""", RegexOption.IGNORE_CASE)
    private val exceptionPrefixRegex = Regex("""^(?:[a-zA-Z0-9_.]+\.)?[A-Za-z0-9_]+(?:Exception|Error)\s*:\s*""")
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val pgrstRegex = Regex("""\bPGRST\d+\b""", RegexOption.IGNORE_CASE)
    private val localhostRegex = Regex("""localhost:\d+\S*""", RegexOption.IGNORE_CASE)

    fun sanitize(throwable: Throwable?, fallback: String = "Something went wrong. Please try again."): String {
        if (throwable == null) return fallback
        val msg = throwable.localizedMessage ?: throwable.message
        if (msg.isNullOrBlank()) {
            val cls = throwable::class.java.simpleName
            return when {
                cls.contains("UnknownHost", true) ||
                    cls.contains("Timeout", true) ||
                    cls.contains("Connect", true) -> "Unable to connect right now. Check your internet connection and try again."
                else -> fallback
            }
        }
        return sanitize(msg, fallback)
    }

    fun sanitize(raw: String?, fallback: String = "Something went wrong. Please try again."): String {
        val input = raw?.trim().orEmpty()
        if (input.isBlank()) return fallback

        return when {
            input.contains("Invalid login credentials", ignoreCase = true) ->
                "Incorrect email/username or password."
            input.contains("Email not confirmed", ignoreCase = true) ->
                "Please confirm your email using the verification link sent to your inbox, then continue."
            input.contains("User already registered", ignoreCase = true) ||
                input.contains("already been registered", ignoreCase = true) ||
                input.contains("already exists", ignoreCase = true) ->
                "An account with this email already exists. Please sign in instead."
            input.contains("profiles_username_key", ignoreCase = true) ||
                input.contains("idx_profiles_username_lower_unique", ignoreCase = true) ||
                input.contains("duplicate key", ignoreCase = true) ||
                input.contains("already taken", ignoreCase = true) ||
                input.contains("23505", ignoreCase = true) ->
                "That username is already taken. Please choose a different unique username."
            input.contains("username must be", ignoreCase = true) ||
                input.contains("2 and 32 characters", ignoreCase = true) ||
                input.contains("2–32 characters", ignoreCase = true) ->
                "Username must be between 2 and 32 characters."
            input.contains("No account found matching", ignoreCase = true) ||
                input.contains("account not found", ignoreCase = true) ->
                "No account found with that username. Try your email address or create a new account."
            input.contains("rate limit", ignoreCase = true) ||
                input.contains("too many requests", ignoreCase = true) ||
                input.contains("over_email_send_rate_limit", ignoreCase = true) ||
                input.contains("security purposes", ignoreCase = true) ->
                "Too many requests right now. Please wait a moment and try again."
            input.contains("weak password", ignoreCase = true) ||
                input.contains("Password should be at least", ignoreCase = true) ->
                "Password must be at least 6 characters long."
            input.contains("Unable to validate email", ignoreCase = true) ||
                input.contains("invalid email", ignoreCase = true) ->
                "Please enter a valid email address."
            input.contains("Session expired", ignoreCase = true) ||
                input.contains("JWT expired", ignoreCase = true) ||
                input.contains("invalid refresh token", ignoreCase = true) ||
                input.contains("invalid_grant", ignoreCase = true) ->
                "Your session expired. Please sign in again."
            input.contains("PGRST202", ignoreCase = true) ||
                input.contains("Could not find the function", ignoreCase = true) ->
                "Username login isn't enabled on the server yet. Sign in with your email for now."
            input.contains("Unable to resolve host", ignoreCase = true) ||
                input.contains("UnknownHostException", ignoreCase = true) ||
                input.contains("SocketTimeoutException", ignoreCase = true) ||
                input.contains("ConnectException", ignoreCase = true) ||
                input.contains("Failed to connect to", ignoreCase = true) ||
                input.contains("timed out", ignoreCase = true) ||
                input.contains("network is unreachable", ignoreCase = true) ->
                "Unable to connect right now. Check your internet connection and try again."
            input.contains("Supabase not configured", ignoreCase = true) ||
                input.contains("Supabase is not configured", ignoreCase = true) ->
                "Cloud sync is currently unavailable. You can continue using your local library."
            else -> cleanResidualTechnicalText(input, fallback)
        }
    }

    private fun cleanResidualTechnicalText(input: String, fallback: String): String {
        if (input.trimStart().startsWith("{") || input.trimStart().startsWith("[")) {
            return fallback
        }
        var cleaned = input
            .replace(exceptionPrefixRegex, "")
            .replace(httpStatusRegex, "")
            .replace(statusParenRegex, "")
            .replace(pgrstRegex, "")
            .replace(localhostRegex, "the app")
            .replace(urlRegex, "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .trimEnd(':', '-', '.', ',')
            .trim()

        // If cleaning removed everything or only left generic technical phrasing, return fallback
        val looksTechnical = cleaned.isBlank() ||
            cleaned.length < 4 ||
            cleaned.contains("sqlstate", ignoreCase = true) ||
            cleaned.contains("status code", ignoreCase = true) ||
            cleaned.contains("unexpected response", ignoreCase = true) ||
            cleaned.matches(Regex("""^\d{3}$""")) ||
            cleaned.contains("error_description", ignoreCase = true)

        return if (looksTechnical) fallback else cleaned
    }

    fun maskEmail(raw: String?): String {
        val clean = raw?.trim().orEmpty()
        val at = clean.indexOf('@')
        if (at <= 0 || at >= clean.length - 1) return ""
        val local = clean.substring(0, at)
        val domain = clean.substring(at + 1)
        val visible = if (local.length <= 2) 1 else 2
        val prefix = local.take(visible)
        val maskedLen = (local.length - visible).coerceAtLeast(4)
        return "$prefix${"*".repeat(maskedLen)}@$domain"
    }
}
