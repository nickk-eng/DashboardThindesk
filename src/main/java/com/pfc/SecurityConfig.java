package com.pfc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfc.thindesk.api.ApiResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .requestCache(cache -> cache.disable())
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                mapper.writeValue(response.getWriter(), ApiResponse.error(401, "Autenticação necessária. Entre novamente no sistema."));
            }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.error(401, "Autenticação necessária. Entre novamente no sistema."));
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.error(403, "Acesso não permitido."));
                }));
        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll() // Permitir acesso à página de login e arquivos estáticos
                .anyRequest().authenticated() // Exigir autenticação para qualquer outra página
            )
            .formLogin(form -> form
                .loginPage("/login") // Definir a URL da página de login personalizada
                .defaultSuccessUrl("/", true) // Redirecionar para /home após login bem-sucedido
                .permitAll() // Permitir que todos acessem a página de login
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout") // Redirecionar para o login após logout
            );
        return http.build();
    }
}
