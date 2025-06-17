## 📜 License

This project is under a **Custom License** that prohibits commercial use.

See the [LICENSE](./LICENSE) file for full terms.  
For commercial licensing, please contact: **dev.skyachieve91@gmail.com**

------------------------------------------------------------------------

# Login and Logout JWT
## Diagram 📝
<img src = "https://github.com/devKobe24/images2/blob/main/loginAndLogoutJWT.png?raw=true">

## 기능
1️⃣ JWT</br>
2️⃣ 로그인/로그아웃</br>

### 1️⃣ TokenProviderTest.

<img src = "https://github.com/devKobe24/images2/blob/main/TokenProviderTest.png?raw=true">

**📝 CODE**
```java
package com.kobe.loginandlogoutjwt.config.jwt;

import com.kobe.loginandlogoutjwt.domain.User;
import com.kobe.loginandlogoutjwt.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class TokenProviderTest {

	@Autowired
	private TokenProvider tokenProvider;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtProperties jwtProperties;

	// generateToken() 검증 테스트
	@DisplayName("generateToken(): 유저 정보와 만료 기간을 전달해 토큰을 만들 수 있다.")
	@Test
	void generateToken() {
		// given
		User testUser = userRepository.save(User.builder()
			.email("test@gmail.com")
			.password("password")
			.build());

		// when
		String token = tokenProvider.generateToken(testUser, Duration.ofDays(14));

		// then
		Long userId = Jwts.parser()
			.setSigningKey(jwtProperties.getSecretKey())
			.parseClaimsJws(token)
			.getBody()
			.get("id", Long.class);

		assertThat(userId).isEqualTo(testUser.getId());
	}

	// validToken() 검증 테스트
	@DisplayName("validToken(): 만료된 토큰인 때에 유효성 검증에 실패한다.")
	@Test
	void validToken_invalidToken() {
		// given
		String token = JwtFactory.builder()
			.expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
			.build()
			.createToken(jwtProperties);

		// when
		boolean result = tokenProvider.validToken(token);

		// then
		assertThat(result).isFalse();
	}

	@DisplayName("validToken(): 유효한 토큰인 때에 우효성 검증에 성공한다.")
	@Test
	void validToken_validToken() {
		// given
		String token = JwtFactory.withDefaultValues().createToken(jwtProperties);

		// when
		boolean result = tokenProvider.validToken(token);

		// then
		assertThat(result).isTrue();
	}

	// getAuthentication() 검증 테스트
	@DisplayName("getAuthentication(): 토큰 기반으로 인증 정보를 가져올 수 있다.")
	@Test
	void getAuthentication() {
		// given
		String userEmail = "test@gmail.com";
		String token = JwtFactory.builder()
			.subject(userEmail)
			.build()
			.createToken(jwtProperties);

		// when
		Authentication authentication = tokenProvider.getAuthentication(token);

		// then
		assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);
	}

	// getUserId() 검증 테스트
	@DisplayName("getUserId(): 토큰으로 유저 ID를 가져올 수 있다.")
	@Test
	void getUserId() {
		// given
		Long userId = 1L;
		String token = JwtFactory.builder()
			.claims(Map.of("id", userId))
			.build()
			.createToken(jwtProperties);

		// when
		Long userIdByToken = tokenProvider.getUserId(token);

		// then
		assertThat(userIdByToken).isEqualTo(userId);
	}
}
```

### 2️⃣ TokenApiControllerTest

<img src = "https://github.com/devKobe24/images2/blob/main/TokenApiControllerTest.png?raw=true">

**📝 CODE**
```java
package com.kobe.loginandlogoutjwt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.loginandlogoutjwt.config.jwt.JwtFactory;
import com.kobe.loginandlogoutjwt.config.jwt.JwtProperties;
import com.kobe.loginandlogoutjwt.domain.RefreshToken;
import com.kobe.loginandlogoutjwt.domain.User;
import com.kobe.loginandlogoutjwt.dto.request.CreateAccessTokenRequest;
import com.kobe.loginandlogoutjwt.repository.RefreshTokenRepository;
import com.kobe.loginandlogoutjwt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TokenApiControllerTest {
	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	JwtProperties jwtProperties;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	public void mockMvcSetUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.build();
		userRepository.deleteAll();
	}

	@DisplayName("createNewAccessToken: 새로운 액세스 토큰을 발급한다.")
	@Test
	public void createNewAccessToken() throws Exception {
		// given
		final String url = "/api/token";

		User testUser = userRepository.save(User.builder()
			.email("user@gmail.com")
			.password("test")
			.build());

		String refreshToken = JwtFactory.builder()
			.claims(Map.of("id", testUser.getId()))
			.build()
			.createToken(jwtProperties);

		refreshTokenRepository.save(new RefreshToken(testUser.getId(), refreshToken));

		CreateAccessTokenRequest request = new CreateAccessTokenRequest();
		request.setRefreshToken(refreshToken);

		final String requestBody = objectMapper.writeValueAsString(request);

		// when
		ResultActions resultActions = mockMvc.perform(post(url)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.content(requestBody));

		// then
		resultActions
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}
}
```

## 활용 기술(Skills) 🛠️

1️⃣ JWT</br>
2️⃣ 토큰 기반 인증 기법</br>
3️⃣ 리프레시 토큰</br>
4️⃣ 토큰 필터</br>

## Login and Logout JWT 구조 🏗️
```
.
├── build
│   ├── classes
│   │   └── java
│   │       ├── main
│   │       │   └── com
│   │       │       └── kobe
│   │       │           └── loginandlogoutjwt
│   │       │               ├── config
│   │       │               │   ├── jwt
│   │       │               │   │   ├── JwtProperties.class
│   │       │               │   │   └── TokenProvider.class
│   │       │               │   ├── TokenAuthenticationFilter.class
│   │       │               │   └── WebSecurityConfig.class
│   │       │               ├── controller
│   │       │               │   └── TokenApiController.class
│   │       │               ├── domain
│   │       │               │   ├── RefreshToken.class
│   │       │               │   ├── User.class
│   │       │               │   └── User$UserBuilder.class
│   │       │               ├── dto
│   │       │               │   ├── request
│   │       │               │   │   ├── AddUserRequest.class
│   │       │               │   │   └── CreateAccessTokenRequest.class
│   │       │               │   └── response
│   │       │               │       └── CreateAccessTokenResponse.class
│   │       │               ├── LoginAndLogoutJwtApplication.class
│   │       │               ├── repository
│   │       │               │   ├── RefreshTokenRepository.class
│   │       │               │   └── UserRepository.class
│   │       │               └── service
│   │       │                   ├── RefreshTokenService.class
│   │       │                   ├── TokenService.class
│   │       │                   └── UserService.class
│   │       └── test
│   │           └── com
│   │               └── kobe
│   │                   └── loginandlogoutjwt
│   │                       ├── config
│   │                       │   └── jwt
│   │                       │       ├── JwtFactory.class
│   │                       │       ├── JwtFactory$JwtFactoryBuilder.class
│   │                       │       └── TokenProviderTest.class
│   │                       ├── controller
│   │                       │   └── TokenApiControllerTest.class
│   │                       └── LoginAndLogoutJwtApplicationTests.class
│   ├── generated
│   │   └── sources
│   │       ├── annotationProcessor
│   │       │   └── java
│   │       │       ├── main
│   │       │       └── test
│   │       └── headers
│   │           └── java
│   │               ├── main
│   │               └── test
│   ├── reports
│   │   └── tests
│   │       └── test
│   │           ├── classes
│   │           │   └── com.kobe.loginandlogoutjwt.controller.TokenApiControllerTest.html
│   │           ├── css
│   │           │   ├── base-style.css
│   │           │   └── style.css
│   │           ├── index.html
│   │           ├── js
│   │           │   └── report.js
│   │           └── packages
│   │               └── com.kobe.loginandlogoutjwt.controller.html
│   ├── resources
│   │   └── main
│   │       └── application.yml
│   ├── test-results
│   │   └── test
│   │       ├── binary
│   │       │   ├── output.bin
│   │       │   ├── output.bin.idx
│   │       │   └── results.bin
│   │       └── TEST-com.kobe.loginandlogoutjwt.controller.TokenApiControllerTest.xml
│   └── tmp
│       ├── compileJava
│       │   ├── compileTransaction
│       │   │   ├── backup-dir
│       │   │   └── stash-dir
│       │   └── previous-compilation-data.bin
│       ├── compileTestJava
│       │   ├── compileTransaction
│       │   │   ├── backup-dir
│       │   │   └── stash-dir
│       │   └── previous-compilation-data.bin
│       └── test
├── build.gradle
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── HELP.md
├── LICENSE
├── README.md
├── settings.gradle
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── kobe
    │   │           └── loginandlogoutjwt
    │   │               ├── config
    │   │               │   ├── jwt
    │   │               │   │   ├── JwtProperties.java
    │   │               │   │   └── TokenProvider.java
    │   │               │   ├── TokenAuthenticationFilter.java
    │   │               │   └── WebSecurityConfig.java
    │   │               ├── controller
    │   │               │   └── TokenApiController.java
    │   │               ├── domain
    │   │               │   ├── RefreshToken.java
    │   │               │   └── User.java
    │   │               ├── dto
    │   │               │   ├── request
    │   │               │   │   ├── AddUserRequest.java
    │   │               │   │   └── CreateAccessTokenRequest.java
    │   │               │   └── response
    │   │               │       └── CreateAccessTokenResponse.java
    │   │               ├── LoginAndLogoutJwtApplication.java
    │   │               ├── repository
    │   │               │   ├── RefreshTokenRepository.java
    │   │               │   └── UserRepository.java
    │   │               └── service
    │   │                   ├── RefreshTokenService.java
    │   │                   ├── TokenService.java
    │   │                   └── UserService.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java
            └── com
                └── kobe
                    └── loginandlogoutjwt
                        ├── config
                        │   └── jwt
                        │       ├── JwtFactory.java
                        │       └── TokenProviderTest.java
                        ├── controller
                        │   └── TokenApiControllerTest.java
                        └── LoginAndLogoutJwtApplicationTests.java
```
