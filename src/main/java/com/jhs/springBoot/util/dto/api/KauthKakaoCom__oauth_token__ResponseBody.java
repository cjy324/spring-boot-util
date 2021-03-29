package com.jhs.springBoot.util.dto.api;

public class KauthKakaoCom__oauth_token__ResponseBody {
	public String access_token;
	public String refresh_token;
	
	// 테스트 추가
	public int expires_in; //액세스 토큰 만료 시간(초) //최초: 21599초(약6시간)
	public int refresh_token_expires_in; //리프레시 토큰 만료 시간(초) //최초: 5183999초(약60일)
	public String scope; //인증된 사용자의 정보 조회 권한 범위 //범위가 여러 개일 경우, 공백으로 구분
}
