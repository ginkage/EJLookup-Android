package com.ginkage.ejlookup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.os.AsyncTask;

class Suggest {
	private static int Tokenize(char[] text, int len, RandomAccessFile fileIdx, HashMap<String, Integer> suggest, AsyncTask<String, Integer, ArrayList<String>> task, long sugPos) throws IOException
	{
		int p, last = -1;

		for (p = 0; p < len; p++)
			if (Nihongo.letter(text[p]) || (text[p] == '\'' && p > 0 && p+1 < len && Nihongo.letter(text[p-1]) && Nihongo.letter(text[p+1]))) {
				if (last < 0) {
					last = p;
				}
			}
			else if (last >= 0)	{
				last = -1;
			}

		if (last >= 0) // Only search for the last word entered
			Traverse(new String(text, last, p - last), fileIdx, 0, "", suggest, task, sugPos);
		
		return last;
	}
	
	static class Pair implements Comparable<Pair>
	{
		final String line;
		final int freq;

		public final int compareTo(Pair other)
		{
			if (this.freq != other.freq)
				return other.freq - this.freq;
			return this.line.compareToIgnoreCase(other.line);
		}

		Pair(String line, int freq)
		{
			this.line = line;
			this.freq = freq;
		}
	}

	public static ArrayList<String> getLookupResults(Context context, String request, AsyncTask<String, Integer, ArrayList<String>> task)
	{
		ArrayList<String> result = null;

		int maxsug = Integer.parseInt(EJLookupActivity.getString(context.getString(R.string.setting_max_suggest), "10"));
		boolean romanize = EJLookupActivity.getBoolean(context.getString(R.string.setting_suggest_romaji), true);

		char[] text = new char[request.length()];
		request.getChars(0, request.length(), text, 0);

		String kanareq = Nihongo.Kanate(text);
		char[] kanatext = new char[kanareq.length()];
		kanareq.getChars(0, kanareq.length(), kanatext, 0);

		int qlen = Nihongo.Normalize(text);
		int klen = Nihongo.Normalize(kanatext);

		HashMap<String, Integer> suggest = new HashMap<String, Integer>();

		int last = -1;
		try {
			File idx;
			long sugPos = 0;

			if (DictionaryTraverse.bugKitKat) {
				idx = new File(DictionaryTraverse.filePath);
				sugPos = DictionaryTraverse.sugPos;
			}
			else {
				idx = new File(DictionaryTraverse.filePath + "suggest.dat");
			}

			if (!idx.exists()) return null;
			RandomAccessFile fileIdx = new RandomAccessFile(idx.getAbsolutePath(), "r");

			last = Tokenize(text, qlen, fileIdx, suggest, task, sugPos);
			if (!Arrays.equals(text, kanatext))
				Tokenize(kanatext, klen, fileIdx, suggest, task, sugPos);
			fileIdx.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!suggest.isEmpty() && !task.isCancelled()) {
			result = new ArrayList<String>(10);
			TreeSet<Pair> freq = new TreeSet<Pair>();
			for (String str : suggest.keySet()) {
				Integer n = suggest.get(str);
				freq.add(new Pair(str, n));
			}

			HashSet<String> duplicate = new HashSet<String>();
			String begin = (last >= 0 ? request.substring(0, last) : "");
			for (Pair pit : freq)
				if (result.size() < maxsug) {
					String str = pit.line;
					String k = str;

					if (romanize) {
						int i;
						boolean convert = true;
						for (i = 0; i < str.length(); i++)
							if (str.charAt(i) >= 0x3200) {
								convert = false;
								break;
							}

						if (convert) {
							char[] txt = new char[str.length()];
							str.getChars(0, str.length(), txt, 0);
							k = Nihongo.Romanate(txt, 0, str.length() - 1);
						}
					}

					if (!duplicate.contains(k)) {
						result.add(begin + k);
						duplicate.add(k);
					}
				}
		}

		return result;
	}

	private static int betole(int p)
	{
		return ((p & 0x000000ff) << 24) + ((p & 0x0000ff00) << 8) + ((p & 0x00ff0000) >>> 8) + ((p & 0xff000000) >>> 24);
	}

	private static char shtoch(int p)
	{
		return (char) (((p & 0x000000ff) << 8) + ((p & 0x0000ff00) >>> 8));
	}

	private static boolean Traverse(String word, RandomAccessFile fidx, long pos, String str, HashMap<String, Integer> suglist, AsyncTask<String, Integer, ArrayList<String>> task, long sugPos) throws IOException
	{
		if (task.isCancelled()) return false;
		fidx.seek(pos + sugPos);

		int tlen = fidx.readUnsignedByte();
		int c = fidx.readUnsignedByte();
		int freq = betole(fidx.readInt());
		boolean children = ((c & 1) != 0), unicode = ((c & 8) != 0), exact = !(word.equals(""));
		int match = 0, nlen = 0, wlen = word.length(), p;
		char ch;

		if (pos > 0) {
			String nword = "";

			if (tlen > 0) {
				if (unicode) {
					char[] wbuf = new char[tlen];
					for (c = 0; c < tlen; c++)
						wbuf[c] = shtoch(fidx.readUnsignedShort());
					nword = new String(wbuf);
				}
				else {
					byte[] wbuf = new byte[tlen];
					fidx.read(wbuf, 0, tlen);
					nword = new String(wbuf);
				}
			}

			nlen = nword.length();
			str += nword;

			if (exact) {
				word = word.substring(1);
				wlen--;
	
				while (match < wlen && match < nlen) {
					if (word.charAt(match) != nword.charAt(match))
						break;
					match++;
				}
			}
		}

		if (match == nlen || match == wlen) {
			TreeMap<Integer, Character> cpos = new TreeMap<Integer, Character>();
			exact = exact && (match == nlen);

			if (children) // One way or the other, we'll need a full children list
				do { // Read it from this location once, save for later
					ch = shtoch(fidx.readUnsignedShort());
					p = betole(fidx.readInt());
					if (match < wlen) { // (match == nlen), Traverse children
						if (ch == word.charAt(match)) {
							String newWord = word.substring(match, word.length());
							return Traverse(newWord, fidx, (p & 0x7fffffff), str + ch, suglist, task, sugPos); // Traverse children
						}
					}
					else
						cpos.put(p & 0x7fffffff, ch);
				} while ((p & 0x80000000) == 0);

			if (match == wlen) {
				if (freq > 0 && !exact) {
					Integer v = suglist.get(str);
					if (v == null)
						v = 0;
					v += freq;
					suglist.put(str, v);
				}

				for (int child_pos : cpos.keySet())
					Traverse("", fidx, child_pos, str + cpos.get(child_pos), suglist, task, sugPos); // Traverse everything that begins with this word

				return true; // Got result
			}
		}

		return false; // Nothing found
	}
}
