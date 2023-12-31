package com.cos.jwt.config.jwt;

import java.io.IOException;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.cos.jwt.config.auth.PrincipalDetails;
import com.cos.jwt.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

// 스프링 시큐리티에서 제공하는 UsernamePasswordAuthenticationFilter
// UsernamePasswordAuthenticationFilter 필터는 원래  "/login" 요청과 함께 POST메소드로 username, password가 오면 동작하는 시큐리티 필터임
// 하지만 현재는 SecurityConfig에서 formLogin을 disable해버렸기 때문에 "/login"요청과 함께 인증정보(ID,PW)가 POST로 전송되어도 동작하지 않음 => 404 error

// 우리는 jwt를 이용하므로 formLogin은 필요없지만, UsernamePasswordAuthenticationFilter는 필요함!
// 그래서 SecurityConfig에 해당 필터를 등록해줄 것임

// 등록해주고나면 로그인 default url인 "/login"요청시, UsernamePasswordAuthenticationFilter를 상속받은 JwtAuthenticationFilter필터가 동작할 것이고,
// 자동적으로 attemptAuthentication함수가 실행된다.
// 그러므로 attemptAuthentication함수에서 우리는 로그인 처리를 해주면 됨
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter{
	
	private final AuthenticationManager authenticationManager;

	// /login 요청을 하면 로그인 시도를 위해서 실행되는 함수
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		System.out.println("JwtAuthenticationFilter : 로그인 시도중");
		
		// 1. username, password 받아서
		try {
//			BufferedReader br = request.getReader();
//			
//			String input = null;
//			while((input = br.readLine()) != null) {
//				System.out.println(input);
//			}
			
			ObjectMapper om = new ObjectMapper(); // JSON데이터를 파싱해줌
			User user = om.readValue(request.getInputStream(), User.class);
			System.out.println(user);
			
			
			// 2. 정상인지 로그인 시도 해보기. 
			// authenticationManager로 로그인 시도를 하면 PrincipalDetailsService가 호출되어 loadUserByUsername()함수가 실행된다.
			// loadUserByUsername()함수가 실행되어 로그인 정보가 정확하면, 반환된 PrincipalDetails객체(로그인한 유저정보)는 authentication변수에 담긴다.
			UsernamePasswordAuthenticationToken authenticationToken =
					new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
			
			Authentication authentication = 
					authenticationManager.authenticate(authenticationToken);
			
			
			
			// 3. authentication변수가 가진 Principal객체를 꺼내어 principalDetails변수에 담아준다.
			PrincipalDetails principalDetails = (PrincipalDetails)authentication.getPrincipal();
			
			
			
			// 4. principalDetails변수에 값이 담겨있다면 로그인이 정상적으로 이루어졌다는 뜻.
			System.out.println("로그인 완료됨 : " + principalDetails.getUser().getUsername());

			
			
			// 5. authentication객체를 return해주면 시큐리티 세션영역에 저장됨.
			// 시큐리티 세션영역에 저장해주는 이유는 권한 관리를 security가 대신 해주기 때문에 편리하기 때문.
			// JWT토큰을 사용하면서 시큐리티 세션을 만들 이유는 없음. 단지 권한처리를 편리하게 하기 위해 security session에 저장하는 것이므로 생략가능함
			return authentication;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	// attemptAuthentication 실행 후 인증이 정상적으로 되었으면 successfulAuthentcation 함수가 실행
	// 6. JWT토큰을 만들어서 request요청한 사용자에게 JWT 토큰을 response해주면 됨
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		PrincipalDetails principalDetails = (PrincipalDetails)authResult.getPrincipal();
		
		/*
		// JWT라는 라이브러리 이용(pom.xml에 java-jwt Dependency) -> JWT토큰 생성
		String jwtToken = JWT.create() // builder패턴
				.withSubject("cos토큰") // 토큰 이름
				.withExpiresAt(new Date(System.currentTimeMillis()+(60000*10))) // 만료시간 : 토큰이 언제까지 유효할지, 10분으로 설정
				.withClaim("id", principalDetails.getUser().getId()) // withClaim은 비공개 claim으로, 토큰에 넣고 싶은 key-value값을 자유롭게 작성가능. (정해진 규칙 없음)
				.withClaim("username", principalDetails.getUser().getUsername())
				.sign(Algorithm.HMAC512("cos")); // 내 서버만 아는 고유한 값, SecretKey! 
												 // HMAC512 : RSA방식 아니고 Hash암호방식, 특징은 내 서버만 아는 secret값이 필수
												   
		// 응답헤더 Authorization키 값에다 Bearer방식임을 명시하고, jwtToken을 담아 전송("Bearer" 다음 한칸 띄어쓰기 필수 !)										  
		response.addHeader("Authorization", "Bearer " + jwtToken);
		*/
		
		String jwtToken = JWT.create()
				.withSubject("cos토큰")
				.withExpiresAt(new Date(System.currentTimeMillis()+ JwtProperties.EXPIRATION_TIME))
				.withClaim("id", principalDetails.getUser().getId())
				.withClaim("username", principalDetails.getUser().getUsername())
				.sign(Algorithm.HMAC512(JwtProperties.SECRET));
		
		
		response.addHeader(JwtProperties.HEADER_STRING, JwtProperties.TOKEN_PREFIX + jwtToken);
	}
}
