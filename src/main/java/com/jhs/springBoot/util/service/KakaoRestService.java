package com.jhs.springBoot.util.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jhs.springBoot.util.dto.ResultData;
import com.jhs.springBoot.util.dto.api.KapiKakaoCom__v2_api_talk_memo_default_send__RequestBody__object_type_text;
import com.jhs.springBoot.util.dto.api.KapiKakaoCom__v2_api_talk_memo_default_send__ResponseBody;
import com.jhs.springBoot.util.dto.api.KapiKakaoCom__v2_user_me__ResponseBody;
import com.jhs.springBoot.util.dto.api.KauthKakaoCom__oauth_token__ResponseBody;
import com.jhs.springBoot.util.util.Util;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KakaoRestService {
	@Autowired
	private RestTemplateBuilder restTemplateBuilder;
	@Autowired
	private MemberService memberService;

	// application.yml에 설정해둔 custom으로부터 값 가져오기
	@Value("${custom.kakaoRest.apiKey}")
	private String kakaoRestApiKey;
	@Value("${custom.kakaoRest.redirectUrl}")
	private String kakaoRestRedirectUrl;

	public void getAccessToken(String kakaoAppKeyRestApi) {
	}

	public String getKakaoLoginPageUrl() {
		StringBuilder sb = new StringBuilder();
		sb.append("https://kauth.kakao.com/oauth/authorize");
		sb.append("?client_id=" + kakaoRestApiKey);
		sb.append("&redirect_uri=" + Util.getUrlEncoded(kakaoRestRedirectUrl));
		sb.append("&response_type=code");

		return sb.toString();
	}

	// 인증코드를 통해 엑세스토큰 받기
	public KapiKakaoCom__v2_user_me__ResponseBody getKakaoUserByAuthorizeCode(String authorizeCode) {
		RestTemplate restTemplate = restTemplateBuilder.build();

		Map<String, String> params = Util.getNewMapStringString();
		params.put("grant_type", "authorization_code");
		params.put("client_id", kakaoRestApiKey);
		params.put("redirect_uri", kakaoRestRedirectUrl);
		params.put("code", authorizeCode);

		KauthKakaoCom__oauth_token__ResponseBody respoonseBodyRs = Util
				.getHttpPostResponseBody(new ParameterizedTypeReference<KauthKakaoCom__oauth_token__ResponseBody>() {
				}, restTemplate, "https://kauth.kakao.com/oauth/token", params, null);

		/* 테스트 시작 - expires_in, refresh_token_expires_in 값 확인 */
		System.out.println("테스트-엑세스토큰 만료시간(초) : " + respoonseBodyRs.expires_in);
		System.out.println("테스트-리프레시토큰 만료시간(초) : " + respoonseBodyRs.refresh_token_expires_in);
		System.out.println("테스트-설정 권한 항목 : " + respoonseBodyRs.scope);
		/* 테스트 끝 - expires_in, refresh_token_expires_in 값 확인 */

		// 아래 함수로 전달
		return getKakaoUserByAccessToken(respoonseBodyRs);
	}

	// 엑세스토큰으로 유저정보 받아오기
	public KapiKakaoCom__v2_user_me__ResponseBody getKakaoUserByAccessToken(
			KauthKakaoCom__oauth_token__ResponseBody kauthKakaoCom__oauth_token__ResponseBody) {
		RestTemplate restTemplate = restTemplateBuilder.build();

		Map<String, String> headerParams = new HashMap<>();
		headerParams.put("Authorization", "Bearer " + kauthKakaoCom__oauth_token__ResponseBody.access_token);

		KapiKakaoCom__v2_user_me__ResponseBody respoonseBody = Util
				.getHttpPostResponseBody(new ParameterizedTypeReference<KapiKakaoCom__v2_user_me__ResponseBody>() {
				}, restTemplate, "https://kapi.kakao.com/v2/user/me", null, headerParams);

		respoonseBody.kauthKakaoCom__oauth_token__ResponseBody = kauthKakaoCom__oauth_token__ResponseBody;

		// 유저정보 리턴
		return respoonseBody;
	}

	// 나에게 메시지 보내기
	public ResultData doSendSelfKakaoMessage(int actorId, String msg, String linkBtnName, String webLink,
			String mobileLink) {
		String accessToken = memberService.getToken(actorId,
				MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__access_token);

		
		/* 테스트 - 엑세스토큰 유효기간 가져와 시간 비교 후 필요 시 엑세스토큰 갱신 시작 */

		// DB에 저장되어있는 엑세스토큰 유효기간 가져오기
		String accessTokenExpireDate = memberService.getTokenExpireDate(actorId,
				MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__access_token, accessToken);
		// 오늘 날짜 가져오기
		String todayDate = Util.getTodayDate();
		// 오늘 날짜와 엑세스토큰 유효기간 비교
		int compareResult = todayDate.compareTo(accessTokenExpireDate);

		// compareResult 값이 0보다 크거나 같으면 기간 만료라는 의미
		System.out.println("accessTokenExpireDate : " + accessTokenExpireDate);
		System.out.println("todayDate : " + todayDate);
		System.out.println("compare result : " + compareResult);

		// 기존 엑세스토큰(필요시 리프레시토큰) 값 갱신
		if (compareResult >= 0) {
			String refreshToken = memberService.getToken(actorId,
					MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__refresh_token);
			KauthKakaoCom__oauth_token__ResponseBody respoonseBodyRs = getKakaoAccessTokenByRefreshToken(refreshToken);

			/// 새로 갱신된 엑세스토큰 값으로 변경
			accessToken = respoonseBodyRs.access_token;
			/// respoonseBodyRs안에 들어 있는 초단위 유효기간 가져와 날짜 단위로 환산
			long originAccessToken_expires_in = respoonseBodyRs.expires_in;
			String accessToken_expires_in = Util.getTokenExpiresInToDateTime(originAccessToken_expires_in);
			// 기존 DB attr테이블 내 엑세스토큰 정보 업데이트
			memberService.updateToken(actorId,
					MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__access_token, accessToken,
					accessToken_expires_in);

			/// 기존 리프레시토큰의 유효기간이 1개월 미만인 경우에 리프레시토큰 값도 새로 들어옵니다.
			/// 기존 리프레시토큰의 유효기간이 1개월 이상 남은 경우 리프레시토큰은 그대로 유지됨(null값이 들어옴)
			/// 따라서, null이 아닌 경우에만 리프레시토큰 값 갱신
			if (respoonseBodyRs.refresh_token != null) {
				refreshToken = respoonseBodyRs.refresh_token;

				long originRefreshToken_expires_in = respoonseBodyRs.refresh_token_expires_in;
				String refreshToken_expires_in = Util.getTokenExpiresInToDateTime(originRefreshToken_expires_in);
				memberService.updateToken(actorId,
						MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__refresh_token, refreshToken,
						refreshToken_expires_in);
				
				System.out.println("리프레시토큰 갱신 완료");
				System.out.println("newRefreshToken : " + refreshToken);
				System.out.println("newRefreshToken_expires_in : " + refreshToken_expires_in);
			}

			System.out.println("엑세스토큰 갱신 완료");
			System.out.println("newAccessToken : " + accessToken);
			System.out.println("newAccessToken_expires_in : " + accessToken_expires_in);
		}

		/* 테스트 - 엑세스토큰 유효기간 가져와 시간 비교 후 필요 시 엑세스토큰 갱신 끝 */
		
		

		// 마찬가지로 access_Token값을 가져와 access_Token값을 통해 로그인되어있는 사용자를 확인
		String reqURL = KapiKakaoCom__v2_api_talk_memo_default_send__RequestBody__object_type_text.API_URL;

		Map<String, String> headerParams = new HashMap<>();
		headerParams.put("Authorization", "Bearer " + accessToken);

		Map<String, String> params = KapiKakaoCom__v2_api_talk_memo_default_send__RequestBody__object_type_text
				.make(msg, linkBtnName, webLink, mobileLink);

		RestTemplate restTemplate = restTemplateBuilder.build();
		KapiKakaoCom__v2_api_talk_memo_default_send__ResponseBody kapiKakaoCom__v2_api_talk_memo_default_send__ResponseBody = Util
				.getHttpPostResponseBody(
						new ParameterizedTypeReference<KapiKakaoCom__v2_api_talk_memo_default_send__ResponseBody>() {
						}, restTemplate, reqURL, params, headerParams);

		return new ResultData("S-1", "성공하였습니다.", "kapiKakaoCom__v2_api_talk_memo_default_send__ResponseBody",
				kapiKakaoCom__v2_api_talk_memo_default_send__ResponseBody);
	}

	// 테스트 - 기존 리프레시토큰으로 엑세스토큰 새로 갱신
	public KauthKakaoCom__oauth_token__ResponseBody getKakaoAccessTokenByRefreshToken(String refreshToken) {
		RestTemplate restTemplate = restTemplateBuilder.build();

		Map<String, String> params = Util.getNewMapStringString();
		params.put("grant_type", "refresh_token");
		params.put("client_id", kakaoRestApiKey);
		params.put("refresh_token", refreshToken);

		KauthKakaoCom__oauth_token__ResponseBody respoonseBodyRs = Util
				.getHttpPostResponseBody(new ParameterizedTypeReference<KauthKakaoCom__oauth_token__ResponseBody>() {
				}, restTemplate, "https://kauth.kakao.com/oauth/token", params, null);

		return respoonseBodyRs;
	}

}
