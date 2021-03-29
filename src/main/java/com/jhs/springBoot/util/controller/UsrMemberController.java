package com.jhs.springBoot.util.controller;

import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jhs.springBoot.util.dto.Member;
import com.jhs.springBoot.util.dto.ResultData;
import com.jhs.springBoot.util.dto.api.KapiKakaoCom__v2_user_me__ResponseBody;
import com.jhs.springBoot.util.service.KakaoRestService;
import com.jhs.springBoot.util.service.MemberService;
import com.jhs.springBoot.util.util.Util;

@Controller
public class UsrMemberController extends BaseController {
	@Value("${custom.kakaoRest.apiKey}")
	private String kakaoRestApiKey;

	@Autowired
	private MemberService memberService;

	@Autowired
	private KakaoRestService kakaoRestService;

	@GetMapping("/usr/member/login")
	public String showLogin() {
		return "usr/member/login";
	}

	//인증코드 요청
	@GetMapping("/usr/member/goKakaoLoginPage")
	public String goKakaoLoginPage() {

		String url = kakaoRestService.getKakaoLoginPageUrl();

		//url을 받아서 바로 이동
		return "redirect:" + url;
	}
	
	//엑세스,리프레시토큰 발급 후 유저정보까지 받아오기
	@GetMapping("/usr/member/doLoginByKakoRest")
	public String doLoginByKakoRest(HttpServletRequest req, HttpSession session, String code) {
		//엑세스,리프레시토큰 발급 후 유저정보까지 한번에 받아오는 로직 수행
		KapiKakaoCom__v2_user_me__ResponseBody kakaoUser = kakaoRestService.getKakaoUserByAuthorizeCode(code);

		//DB에서 카카오 로그인한 회원의 정보 가져오기(조회하기)
		Member member = memberService.getMemberByOnLoginProviderMemberId("kakaoRest", kakaoUser.id);

		ResultData rd = null;

		//만약, 기존 회원정보 있으면
		if (member != null) {
			//카카오 유저정보로 기존 회원정보 업데이트
			rd = memberService.updateMember(member, kakaoUser);
		} else {
			//만약, 기존 회원정보 없으면 회원가입 실시
			rd = memberService.join(kakaoUser);
		}

		String accessToken = kakaoUser.kauthKakaoCom__oauth_token__ResponseBody.access_token;
		String refreshToken = kakaoUser.kauthKakaoCom__oauth_token__ResponseBody.refresh_token;

		int id = (int) rd.getBody().get("id");
		
		
		/* 테스트 - 엑세스,리프레시토큰 만료기간 가져오기 시작 */
		
		long originAccessToken_expires_in = kakaoUser.kauthKakaoCom__oauth_token__ResponseBody.expires_in;
		long originRefreshToken_expires_in = kakaoUser.kauthKakaoCom__oauth_token__ResponseBody.refresh_token_expires_in;

		String accessToken_expires_in = Util.getTokenExpiresInToDateTime(originAccessToken_expires_in);
		String refreshToken_expires_in = Util.getTokenExpiresInToDateTime(originRefreshToken_expires_in);
		
		/* 테스트 - 엑세스,리프레시토큰 만료기간 가져오기 끝 */


		
		//엑세스,리프레시토큰 정보 DB attr테이블에 저장(테스트 - 만료시간을 expireDate에 저장)
		memberService.updateToken(id, MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__access_token, accessToken, accessToken_expires_in);
		memberService.updateToken(id, MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__refresh_token, refreshToken, refreshToken_expires_in);

		session.setAttribute("loginedMemberId", id);

		return msgAndReplace(req, "카카오톡 계정으로 로그인하였습니다.", "../home/main");
	}

	@GetMapping("/usr/member/doLogout")
	public String doLogout(HttpServletRequest req, HttpSession session) {
		int id = -1;
		if (session.getAttribute("loginedMemberId") != null) {
			id = (int) session.getAttribute("loginedMemberId");
			session.removeAttribute("loginedMemberId");
		}

		return msgAndReplace(req, "로그아웃 되었습니다.", "../home/main");
	}
	
	@RequestMapping("/usr/member/doSendSelfKakaoMessage")
	@ResponseBody
	public ResultData doSendKakaoMessage(HttpServletRequest req, String msg, String linkBtnName, String webUrl,
			String mobileUrl) {
		int loginedMemberId = (int) req.getAttribute("loginedMemberId");

		return kakaoRestService.doSendSelfKakaoMessage(loginedMemberId, msg, linkBtnName, webUrl, mobileUrl);
	}

}
