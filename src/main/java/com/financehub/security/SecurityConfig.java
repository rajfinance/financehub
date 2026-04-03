package com.financehub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/error").permitAll()
						.requestMatchers("/login", "/signup", "/forgotPassword", "/password-reset/confirm").permitAll()
						.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
						.requestMatchers("/api/perform_signup").permitAll()
						.requestMatchers("/api/password-reset/request", "/api/password-reset/complete").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/perform_login", "/api/calculate").permitAll()
				.requestMatchers("/account/**").authenticated()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll())
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/api/perform_login")
						.usernameParameter("username")
						.passwordParameter("password")
						.defaultSuccessUrl("/api/home", true)
						.failureUrl("/login?error")
						.permitAll())
				.logout(logout -> logout
						.logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", "POST"))
						.logoutSuccessUrl("/login?logout")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.permitAll());

		return http.build();
	}
}
