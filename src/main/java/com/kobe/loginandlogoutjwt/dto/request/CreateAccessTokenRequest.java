package com.kobe.loginandlogoutjwt.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateAccessTokenRequest {
	private String refreshToken;
}
