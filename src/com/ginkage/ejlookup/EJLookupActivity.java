package com.ginkage.ejlookup;

import java.util.ArrayList;

import com.google.android.vending.expansion.downloader.Helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.util.Linkify;
//import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

public class EJLookupActivity extends Activity {
	private static final int ID_DIALOG_ABOUT = 0;
	private static final int ID_DIALOG_NODICT = 1;
	private static ArrayList<ResultLine> reslist = null;
	private static GetSuggestTask getSuggest = null;
	private static GetLookupResultsTask getResult = null;
	private static boolean keepMount = false;
	private static SharedPreferences preferences = null;
	private AutoCompleteTextView query = null;
	private ExpandableListView results = null;
	private StorageManager storageManager = null;
	private ClipboardManager clipboard = null;
	private String expFile = null;
	private ProgressDialog waitMount = null;
	private boolean initPath = false;

	private boolean expansionFilesDelivered() {
		expFile = null;
		for (DictionaryDownloaderActivity.XAPKFile xf : DictionaryDownloaderActivity.xAPKS) {
			String fileName = Helpers.getExpansionAPKFileName(this, xf.mIsMain, xf.mFileVersion);
			if (!Helpers.doesFileExist(this, fileName, xf.mFileSize, false))
				return false;
			if (xf.mIsMain)
				expFile = Helpers.generateSaveFileName(this, fileName);
		}
		return true;
	}

	public static boolean getBoolean(String key, boolean defValue)
	{
		return preferences.getBoolean(key, defValue);
	}

	public static String getString(String key, String defValue)
	{
		return preferences.getString(key, defValue);
	}
	
	private void setResults()
	{
		final MyExpandableListAdapter adResults = new MyExpandableListAdapter(results.getContext(), new ArrayList<String>(), new ArrayList<ArrayList<ResultLine>>());
		adResults.setData(reslist);

		results.setAdapter(adResults);
		int i, groups = adResults.getGroupCount();
		for (i = 0; i < groups; i++)
			results.expandGroup(i);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Log.i("activity", "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		setProgressBarIndeterminateVisibility(getResult != null);
		if (getResult != null)
			getResult.curContext = this;

		if (!expansionFilesDelivered()) {
			startActivity(new Intent(EJLookupActivity.this, DictionaryDownloaderActivity.class));
			finish();
			return;
		}

		Nihongo.Init(getResources());

		storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		query = (AutoCompleteTextView)findViewById(R.id.editQuery);
		results = (ExpandableListView)findViewById(R.id.listResults);
        Button search = (Button)findViewById(R.id.buttonSearch);

		results.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)menuInfo;
				if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
					menu.setHeaderTitle("EJLookup");
					menu.add(0, v.getId(), 0, "Копировать");
				}
			}
		});

		if (reslist != null)
			setResults();

		query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

		query.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					searchClicked(v);
					return true;
				}
				return false;
			}
		});

		query.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (!initPath || !storageManager.isObbMounted(expFile)) {
					Mount();
					return;
				}

				if (getSuggest != null) {
					getSuggest.cancel(true);
					getSuggest = null;
				}

				query.dismissDropDown();
				query.setAdapter((ArrayAdapter<String>)null);

				if (!s.equals("")) {
					getSuggest = new GetSuggestTask();
					getSuggest.execute(s.toString());
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
		});

		search.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				CharSequence paste = clipboard.getText();

				if (paste == null || paste.length() == 0)
					Toast.makeText(EJLookupActivity.this, "Буфер обмена пуст", Toast.LENGTH_LONG).show();
				else {
					query.setText(paste);
					query.setSelection(paste.length());
					searchClicked(v);
				}

				return true;
			}
		});
	}

	private final OnObbStateChangeListener mStateListener = new OnObbStateChangeListener() {
		public void onObbStateChange(String path, int state) {
			if (state == MOUNTED)
				initPath = DictionaryTraverse.Init(storageManager.getMountedObbPath(path));

			if (waitMount != null) {
				waitMount.dismiss();
				waitMount = null;
			}
		}
	};

	void Mount()
	{
		//Log.i("activity", "Mount");
		initPath = false;
		keepMount = false;

		if (storageManager.isObbMounted(expFile))
			initPath = DictionaryTraverse.Init(storageManager.getMountedObbPath(expFile));
		else if (waitMount == null) {
			waitMount = ProgressDialog.show(this, "Загрузка словарей", "Подождите пожалуйста", true);
			storageManager.mountObb(expFile, null, mStateListener);
		}
	}

	void Unmount()
	{
		//Log.i("activity", "Unmount");
		if (storageManager != null && storageManager.isObbMounted(expFile))
			storageManager.unmountObb(expFile, false, mStateListener);
	}

	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		//Log.i("activity", "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		keepMount = true;
	}

	@Override
	protected void onStart()
	{
		//Log.i("activity", "onStart");
		super.onStart();

		if (!expansionFilesDelivered()) {
			finish();
			return;
		}

		Mount();
	}

	@Override
	protected void onStop()
	{
		//Log.i("activity", "onStop");

		if (getResult != null)
			getResult.curContext = null;

		if (getSuggest != null) {
			getSuggest.cancel(true);
			getSuggest = null;
		}

		super.onStop();
	}

	@Override
	public void onDestroy() {
		//Log.i("activity", "onDestroy");

		if (!keepMount)
			Unmount();

		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_ABOUT)
			return createAboutDialog(this);
		else if (id == ID_DIALOG_NODICT) {
			final TextView message = new TextView(this);
			final SpannableString s = new SpannableString("Словари не найдены!\n\nПопробуйте заново запустить приложение.\n");
//			final SpannableString s = new SpannableString("Словари не найдены!\n\nЗайдите в настройки для скачивания словарей, либо скачайте их по адресу\nhttp://bit.ly/ejldic\nи распакуйте в директорию\n" + DictionaryTraverse.filePath);
			message.setPadding(5, 5, 5, 5);
			message.setText(s);
			message.setGravity(Gravity.CENTER);
//			Linkify.addLinks(message, Linkify.ALL);
			return new AlertDialog.Builder(this).setTitle("EJLookup").setCancelable(true).setIcon(R.drawable.icon).setPositiveButton(
                    this.getString(android.R.string.ok), null).setView(message).create();
		}

		return super.onCreateDialog(id);
	}

	private static AlertDialog createAboutDialog(Context context)
	{
		// Try to load the a package matching the name of our own package
		PackageInfo pInfo;
		String versionInfo = "0.01";
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			versionInfo = pInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		String aboutTitle = String.format("О программе %s", context.getString(R.string.app_name));
		String versionString = String.format("Версия %s,\nGinKage (ginkage@yandex.ru)", versionInfo);
		String aboutText = "Простой словарик Японского языка\n\nПри создании использовались данные MONASH и проекта Warodai,\nhttp://warodai.ru";
 
		final TextView message = new TextView(context);
		final SpannableString s = new SpannableString(aboutText);
 
		message.setPadding(5, 5, 5, 5);
		message.setText(versionString + "\n\n" + s);
		message.setGravity(Gravity.CENTER);
		Linkify.addLinks(message, Linkify.ALL);
	 
		return new AlertDialog.Builder(context).setTitle(aboutTitle).setCancelable(true).setIcon(R.drawable.icon).setPositiveButton(
			 context.getString(android.R.string.ok), null).setView(message).create();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return true;
	}

	public void searchClicked(View view) {
		if (!initPath || !storageManager.isObbMounted(expFile)) {
			Toast.makeText(this, "Ошибка загрузки словарей!\nПопробуйте повторить попытку.", Toast.LENGTH_LONG).show();
			Mount();
		}
		else if (query.getText().length() == 0) {
			Toast.makeText(this, "Пожалуйста, введите запрос", Toast.LENGTH_LONG).show();
		}
		else if (getResult == null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(query.getWindowToken(), 0);

			if (getSuggest != null) {
				getSuggest.cancel(true);
				getSuggest = null;
			}
			query.dismissDropDown();
			query.setAdapter((ArrayAdapter<String>)null);

			setProgressBarIndeterminateVisibility(true);
			results.setAdapter((MyExpandableListAdapter)null);
			reslist = null;
			getResult = new GetLookupResultsTask();
			getResult.curContext = this;
			getResult.execute(query.getText().toString());
		}
	}

	private class GetSuggestTask extends AsyncTask<String, Integer, ArrayList<String>> {
		@Override
		protected ArrayList<String> doInBackground(String... args) {
			if (args != null) {
				String request = args[0];
				return Suggest.getLookupResults(request, this);
			} else {
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
		}

		@Override
		protected void onPostExecute(ArrayList<String> lines) {
			if (lines != null) {
				ArrayAdapter<String> suggest = new ArrayAdapter<String>(EJLookupActivity.this,
						preferences.getString("themeColor", "0").equals("1") ? R.layout.list_item_light : R.layout.list_item_dark, lines);
				query.setAdapter(suggest);
				query.showDropDown();
			}
		}
	}

	private class GetLookupResultsTask extends AsyncTask<String, Integer, ArrayList<ResultLine>> {
		private EJLookupActivity curContext;

		@Override
		protected ArrayList<ResultLine> doInBackground(String... args) {
			if (args != null) {
				String request = args[0];
				ResultLine.StartFill();
				return DictionaryTraverse.getLookupResults(request);
			} else {
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
		}

		@Override
		protected void onPostExecute(ArrayList<ResultLine> lines) {
			reslist = lines;

			if (curContext != null) {
				if (lines == null)
					Toast.makeText(getApplicationContext(), "Произошла непредвиденная ошибка", Toast.LENGTH_LONG).show();
				else if (lines.size() == 0) {
					if (DictionaryTraverse.hasDicts)
						Toast.makeText(getApplicationContext(), "Ничего не найдено", Toast.LENGTH_LONG).show();
					else
						curContext.showDialog(ID_DIALOG_NODICT);
				}
				else {
					if (lines.size() >= DictionaryTraverse.maxres)
						Toast.makeText(getApplicationContext(), "Слишком много результатов, показаны первые " + DictionaryTraverse.maxres, Toast.LENGTH_LONG).show();

					curContext.setResults();
				}

				curContext.setProgressBarIndeterminateVisibility(false);
			}

			getResult = null;
		}

		@Override
		protected void onCancelled() {
			if (curContext == null) return;
			curContext.setProgressBarIndeterminateVisibility(false);
			getResult = null;
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if (featureId == Window.FEATURE_OPTIONS_PANEL) {
			if (item.getItemId() == R.id.itemSettings) {
				Intent i = new Intent(EJLookupActivity.this, Settings.class);
				startActivity(i);
			}
			else if (item.getItemId() == R.id.itemAbout)
				showDialog(ID_DIALOG_ABOUT);
			return true;
		}
		else if (featureId == Window.FEATURE_CONTEXT_MENU) {
			ContextMenuInfo menuInfo = item.getMenuInfo();
			if (menuInfo instanceof ExpandableListContextMenuInfo) {
				ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)item.getMenuInfo();
				TextView textView = (TextView) info.targetView;
				clipboard.setText(textView.getText().toString());
			}
			return true;
		}
		else
			return false;
	}
}
