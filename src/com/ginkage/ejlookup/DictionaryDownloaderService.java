package com.ginkage.ejlookup;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;

public class DictionaryDownloaderService extends DownloaderService {
	private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAic56HNIQyFeTOn6gjE+aYcZmfoV3Dya2lzE5SyZWMKKCQ80r04HPgd0k7SzIg8npvwzv5fH03ORZstW3gSDbc45NmRDNlIaMFC3vd+OfPGW1fT0zFjIwrPOapMtEGRkVpp9gexbzxEsPSW9t6eUrZ0q9J0tVcpB4+rw16jWL2GgBXirPTxF9SMqRmadgB61w1usPGi0aipb1SBSOEHzxiLFHzuDr587OHpEVYaoShDzjXeaBbXmr17OaviY37I6W3sV4Oz17alD2qlbYJk0J+Cv/94PCOk7BVjoz1U7UIGrD+Q0PDt4oHvuJ8iYrFuxmjEjoO1120GkBkjJ4F0xHuwIDAQAB";
	private static final byte[] SALT = new byte[] { 31, -41, 59, -21, 78, -21, 78, -2, 3, -14, 15, -92, 65, -35, 8, -2, 17, -82, 81, -78, 28 };

	@Override
	public String getPublicKey() {
		return BASE64_PUBLIC_KEY;
	}

	@Override
	public byte[] getSALT() {
		return SALT;
	}

	@Override
	public String getAlarmReceiverClassName() {
		return DictionaryAlarmReceiver.class.getName();
	}
}
