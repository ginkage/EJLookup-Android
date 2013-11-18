package com.ginkage.ejlookup;

import java.util.ArrayList;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

class ResultLine {
	private SpannableString data;
	private String group;

	private static int rescount, resmax;
	private static int font_size;
    public static int theme_color;

	public static void StartFill(int fontSize, int themeColor)
	{
		font_size = fontSize;
        theme_color = themeColor;
		rescount = 0;
		resmax = 250;
	}

	static private String getSubstr(char[] text, int begin)
	{
		int i, len = text.length, end = -1;
		for (i = begin; i < len && text[i] != 0; i++)
			end = i;
		if (end < 0) return "";
		return new String(text, begin, end - begin + 1);
	}

	static private int strchr(char[] text, int begin, char c)
	{
		int i, len = text.length;
		for (i = begin; i < len && text[i] != 0; i++)
			if (text[i] == c)
				return i;
		return -1;
	}

	private class Span {
	    public final int start;
	    public final int end;

	    public Span(int start, int end) {
	        this.start = start;
	        this.end = end;
	    }
	}

	private SpannableString ParseResult(String data)
	{
		String result = "";
		int i, i0 = -1, i1 = -1, i2 = -1, i3 = -1, i4 = -1, i5 = -1;
		int kanji = -1, kana = -1, trans = -1, roshi = -1, p;
		String sdict = "Default", skanji = "", skana = "", strans = "";
		boolean kd = false;

		if (++rescount > resmax) return null;

		char[] text = new char[data.length()];
		data.getChars(0, data.length(), text, 0);
		int len = text.length;

		for (i = 0; i < len; i++)
			if (i0 < 0 && text[i] == ')')
				i0 = i;
			else if (i1 < 0 && i3 < 0 && text[i] == '[')
				i1 = i;
			else if (i2 < 0 && i3 < 0 && text[i] == ']')
				i2 = i;
			else if (i4 < 0 && i3 < 0 && text[i] == '{')
				i4 = i;
			else if (i5 < 0 && i3 < 0 && text[i] == '}')
				i5 = i;
			else if (i3 < 0 && text[i] == '/' && (i == 0 || text[i-1] != '<'))
				i3 = i;

		i0 = -1;
		sdict = this.group;

		if (sdict.startsWith("kanjidic")) {
			kanji = i0 + 1;
			while (kanji < len && text[kanji] == ' ') kanji++;
			p = strchr(text, kanji, ' ');
			if (p >= 0) {
				text[p] = '\0';
				kana = p + 1;
				for (p = kana; p < len && text[p] != 0; p++)
					if (text[p] > 127) {
						kana = p;
						break;
					}

				p = strchr(text, kana, '{');
				if (p >= 0) {
					text[p-1] = '\0';
					trans = p;
				}
			}
			kd = true;
		}
		else {
			trans = i0 + 1;
			if (i1 >= 0 && i2 >= 0 && i1 < i2) {
				text[i1] = '\0';
				text[i2] = '\0';
				kana = i1 + 1;
				trans = i2 + 1;
				if (i0 < i1) {
					kanji = i0 + 1;
					while (kanji < len && text[kanji] == ' ')
						kanji++;
				}
			}

			if (i3 >= 0 && i3 > i0 && i3 > i1 && i3 > i2 && i3 > i4 && i3 > i5) {
				if (kana < 0) kana = trans;
				text[i3] = '\0';
				trans = i3 + 1;
			}

			if (i4 >= 0 && i5 >= 0 && i4 < i5) {
				text[i4] = '\0';
				text[i5] = '\0';
				roshi = i4 + 1;
			}
		}

		if (kanji >= 0) {
			int end = kanji;
			while (end < len && text[end] != 0) end++;
			if (end > kanji) {
				end--;
				while (end > kanji && text[end] == ' ')
					text[end--] = '\0';
			}
			skanji = getSubstr(text, kanji);
		}

		if (kana >= 0) {
			for (p = kana; p < len && text[p] != 0; ) {
				while (p < len && (text[p] == ' ' || text[p] == ',')) p++;
				if (p < len && text[p] != 0) {
					int begin = p, end = p - 1;
					while (p < len && text[p] != 0 && text[p] != ' ' && text[p] != ',') { end = p; p++; }
					if (end >= begin) {
						if (!skana.equals("")) skana += "\n";
						skana += "[" + new String(text, begin, end - begin + 1);
						if (text[begin] > 127) {
							skana += (kd ? " / " : "]\n[");
							skana += Nihongo.Romanate(text, begin, end);
						}
						skana += "]";
					}
				}
			}
		}

		if (roshi >= 0) {
			for (p = roshi; p < len && text[p] != 0; ) {
				while (p < len && (text[p] == ' ' || text[p] == ',')) p++;
				if (p < len && text[p] != 0) {
					int begin = p, end = p - 1;
					while (p < len && text[p] != 0 && text[p] != ' ' && text[p] != ',') { end = p; p++; }
					if (end >= begin) {
						if (!skana.equals("")) skana += "\n";
						skana += "[" + new String(text, begin, end - begin + 1) + "]";
					}
				}
			}
		}

		if (trans >= 0) {
			if (kd) {
				for (p = trans; p < len && text[p] != 0; ) {
					while (p < len && (text[p] == '{' || text[p] == '}')) p++;
					if (p < len && text[p] != 0) {
						while (p < len && text[p] == ' ') p++;
						int begin = p, end = p - 1;
						while (p < len && text[p] != 0 && text[p] != '{' && text[p] != '}') { end = p; p++; }
						if (end >= begin) {
							if (!strans.equals("")) strans += "\n";
							strans += new String(text, begin, end - begin + 1);
						}
					}
				}
			}
			else {
				for (p = trans; p < len && text[p] != 0; p++) {
					if (text[p] == '/' && (p == trans || text[p-1] != '<')) {
						text[p] = '\0';
						p++;
						while (trans < len && text[trans] == ' ') trans++;
						if (trans < len && text[trans] != 0) {
							if (!strans.equals("")) strans += "\n";
							strans += getSubstr(text, trans);
						}
						trans = p;
					}
				}
				if (trans >= 0) {
					while (trans < len && text[trans] == ' ') trans++;
					if (trans < len && text[trans] != 0) {
						if (!strans.equals("")) strans += "\n";
						strans += getSubstr(text, trans);
					}
				}
			}
		}

		int kanjistart = -1, kanjiend = -1, kanastart = -1, kanaend = -1, transstart = -1;
		if (!skanji.equals("")) {
			if (!result.equals("")) result += "\n";
			kanjistart = result.length();
			result += skanji;
		}
		kanjiend = result.length();
		if (!skana.equals("")) {
			if (!result.equals("")) result += "\n";
			kanastart = result.length();
			result += skana;
		}
		kanaend = result.length();
		
		ArrayList<Span> italic = new ArrayList<Span>();

		if (!strans.equals("")) {
			if (!result.equals("")) result += "\n";
			transstart = result.length();

			int begin, end;
			while ((begin = strans.indexOf("<i>")) >= 0) {
				result += strans.substring(0, begin);
				end = strans.indexOf("</i>", begin + 1);
				int is = result.length();
				if (end < 0) {
					result += strans.substring(begin + 3);
					strans = "";
				}
				else {
					result += strans.substring(begin + 3, end);
					strans = strans.substring(end + 4);
				}
				italic.add(new Span(is, result.length()));
			}

			result += strans;
		}

		SpannableString res = new SpannableString(result);

		if (font_size == 1)
			res.setSpan(new RelativeSizeSpan(1.333333f), 0, res.length(), 0);
		else if (font_size == 2)
			res.setSpan(new RelativeSizeSpan(1.666666f), 0, res.length(), 0);

		if (kanjistart >= 0) {
			res.setSpan(new ForegroundColorSpan((theme_color == 0 ? Color.rgb(170, 127, 85) : Color.rgb(127, 63, 31))), kanjistart, kanjiend, 0);
			res.setSpan(new RelativeSizeSpan(1.333333f), kanjistart, kanjiend, 0);
		}

		if (kanastart >= 0) {
			res.setSpan(new ForegroundColorSpan((theme_color == 0 ? Color.rgb(42, 170, 170) : Color.rgb(31, 63, 63))), kanastart, kanaend, 0);
		}

		if (transstart >= 0) {
            for (Span s : italic)
				res.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), s.start, s.end, 0);
		}

		return res;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public ResultLine(String data, String dict) {
		this.group = dict;
		this.data = ParseResult(data);
	}

	public SpannableString getData() {
		return data;
	}
	
	public void setData(SpannableString data) {
		this.data = data;
	}
}
