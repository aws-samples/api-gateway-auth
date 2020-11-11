package com.auth;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;

public class CustomLambdaAuth implements RequestHandler<APIGatewayV2CustomAuthorizerEvent, Object> {
    private static final String SECRET_TOKEN = "SECRET_TOKEN";

    @Override
    public Object handleRequest(final APIGatewayV2CustomAuthorizerEvent input,
                                final Context context) {
        context.getLogger().log(input.toString());

        Map<String, String> response = new HashMap<>();
        response.put("isAuthorized", "false");

        String authHeaderValue = input.getHeaders().getOrDefault("authorization", "");

        if (SECRET_TOKEN.equals(authHeaderValue)) {
            response.put("isAuthorized", "true");
        }

        return response;
    }
}
