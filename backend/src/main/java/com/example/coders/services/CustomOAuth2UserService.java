package com.example.coders.services;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if ("github".equals(registrationId)) {
            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());

            if (attributes.get("email") == null) {
                String token = userRequest.getAccessToken().getTokenValue();

                List<Map<String, Object>> emails = fetchGithubEmails(token);
                if (emails != null) {
                    String primaryEmail = emails.stream()
                            .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                            .map(e -> (String) e.get("email"))
                            .findFirst()
                            .orElse(null);

                    if (primaryEmail != null) {
                        attributes.put("email", primaryEmail);
                    }
                }
            }

            return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "login");
        }

        return oAuth2User;
    }

    private List<Map<String, Object>> fetchGithubEmails(String token) {
        RestTemplate restTemplate = new RestTemplate();
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "token " + token);
        headers.set("Accept", "application/vnd.github.v3+json");

        var entity = new org.springframework.http.HttpEntity<String>(headers);
        var response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                org.springframework.http.HttpMethod.GET,
                entity,
                List.class
        );

        return response.getBody();
    }
}
