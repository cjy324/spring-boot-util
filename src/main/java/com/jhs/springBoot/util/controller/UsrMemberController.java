package com.jhs.springBoot.util.controller;

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

	@GetMapping("/usr/member/goKakaoLoginPage")
	public String goKakaoLoginPage() {

		String url = kakaoRestService.getKakaoLoginPageUrl();

		return "redirect:" + url;
	}

	@RequestMapping("/usr/member/doSendSelfKakaoMessage")
	@ResponseBody
	public ResultData doSendKakaoMessage(HttpServletRequest req, String msg, String linkBtnName, String webUrl,
			String mobileUrl) {
		int loginedMemberId = (int) req.getAttribute("loginedMemberId");

		return kakaoRestService.doSendSelfKakaoMessage(loginedMemberId, msg, linkBtnName, webUrl, mobileUrl);
	}

	@GetMapping("/usr/member/doLoginByKakoRest")
	public String doLoginByKakoRest(HttpServletRequest req, HttpSession session, String code) {
		KapiKakaoCom__v2_user_me__ResponseBody kakaoUser = kakaoRestService.getKakaoUserByAuthorizeCode(code);

		Member member = memberService.getMemberByOnLoginProviderMemberId("kakaoRest", kakaoUser.id);

		ResultData rd = null;

		if (member != null) {
			rd = memberService.updateMember(member, kakaoUser);
		} else {
			rd = memberService.join(kakaoUser);
		}

		String accessToken = kakaoUser.kauthKakaoCom__oauth_token__ResponseBody.access_token;
		String refreshToken = kakaoUser.kauthKakaoCom__oauth_token__ResponseBody.refresh_token;

		int id = (int) rd.getBody().get("id");

		memberService.updateToken(id, MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__access_token, accessToken);
		memberService.updateToken(id, MemberService.AttrKey__Type2Code.kauthKakaoCom__oauth_token__refresh_token, refreshToken);

		session.setAttribute("loginedMemberId", id);
		
		//엑세스토큰 만료시 리프레시 토큰으로 대체 가능하게????


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
}
