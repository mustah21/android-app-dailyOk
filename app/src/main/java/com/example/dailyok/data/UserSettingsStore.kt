package com.example.dailyok.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dailyok.ui.screens.settings.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.myDataStore by preferencesDataStore(name = "user_prefs")

typealias UserSettingsStore = UserManager

class UserManager(
    private val context: Context
) {
    companion object {
        val USER_DOB_KEY = intPreferencesKey("USER_DOB")
        val USER_NAME_KEY = stringPreferencesKey("USER_NAME")
        val USER_MAIL_KEY = stringPreferencesKey("MAIL_KEY")
        val ONBOARDING_DONE_KEY = booleanPreferencesKey("ONBOARDING_DONE")

        val CONTACT1_NAME_KEY = stringPreferencesKey("CONTACT1_NAME")
        val CONTACT1_PHONE_KEY = stringPreferencesKey("CONTACT1_PHONE")

        // Optional, can be edited or deleted later
        val CONTACT2_NAME_KEY = stringPreferencesKey("CONTACT2_NAME")
        val CONTACT2_PHONE_KEY = stringPreferencesKey("CONTACT2_PHONE")
    }

    object PhoneValidator {
        fun isValid(phone: String?): Boolean {
            if (phone.isNullOrBlank()) return false
            val trimmed = phone.trim()
            val digitCount = trimmed.count { it.isDigit() }
            if (digitCount < 7 || digitCount > 15) return false
            return android.util.Patterns.PHONE.matcher(trimmed).matches()
        }
    }

    // Function to store user data (dob as Int, e.g. birth year or epoch day)
    suspend fun storeUser(dob: Int, name: String, mail: String) {
        context.myDataStore.edit {
            it[USER_DOB_KEY] = dob
            it[USER_NAME_KEY] = name.trim()
            it[USER_MAIL_KEY] = mail.trim()
        }
    }

    // Create or initialize contacts, validating phone numbers first
    suspend fun createContact(
        c1_name: String,
        c1_phone: String,
        c2_name: String? = null,
        c2_phone: String? = null
    ): Boolean {
        if (!PhoneValidator.isValid(c1_phone)) return false
        if (!c2_phone.isNullOrBlank() && !PhoneValidator.isValid(c2_phone)) return false

        context.myDataStore.edit { prefs ->
            prefs[CONTACT1_NAME_KEY] = c1_name.trim()
            prefs[CONTACT1_PHONE_KEY] = c1_phone.trim()
            if (!c2_name.isNullOrBlank() && !c2_phone.isNullOrBlank()) {
                prefs[CONTACT2_NAME_KEY] = c2_name.trim()
                prefs[CONTACT2_PHONE_KEY] = c2_phone.trim()
            } else {
                prefs.remove(CONTACT2_PHONE_KEY)
                prefs.remove(CONTACT2_NAME_KEY)
            }
        }
        return true
    }

    // Edit or add a contact later from Settings or Setup
    suspend fun saveContact(id: String?, name: String, phone: String): Boolean {
        if (!PhoneValidator.isValid(phone)) return false
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        context.myDataStore.edit { prefs ->
            when (id) {
                "1" -> {
                    prefs[CONTACT1_NAME_KEY] = trimmedName
                    prefs[CONTACT1_PHONE_KEY] = trimmedPhone
                }
                "2" -> {
                    prefs[CONTACT2_NAME_KEY] = trimmedName
                    prefs[CONTACT2_PHONE_KEY] = trimmedPhone
                }
                else -> {
                    // If adding without a pre-assigned id:
                    // If contact 1 is not set, set contact 1; otherwise set contact 2
                    val c1 = prefs[CONTACT1_NAME_KEY]
                    if (c1.isNullOrBlank()) {
                        prefs[CONTACT1_NAME_KEY] = trimmedName
                        prefs[CONTACT1_PHONE_KEY] = trimmedPhone
                    } else {
                        prefs[CONTACT2_NAME_KEY] = trimmedName
                        prefs[CONTACT2_PHONE_KEY] = trimmedPhone
                    }
                }
            }
        }
        return true
    }

    // Delete contact by id ("1" or "2")
    suspend fun removeContact(id: String) {
        context.myDataStore.edit { prefs ->
            if (id == "2") {
                prefs.remove(CONTACT2_NAME_KEY)
                prefs.remove(CONTACT2_PHONE_KEY)
            } else if (id == "1") {
                val c2Name = prefs[CONTACT2_NAME_KEY]
                val c2Phone = prefs[CONTACT2_PHONE_KEY]
                if (!c2Name.isNullOrBlank() && !c2Phone.isNullOrBlank()) {
                    // Promote contact 2 to contact 1 if contact 1 is deleted
                    prefs[CONTACT1_NAME_KEY] = c2Name
                    prefs[CONTACT1_PHONE_KEY] = c2Phone
                    prefs.remove(CONTACT2_NAME_KEY)
                    prefs.remove(CONTACT2_PHONE_KEY)
                } else {
                    prefs.remove(CONTACT1_NAME_KEY)
                    prefs.remove(CONTACT1_PHONE_KEY)
                }
            }
        }
    }

    // Direct helper to remove contact 2 from settings
    suspend fun removeContact2() = removeContact("2")

    // Call after finishing the onboarding
    suspend fun setOnboardingDone(done: Boolean) {
        context.myDataStore.edit { it[ONBOARDING_DONE_KEY] = done }
    }

    // Flows to retrieve data from preferences
    val userDOBFlow: Flow<Int> = context.myDataStore.data.map {
        it[USER_DOB_KEY] ?: 0
    }

    val userNameFlow: Flow<String> = context.myDataStore.data.map {
        it[USER_NAME_KEY] ?: ""
    }

    val userMailFlow: Flow<String> = context.myDataStore.data.map {
        it[USER_MAIL_KEY] ?: ""
    }

    val onboardingDoneFlow: Flow<Boolean> = context.myDataStore.data.map {
        it[ONBOARDING_DONE_KEY] ?: false
    }

    val onboardingDone: Flow<Boolean> get() = onboardingDoneFlow

    val contactsFlow: Flow<List<Contact>> = context.myDataStore.data.map { prefs ->
        val list = mutableListOf<Contact>()
        val c1Name = prefs[CONTACT1_NAME_KEY] ?: ""
        val c1Phone = prefs[CONTACT1_PHONE_KEY] ?: ""
        if (c1Name.isNotBlank() && c1Phone.isNotBlank()) {
            list.add(Contact(id = "1", name = c1Name, phone = c1Phone))
        }
        val c2Name = prefs[CONTACT2_NAME_KEY] ?: ""
        val c2Phone = prefs[CONTACT2_PHONE_KEY] ?: ""
        if (c2Name.isNotBlank() && c2Phone.isNotBlank()) {
            list.add(Contact(id = "2", name = c2Name, phone = c2Phone))
        }
        list
    }
}
