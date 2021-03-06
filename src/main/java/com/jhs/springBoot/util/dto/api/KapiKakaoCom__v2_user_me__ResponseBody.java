package com.jhs.springBoot.util.dto.api;

public class KapiKakaoCom__v2_user_me__ResponseBody {
	public int id;
	public Properties properties;
	public KakaoAccount kakao_account;
	
	// 이 정보는 서버에서 날라오는 정보는 아닙니다.
	public KauthKakaoCom__oauth_token__ResponseBody kauthKakaoCom__oauth_token__ResponseBody;
	
	public static class Properties {
		public String nickname;
	}
	
	public static class KakaoAccount {
		public String nickname;
		public Profile profile;
		public boolean has_email;
		public boolean email_needs_agreement;
		public boolean is_email_valid;
		public boolean is_email_verified;
		public String email;
		public boolean has_age_range;
		public boolean age_range_needs_agreement;
		public String age_range; // EX : 30~39
		public boolean has_birthday;
		public boolean birthday_needs_agreement;
		public String birthday; // EX : 0404
		public String birthday_type; // SOLAR or ...
		public boolean has_gender;
		public boolean gender_needs_agreement;
		public String gender; // mail or ...
		
		public static class Profile {
			public String nickname;
		}
	}
}
