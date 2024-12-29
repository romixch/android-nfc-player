package ch.romix.nfcplayer

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class PlayerSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.player_settings_preferences, rootKey)
    }
}