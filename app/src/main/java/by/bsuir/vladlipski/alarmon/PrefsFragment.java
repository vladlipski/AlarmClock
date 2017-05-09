package by.bsuir.vladlipski.alarmon;


import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;


public class PrefsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_settings);

        getActivity().setTitle(R.string.app_settings);
    }


}
