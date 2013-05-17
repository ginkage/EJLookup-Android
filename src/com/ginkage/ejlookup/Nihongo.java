package com.ginkage.ejlookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.res.Resources;

class Nihongo {
	private static char[][] roma = null;
	private static char[][] kana = null;
	private static char[] hashtab = null;
	
	private static char[][] readResource(Resources res, int id)
	{
		char[][] table = null;

		try {
			InputStream dataIn = res.openRawResource(id);
			InputStreamReader isr = new InputStreamReader(dataIn, "UTF-8");
			BufferedReader br = new BufferedReader(isr);  
			String line;
			ArrayList<String> lines = new ArrayList<String>();

			while ((line = br.readLine()) != null)
				lines.add(line);
			table = new char[lines.size()][];
			Iterator<String> it = lines.iterator();

			int count = 0;
			while (it.hasNext()) {
				line = it.next();
				table[count] = new char[line.length()];
				line.getChars(0, line.length(), table[count], 0);
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return table;
	}

	public static void Init(Resources res)
	{
		if (hashtab == null) {
			hashtab = new char[65536];

			for (int i = 0; i < 65536; i++)
				if ((i >= 'A' && i <= 'Z') ||
					(i >= 0x0410 && i <= 0x042F))
					hashtab[i] = (char) (i + 0x20);
				else if (i == 0x0451 || i == 0x0401)
					hashtab[i] = 0x0435;
				else if (i == 0x040E || i == 0x045E)
					hashtab[i] = 0x0443;
				else if (i == 0x3000)
					hashtab[i] = 0x0020;
				else if (i >= 0x30A1 && i <= 0x30F4)
					hashtab[i] = (char) (i - 0x60);
				else if (i >= 0xFF01 && i <= 0xFF20)
					hashtab[i] = (char) (i - 0xFEE0);
				else if (i >= 0xFF21 && i <= 0xFF3A)
					hashtab[i] = (char) (i - 0xFEC0);
				else if (i >= 0xFF3B && i <= 0xFF5E)
					hashtab[i] = (char) (i - 0xFEE0);
				else
					hashtab[i] = (char) i;
		}

		if (kana == null)
			kana = readResource(res, R.raw.kanatab);

		if (roma == null)
			roma = readResource(res, R.raw.romatab);
	}

	private static boolean Jaiueoy(char c)
	{
		return (c >= 0x3041 && c <= 0x304A) || (c >= 0x30A1 && c <= 0x30AA) ||
				(c >= 0x3083 && c <= 0x3088) || (c >= 0x30E3 && c <= 0x30E8);
	}

	private static boolean aiueo(char c)
	{
		return c == 'a' || c == 'i' || c == 'u' || c == 'e' || c == 'o';
	}

	private static char tolower(char c)
	{
		return (c >= 'A' && c <= 'Z' || (c >= 0x0410 && c <= 0x042F)) ? (char) (c + 0x20) : c;
	}

	public static boolean letter(char c)
	{
		return ((c >= '0' && c <= '9') ||
			(c >= 'A' && c <= 'Z') ||
			(c >= 'a' && c <= 'z') ||
			(c >= 0x00C0 && c <= 0x02A8) ||
			(c >= 0x0401 && c <= 0x0451) ||
			c == 0x3005 ||
			(c >= 0x3041 && c <= 0x30FA) ||
			(c >= 0x4E00 && c <= 0xFA2D) ||
			(c >= 0xFF10 && c <= 0xFF19) ||
			(c >= 0xFF21 && c <= 0xFF3A) ||
			(c >= 0xFF41 && c <= 0xFF5A) ||
			(c >= 0xFF66 && c <= 0xFF9F));
	}

	private static int findsub(char[] str, int offset)
	{
		int a = 0, b = roma.length - 1, cur;
		int psub, pstr;

		while (b-a > 1)
		{
			cur = (a+b)/2;
			psub = 0;
			pstr = offset;

			while (pstr < str.length && roma[cur][psub] != '=')
			{
				if (tolower(str[pstr]) < roma[cur][psub])
				{	b = cur;	break;	}
				else if (tolower(str[pstr]) > roma[cur][psub])
				{	a = cur;	break;	}
				pstr++;	psub++;
			}

			if (roma[cur][psub++] == '=') return cur;
			else if (pstr >= str.length) return -1;
		}

		psub = 0;
		pstr = offset;
		while (pstr < str.length && roma[a][psub] != '=')
		{
			if (tolower(str[pstr]) != roma[a][psub]) break;
			pstr++;	psub++;
		}
		if (roma[a][psub++] == '=') return a;
		else if (pstr >= str.length) return -1;

		if (a != b)
		{
			psub = 0;
			pstr = offset;
			while (pstr <= str.length && roma[b][psub] != '=')
			{
				if (tolower(str[pstr]) != roma[b][psub]) break;
				pstr++;	psub++;
			}
			if (roma[b][psub++] == '=') return b;
		}

		return -1;
	}

	public static String Kanate(char[] text)
	{
		int pb, pk = 0, pls, prs, r;
		String out = "";
		char[] kanabuf = new char[1024];
		boolean tsu;

		for (pb = 0; pb < text.length; pb++)
		{
			tsu = false;
			if (pb+1 < text.length && tolower(text[pb]) == tolower(text[pb+1]) && !aiueo(tolower(text[pb])))
			{
				if (pb+2 < text.length && tolower(text[pb]) == 'n' && tolower(text[pb+1]) == 'n' && tolower(text[pb+2]) == 'n')
				{
					out += 0x3093;
					pb++;
					continue;
				}

				tsu = true;
				pb++;
			}

			if (pb < text.length && ((pls = findsub(text, pb)) >= 0))
			{
				if (tsu)
				{
					if (tolower(text[pb-1]) == 'n') kanabuf[pk++] = 0x3093;
					else kanabuf[pk++] = 0x3063;
				}

				r = 0;
				while (roma[pls][r++] != '=') pb++;
				pb--;

				prs = pk;
				while (r < roma[pls].length) kanabuf[prs++] = roma[pls][r++];
				pk = prs;
			}
			else if (tolower(text[pb]) == 'n' || tolower(text[pb]) == 'm')
				kanabuf[pk++] = 0x3093;
			else
			{
				char[] tmp = new char[4];
				pls = -1;

				if (pb+1 < text.length && tolower(text[pb]) == 't' && tolower(text[pb+1]) == 's')
				{
					tmp[0] = 't'; tmp[1] = 's'; tmp[2] = 'u'; tmp[3] = '\0';
					pls = findsub(tmp, 0);
				}

				if (pb+1 < text.length && tolower(text[pb]) == 's' && tolower(text[pb+1]) == 'h')
				{
					tmp[0] = 's'; tmp[1] = 'h'; tmp[2] = 'i'; tmp[3] = '\0';
					pls = findsub(tmp, 0);
				}

				if (pls >= 0)
				{
					r = 0;
					pb++;
					while (roma[pls][r] != '=') r++;
					prs = pk;
					while (r < roma[pls].length) kanabuf[prs++] = roma[pls][r++];
					pk = prs;
				}
				else
				{
					if (tsu) out += text[pb-1];
					out += text[pb];
				}
			}

			if (pk != 0)
			{
				out += new String(kanabuf, 0, pk);
				pk = 0;
			}
		}
		
		return out;
	}

	public static String Romanate(char[] text, int begin, int end)
	{
		String out = "";
		int pkana, pk, pi, ps, pb;
		boolean tsu = false;

		for (pb = begin; pb <= end; pb++) {
			if ((text[pb] >= 0x3041 && text[pb] <= 0x3094) || (text[pb] >= 0x30A1 && text[pb] <= 0x30FC)) {
				if (text[pb] == 0x3063 || text[pb] == 0x30C3) {
					if (pb+1 <= end && ((text[pb+1] >= 0x3041 && text[pb+1] <= 0x3094) ||
						(text[pb+1] >= 0x30A1 && text[pb+1] <= 0x30FC)))
						tsu = true;
					else
						out += "ltsu";
					continue;
				}

				for (pkana = kana.length - 1; pkana >= 0; pkana--) {
					for (pk = 0, pi = pb; pi <= end && pk < kana[pkana].length && kana[pkana][pk] != '='; pk++, pi++)
						if (kana[pkana][pk] != text[pi] && kana[pkana][pk] != (text[pi]-0x60)) break;

					if (kana[pkana][pk] == '=') {
						ps = pk + 1;

						if (tsu) {
							out += kana[pkana][ps];
							tsu = false;
						}

						out += new String(kana[pkana], ps, kana[pkana].length - ps);
						if (text[pb] == 0x3093 && pb+1 <= end && Jaiueoy(text[pb+1])) out += '\'';

						pb = pi-1;
						break;
					}
				}

				if (pkana < 0) out += text[pb];
			}
			else out += text[pb];
		}

		return out;
	}

	public static int Normalize(char[] buffer)
	{
		int p, unibuf;
	
		for (unibuf = p = 0; p < buffer.length && buffer[p] != 0; p++) 
		{
			if (buffer[p] >= 0xFF61 && buffer[p] <= 0xFF9F)
			{
				switch (buffer[p])
				{
				case 0xFF61:	buffer[p] = 0x3002;	break;
				case 0xFF62:	buffer[p] = 0x300C;	break;
				case 0xFF63:	buffer[p] = 0x300D;	break;
				case 0xFF64:	buffer[p] = 0x3001;	break;
				case 0xFF65:	buffer[p] = 0x30FB;	break;
				case 0xFF66:	buffer[p] = 0x30F2;	break;
	
				case 0xFF67: case 0xFF68: case 0xFF69: case 0xFF6A: case 0xFF6B:
					buffer[p] = (char) ((buffer[p] - 0xFF67)*2 + 0x30A1);	break;
	
				case 0xFF6C: case 0xFF6D: case 0xFF6E:
					buffer[p] = (char) ((buffer[p] - 0xFF6C)*2 + 0x30E3); break;
	
				case 0xFF6F:	buffer[p] = 0x30C3;	break;
				case 0xFF70:	buffer[p] = 0x30FC;	break;
	
				case 0xFF71: case 0xFF72: case 0xFF73: case 0xFF74: case 0xFF75:
					buffer[p] = (char) ((buffer[p] - 0xFF71)*2 + 0x30A2); break;
	
				case 0xFF76: case 0xFF77: case 0xFF78: case 0xFF79: case 0xFF7A:
				case 0xFF7B: case 0xFF7C: case 0xFF7D: case 0xFF7E: case 0xFF7F:
				case 0xFF80: case 0xFF81:
					buffer[p] = (char) ((buffer[p] - 0xFF76)*2 + 0x30AB); break;
	
				case 0xFF82: case 0xFF83: case 0xFF84:
					buffer[p] = (char) ((buffer[p] - 0xFF82)*2 + 0x30C4); break;
	
				case 0xFF85: case 0xFF86: case 0xFF87: case 0xFF88: case 0xFF89:
					buffer[p] = (char) ((buffer[p] - 0xFF85) + 0x30CA); break;
	
				case 0xFF8A: case 0xFF8B: case 0xFF8C: case 0xFF8D: case 0xFF8E:
					buffer[p] = (char) ((buffer[p] - 0xFF8A)*3 + 0x30CF); break;
	
				case 0xFF8F: case 0xFF90: case 0xFF91: case 0xFF92: case 0xFF93:
					buffer[p] = (char) ((buffer[p] - 0xFF8F) + 0x30DE); break;
	
				case 0xFF94: case 0xFF95: case 0xFF96:
					buffer[p] = (char) ((buffer[p] - 0xFF94)*2 + 0x30E4); break;
	
				case 0xFF97: case 0xFF98: case 0xFF99: case 0xFF9A: case 0xFF9B:
					buffer[p] = (char) ((buffer[p] - 0xFF97) + 0x30E9); break;
	
				case 0xFF9C:	buffer[p] = 0x30EF;	break;
				case 0xFF9D:	buffer[p] = 0x30F3;	break;
	
				case 0xFF9E:	if (unibuf > 0) buffer[unibuf-1] += 1;	break;
				case 0xFF9F:	if (unibuf > 0) buffer[unibuf-1] += 2;	break;
				}
			}

			if (buffer[p] != 0xFF9E && buffer[p] != 0xFF9F && buffer[p] != 0x0301) buffer[unibuf++] = hashtab[buffer[p]];
		}

		return unibuf;
	}
}
