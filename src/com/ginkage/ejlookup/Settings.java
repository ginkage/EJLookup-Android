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
	private static final int ID_DIALOG_DOWNLOAD = 0;
	private static DownloadFile dlTask = null;
	private static LinkedList<String> queue = null;
	private ProgressDialog mProgressDialog = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EJLookupActivity.getBoolean("sugRoman", true);
		addPreferencesFromResource(R.xml.settings);
		PreferenceCategory listDict = (PreferenceCategory)findPreference("dict");

		if (queue == null)
			queue = new LinkedList<String>();

		if (dlTask != null)
			dlTask.curContext = this;

		int i, n = DictionaryTraverse.fileList.length;
		for (i = 0; i < n; i++) {
			CheckBoxPreference cb = new CheckBoxPreference(this);
			String fileName = DictionaryTraverse.fileList[i];
			cb.setKey(fileName);
			cb.setTitle(fileName);
			boolean exists = DictionaryTraverse.checkExists(fileName);
			boolean checked = EJLookupActivity.getBoolean(fileName, true);
			cb.setOnPreferenceChangeListener(new  Preference.OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (newValue instanceof Boolean) {
						Boolean val = (Boolean)newValue;
						if (val)
							Download(preference.getKey());
					}
					return true;
				}
			});
			listDict.addPreference(cb);
			cb.setChecked(checked && exists);
		}
	}
	
	@Override
	public void onDestroy() {
		if (dlTask != null) {
			dlTask.curContext = null;
			dlTask.mDialog = null;
		}
		super.onDestroy();
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		if (preference.getKey().equals("download")) {
			startActivity(new Intent(this, DictionaryDownloaderActivity.class));

/*			Download("suggest");
			int i, n = DictionaryTraverse.fileList.length;
			for (i = 0; i < n; i++) {
				String fileName = DictionaryTraverse.fileList[i];
				Download(fileName);
			}
*/		}

		return true;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_DOWNLOAD) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setMessage("Скачивается " + (dlTask != null ? dlTask.getName() : ""));
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					if (dlTask != null)
						dlTask.cancel(false);
				}
			});
			
			if (dlTask != null)
				dlTask.mDialog = mProgressDialog;
	
			return mProgressDialog;
		}
	
		return super.onCreateDialog(id);
	}

	private void Download(String fileName)
	{
		if (fileName != null)
			queue.add(fileName);

		while (queue.size() > 0 && DictionaryTraverse.checkExists(queue.getFirst()))
				queue.removeFirst();

		if (queue.size() > 0 && dlTask == null) {
			fileName = queue.getFirst();
			queue.removeFirst();

			dlTask = new DownloadFile();
			dlTask.fname = fileName;
			dlTask.curContext = this;
			dlTask.mDialog = mProgressDialog;
			dlTask.execute(fileName);

			if (mProgressDialog != null) {
				mProgressDialog.setMessage("Скачивается " + fileName);
				mProgressDialog.setProgress(0);
			}
		}
	}

	private class DownloadFile extends AsyncTask<String, Integer, String> {
		private String title;
		private String fname;
		private Settings curContext = null;
		private ProgressDialog mDialog = null; 

		@Override
		protected void onPreExecute() {
			showDialog(ID_DIALOG_DOWNLOAD);
		}
		
		public String getName()
		{
			return (fname == null ? "" : fname);
		}

		@Override
		protected String doInBackground(String... fileName) {
			try {
				fname = fileName[0];
				title = "Скачивается " + fname;
				URL url = new URL("http://dl.dropbox.com/u/8563400/" + fname + ".zip");
				URLConnection conexion = url.openConnection();
				conexion.connect();
				// this will be useful so that you can show a typical 0-100% progress bar
				int lenghtOfFile = conexion.getContentLength();

				// download the file
				File file = new File(DictionaryTraverse.filePath + File.separator + fname + ".zip");
				InputStream input = new BufferedInputStream(url.openStream(), 65536);

				if (CopyStream(input, file, lenghtOfFile)) {
					ZipFile zipfile = new ZipFile(file);
					Enumeration<? extends ZipEntry> e = zipfile.entries();
					while (e.hasMoreElements() && !isCancelled()) {
						ZipEntry entry = e.nextElement();
						unzipEntry(zipfile, entry, DictionaryTraverse.filePath);
					}
				}
				file.delete();

				return fname;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		private boolean CopyStream(InputStream input, File file, long lenghtOfFile) throws IOException
		{
			if (!file.getParentFile().exists())
				createDir(file.getParentFile());

			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file), 65536);

			int count;
			byte data[] = new byte[65536];
			long total = 0;
			
			while ((count = input.read(data)) != -1 && !isCancelled()) {
				total += count;
				// publishing the progress....
				publishProgress((int)(total*100/lenghtOfFile));
				output.write(data, 0, count);
			}

			output.flush();
			output.close();
			input.close();
			
			if (isCancelled()) {
				file.delete();
				return false;
			}
			
			return true;
		}

		private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir) throws IOException {
			if (entry.isDirectory()) {
				createDir(new File(outputDir, entry.getName()));
				return;
			}

			File outputFile = new File(outputDir, entry.getName());
			title = "Распаковывается " + entry.getName();
			BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry), 65536);
			CopyStream(inputStream, outputFile, entry.getSize());
		}

		private void createDir(File dir) {
			if (!dir.exists() && !dir.mkdirs())
				throw new RuntimeException("Не удалось создать директорию " + dir);
		}

		@Override
		protected void onProgressUpdate(Integer... args) {
			if (mDialog != null) {
				mDialog.setMessage(title);
				mDialog.setProgress(args[0]);
			}
		}

		private void endTask(boolean cancel)
		{
			if (fname != null && curContext != null) {
				PreferenceCategory listDict = (PreferenceCategory)curContext.findPreference("dict");
				Preference pref = listDict.findPreference(fname);
				if (pref != null && pref instanceof CheckBoxPreference) {
					CheckBoxPreference cb = (CheckBoxPreference) pref;
					cb.setChecked(DictionaryTraverse.checkExists(fname));
				}
			}

			if (curContext != null && mDialog != null && mDialog.isShowing())
				curContext.dismissDialog(ID_DIALOG_DOWNLOAD);
			mDialog = null;
			dlTask = null;

			if (cancel) {
				while (queue.size() > 0)
					queue.removeFirst();
			}
			else if (curContext != null)
				curContext.Download(null);
			curContext = null;
		}

		@Override
		protected void onPostExecute(String arg) {
			if (arg == null)
				Toast.makeText(getApplicationContext(), "Произошла ошибка ввода-вывода, проверьте интернет-соединение и наличие свободного места", Toast.LENGTH_LONG).show();
			endTask(arg == null);
		}

		@Override
		protected void onCancelled() {
			endTask(true);
		}
	}
}
