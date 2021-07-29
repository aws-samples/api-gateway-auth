package com.auth;

import java.util.HashMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, APIGatewayV2HTTPResponse> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public APIGatewayV2HTTPResponse handleRequest(final Object input, final Context context) {
        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withIsBase64Encoded(false)
                    .withBody(OBJECT_MAPPER.writeValueAsString(input))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
