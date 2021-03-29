package com.jhs.springBoot.util.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhs.springBoot.util.dto.api.Aligo__send__ResponseBody;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {
	private static String aligoUserId;
	private static String aligoApiKey;

	public static Aligo__send__ResponseBody sendSms(String from, String to, String msg) {
		return sendSms(from, to, msg, false);
	}

	public static Aligo__send__ResponseBody sendSms(String from, String to, String msg, boolean isTest) {
		try {

			final String encodingType = "utf-8";
			final String boundary = "____boundary____";

			/**************** 문자전송하기 예제 ******************/
			/* "result_code":결과코드,"message":결과문구, */
			/* "msg_id":메세지ID,"error_cnt":에러갯수,"success_cnt":성공갯수 */
			/*
			 * 동일내용 > 전송용 입니다. /******************** 인증정보
			 ********************/
			String sms_url = "https://apis.aligo.in/send/"; // 전송요청 URL

			Map<String, String> sms = new HashMap<String, String>();

			sms.put("user_id", aligoUserId); // SMS 아이디
			sms.put("key", aligoApiKey); // 인증키

			/******************** 인증정보 ********************/

			/******************** 전송정보 ********************/
			sms.put("msg", msg); // 메세지 내용
			sms.put("receiver", to); // 수신번호
			// sms.put("destination", "01111111111|담당자,01111111112|홍길동"); // 수신인 %고객명% 치환
			sms.put("sender", from); // 발신번호
			// sms.put("rdate", ""); // 예약일자 - 20161004 : 2016-10-04일기준
			// sms.put("rtime", ""); // 예약시간 - 1930 : 오후 7시30분
			sms.put("testmode_yn", isTest ? "Y" : "N"); // Y 인경우 실제문자 전송X , 자동취소(환불) 처리
			// sms.put("title", "제목입력"); // LMS, MMS 제목 (미입력시 본문중 44Byte 또는 엔터 구분자 첫라인)

			String image = "";
			// image = "/tmp/pic_57f358af08cf7_sms_.jpg"; // MMS 이미지 파일 위치

			/******************** 전송정보 ********************/

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();

			builder.setBoundary(boundary);
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setCharset(Charset.forName(encodingType));

			for (Iterator<String> i = sms.keySet().iterator(); i.hasNext();) {
				String key = i.next();
				builder.addTextBody(key, sms.get(key), ContentType.create("Multipart/related", encodingType));
			}

			File imageFile = new File(image);
			if (image != null && image.length() > 0 && imageFile.exists()) {

				builder.addPart("image", new FileBody(imageFile, ContentType.create("application/octet-stream"),
						URLEncoder.encode(imageFile.getName(), encodingType)));
			}

			org.apache.http.HttpEntity entity = (org.apache.http.HttpEntity) builder.build();

			HttpClient client = HttpClients.createDefault();
			HttpPost post = new HttpPost(sms_url);
			post.setEntity(entity);

			HttpResponse res = client.execute(post);

			String result = "";
			if (res != null) {
				BufferedReader in = new BufferedReader(
						new InputStreamReader(res.getEntity().getContent(), encodingType));
				String buffer = null;
				while ((buffer = in.readLine()) != null) {
					result += buffer;
				}
				in.close();
			}

			Aligo__send__ResponseBody rb = Util.getObjectFromJsonString(result, Aligo__send__ResponseBody.class);

			return rb;
		} catch (Exception e) {
			e.printStackTrace();
			Aligo__send__ResponseBody rb = new Aligo__send__ResponseBody();
			rb.message = e.getLocalizedMessage();
			rb.msg_id = "-1";
			rb.error_cnt = 1;
			return rb;
		}
	}

	public static String getNowDateStr() {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date time = new Date();
		return format1.format(time);
	}

	public static Map<String, Object> mapOf(Object... args) {
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException("인자를 짝수개 입력해주세요.");
		}

		int size = args.length / 2;

		Map<String, Object> map = new LinkedHashMap<>();

		for (int i = 0; i < size; i++) {
			int keyIndex = i * 2;
			int valueIndex = keyIndex + 1;

			String key;
			Object value;

			try {
				key = (String) args[keyIndex];
			} catch (ClassCastException e) {
				throw new IllegalArgumentException("키는 String으로 입력해야 합니다. " + e.getMessage());
			}

			value = args[valueIndex];

			map.put(key, value);
		}

		return map;
	}

	public static int getAsInt(Object object, int defaultValue) {
		if (object instanceof BigInteger) {
			return ((BigInteger) object).intValue();
		} else if (object instanceof Double) {
			return (int) Math.floor((double) object);
		} else if (object instanceof Float) {
			return (int) Math.floor((float) object);
		} else if (object instanceof Long) {
			return (int) object;
		} else if (object instanceof Integer) {
			return (int) object;
		} else if (object instanceof String) {
			return Integer.parseInt((String) object);
		}

		return defaultValue;
	}

	public static String msgAndBack(String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("<script>");
		sb.append("alert('" + msg + "');");
		sb.append("history.back();");
		sb.append("</script>");

		return sb.toString();
	}

	public static String msgAndReplace(String msg, String url) {
		StringBuilder sb = new StringBuilder();
		sb.append("<script>");
		sb.append("alert('" + msg + "');");
		sb.append("location.replace('" + url + "');");
		sb.append("</script>");

		return sb.toString();
	}

	public static String toJsonStr(Map<String, Object> param) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(param);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return "";
	}

	public static Map<String, Object> getParamMap(HttpServletRequest request) {
		Map<String, Object> param = new HashMap<>();

		Enumeration<String> parameterNames = request.getParameterNames();

		while (parameterNames.hasMoreElements()) {
			String paramName = parameterNames.nextElement();
			Object paramValue = request.getParameter(paramName);

			param.put(paramName, paramValue);
		}

		return param;
	}

	public static String getUrlEncoded(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return str;
		}
	}

	public static <T> T ifNull(T data, T defaultValue) {
		return data != null ? data : defaultValue;
	}

	public static <T> T reqAttr(HttpServletRequest req, String attrName, T defaultValue) {
		return (T) ifNull(req.getAttribute(attrName), defaultValue);
	}

	public static boolean isEmpty(Object data) {
		if (data == null) {
			return true;
		}

		if (data instanceof String) {
			String strData = (String) data;

			return strData.trim().length() == 0;
		} else if (data instanceof Integer) {
			Integer integerData = (Integer) data;

			return integerData != 0;
		} else if (data instanceof List) {
			List listData = (List) data;

			return listData.isEmpty();
		} else if (data instanceof Map) {
			Map mapData = (Map) data;

			return mapData.isEmpty();
		}

		return true;
	}

	public static <T> T ifEmpty(T data, T defaultValue) {
		if (isEmpty(data)) {
			return defaultValue;
		}

		return data;
	}

	public static String getFileExtTypeCodeFromFileName(String fileName) {
		String ext = getFileExtFromFileName(fileName).toLowerCase();

		switch (ext) {
		case "jpeg":
		case "jpg":
		case "gif":
		case "png":
			return "img";
		case "mp4":
		case "avi":
		case "mov":
			return "video";
		case "mp3":
			return "audio";
		}

		return "etc";
	}

	public static String getFileExtType2CodeFromFileName(String fileName) {
		String ext = getFileExtFromFileName(fileName).toLowerCase();

		switch (ext) {
		case "jpeg":
		case "jpg":
			return "jpg";
		case "gif":
			return ext;
		case "png":
			return ext;
		case "mp4":
			return ext;
		case "mov":
			return ext;
		case "avi":
			return ext;
		case "mp3":
			return ext;
		}

		return "etc";
	}

	public static String getFileExtFromFileName(String fileName) {
		int pos = fileName.lastIndexOf(".");
		String ext = fileName.substring(pos + 1);

		return ext;
	}

	public static String getNowYearMonthDateStr() {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy_MM");

		String dateStr = format1.format(System.currentTimeMillis());

		return dateStr;
	}

	public static List<Integer> getListDividedBy(String str, String divideBy) {
		return Arrays.asList(str.split(divideBy)).stream().map(s -> Integer.parseInt(s.trim()))
				.collect(Collectors.toList());
	}

	public static boolean delteFile(String filePath) {
		java.io.File ioFile = new java.io.File(filePath);
		if (ioFile.exists()) {
			return ioFile.delete();
		}

		return true;
	}

	public static String numberFormat(int num) {
		DecimalFormat df = new DecimalFormat("###,###,###");

		return df.format(num);
	}

	public static String numberFormat(String numStr) {
		return numberFormat(Integer.parseInt(numStr));
	}

	public static boolean allNumberString(String str) {
		if (str == null) {
			return false;
		}

		if (str.length() == 0) {
			return true;
		}

		for (int i = 0; i < str.length(); i++) {
			if (Character.isDigit(str.charAt(i)) == false) {
				return false;
			}
		}

		return true;
	}

	public static boolean startsWithNumberString(String str) {
		if (str == null) {
			return false;
		}

		if (str.length() == 0) {
			return false;
		}

		return Character.isDigit(str.charAt(0));
	}

	public static boolean isStandardLoginIdString(String str) {
		if (str == null) {
			return false;
		}

		if (str.length() == 0) {
			return false;
		}

		// 조건
		// 5자 이상, 20자 이하로 구성
		// 숫자로 시작 금지
		// _, 알파벳, 숫자로만 구성
		return Pattern.matches("^[a-zA-Z]{1}[a-zA-Z0-9_]{4,19}$", str);
	}

	public static <T> T getHttpPostResponseBody(ParameterizedTypeReference<T> responseType, RestTemplate restTemplate,
			String url, Map<String, String> params, Map<String, String> headerParams) {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		if (headerParams != null) {
			for (String key : headerParams.keySet()) {
				String headerValue = headerParams.get(key);
				httpHeaders.add(key, headerValue);
			}
		}

		MultiValueMap<String, String> newParams = Util.hashMapToLinkedMultiValueMap(params);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(newParams, httpHeaders);

		ResponseEntity<T> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
		return responseEntity.getBody();
	}

	private static MultiValueMap<String, String> hashMapToLinkedMultiValueMap(Map<String, String> origin) {
		if (origin == null) {
			return null;
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

		for (String key : origin.keySet()) {
			String value = (String) origin.get(key);
			map.add(key, value);
		}

		return map;
	}

	public static void changeMapKey(Map<String, Object> param, String oldKey, String newKey) {
		Object value = param.get(oldKey);
		param.remove(oldKey);
		param.put(newKey, value);
	}

	public static String getTempPassword(int length) {
		int index = 0;
		char[] charArr = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
				'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a',
				'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
				'w', 'x', 'y', 'z' };

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < length; i++) {
			index = (int) (charArr.length * Math.random());
			sb.append(charArr[index]);
		}

		return sb.toString();
	}

	public static String sha256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();

		} catch (Exception ex) {
			return "";
		}
	}

	public static <T> T getObjectFromJsonString(String rs, Class<T> cls) {
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return (T) objectMapper.readValue(rs, cls);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getJsonStringFromObject(Object o) {
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return objectMapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public static Map<String, String> getNewMapStringString() {
		return new LinkedHashMap<String, String>();
	}

	public static String getUUIDStr() {
		return UUID.randomUUID().toString();
	}

	public static void initAligo(String aligoUserId, String aligoApiKey) {
		Util.aligoUserId = aligoUserId;
		Util.aligoApiKey = aligoApiKey;
	}

	public static Map getMapFromObject(Object o) {
		// ObjectMapper
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.convertValue(o, Map.class);
	}
	
	//초단위인 토큰 유효기간을 날짜단위로 환산하는 유틸
	public static String getTokenExpiresInToDateTime(long originToken_expires_in) {
		// 현재 시간 초단위로 가져오기
		long currentTimeMillis = System.currentTimeMillis();
		// 현재 시간에 만료기간 더하기
		originToken_expires_in = currentTimeMillis + (originToken_expires_in * 1000);
		
		// 다시 날짜 단위로 환산
		SimpleDateFormat simpl = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String token_expires_in = simpl.format(originToken_expires_in);

		return token_expires_in;
	}

	// 오늘 날짜 계산 유틸
	public static String getTodayDate() {
		// 날짜 포멧 셋팅
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Calendar생성
		Calendar cal = Calendar.getInstance();
		// 오늘 날짜 가져오기
		Date date = new Date();
		// 오늘 날짜 셋팅
		cal.setTime(date);
		String todayDate = sdf.format(cal.getTime());

		return todayDate;
	}

}