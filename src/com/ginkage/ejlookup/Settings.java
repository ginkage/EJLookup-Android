package com.ginkage.ejlookup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;


public class Settings extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EJLookupActivity.getBoolean(getString(R.string.setting_suggest_romaji), true);
		addPreferencesFromResource(R.xml.settings);
		PreferenceCategory listDict = (PreferenceCategory)findPreference(getString(R.string.setting_dictionaries));

        int i = 0;
        for (String fileName : DictionaryTraverse.fileList) {
            boolean exists = DictionaryTraverse.checkExists(fileName);
            boolean checked = EJLookupActivity.getBoolean(fileName, true);

			CheckBoxPreference cb = new CheckBoxPreference(this);
			cb.setKey(fileName);
			cb.setTitle(fileName);
            cb.setSummary(DictionaryTraverse.fileDesc[i++]);
            cb.setChecked(checked && exists);
            listDict.addPreference(cb);
		}
	}
}
