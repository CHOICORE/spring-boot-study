package com.corelab.blog.config;

import com.corelab.blog.service.PrincipalDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity // 스프링 시큐리티 필터 추가
//@EnableGlobalMethodSecurity(prePostEnabled = true) // 특정 주소로 접근하면 권한 및 인증 선행 체크
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final String idForEncode = "bcrypt";

    @Autowired
    private PrincipalDetailsService principalDetailsService;

    @Value(value = "${spring.profiles.active ?:local}")
    private String profiles;

    @Bean
    public PasswordEncoder passwordEncoder() {

        /* Create Custom DelegatingPasswordEncoder
        Map encoders = new HashMap<>();
        encoders.put("bycrypt", new BCryptPasswordEncoder());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
        encoders.put("scrypt", new SCryptPasswordEncoder());
        encoders.put("sha256", new StandardPasswordEncoder());
        return new DelegatingPasswordEncoder(idForEncode, encoders);
        */
        /* default - {bcrypt}encodedPassword */
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        System.out.println(isLocalEnvironment());
        if (isLocalEnvironment()) {
            /*
             로컬환경에서 개발할 때 csrf disabled
             h2-console - frameOptions disabled() or sameOrigin()
             */
            setLocalAuthorityConfigure(http);
        } else {
            // 운영 시 필요한 전략
            setOperationAuthorityConfigure(http);
        }
    }


    private boolean isLocalEnvironment() {
        // // TODO: 프로파일을 구분하는 함수 - ENUM TYPE으로 변경
        return profiles.contains("local") || profiles.contains("dev");
    }

    private void setLocalAuthorityConfigure(HttpSecurity http) throws Exception {
        http.headers()
                .frameOptions().sameOrigin()
                .and()
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/h2-console/**").permitAll();

        this.setOperationAuthorityConfigure(http);
    }

    private void setOperationAuthorityConfigure(HttpSecurity http) throws Exception {

        http.authorizeRequests().antMatchers("/user/**").authenticated()
                // PrincipalDetailsService 에서 Authority 를 가져와서 확인할 때 자동으로 ROLE 이라는 접두어를 붙어서 확인한다.
                .antMatchers("/manager/**").access("hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER')")
                .antMatchers("/admin/**").access("hasRole('ROLE_ADMIN')")
                .antMatchers("/hello/**", "/account/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin().loginPage("/account/login")
                .loginProcessingUrl("/account/login")
                .defaultSuccessUrl("/");
    }

}
