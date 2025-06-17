package com.kobe.loginandlogoutjwt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateAccessTokenResponse {
	private String accessToken;
}
