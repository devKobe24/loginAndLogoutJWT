package com.kobe.loginandlogoutjwt.service;

import com.kobe.loginandlogoutjwt.domain.RefreshToken;
import com.kobe.loginandlogoutjwt.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
	private final RefreshTokenRepository refreshTokenRepository;

	public RefreshToken findByRefreshToken(String refreshToken) {
		return refreshTokenRepository.findByRefreshToken(refreshToken)
			.orElseThrow(() -> new IllegalArgumentException("Unexpected token"));
	}
}
