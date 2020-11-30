package com.auth;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import org.json.JSONObject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static java.util.Collections.emptyList;

public class TrustedStoreCustomResourceHandler implements RequestHandler<CloudFormationCustomResourceEvent, Object> {
    private static final S3Client S3_CLIENT = S3Client.create();

    @Override
    public Object handleRequest(CloudFormationCustomResourceEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Input: " + input);

        final String requestType = input.getRequestType();

        ExecutorService service = Executors.newSingleThreadExecutor();
        JSONObject responseData = new JSONObject();
        String bucket = (String) input.getResourceProperties().getOrDefault("TrustStoreBucket", "");
        String key = (String) input.getResourceProperties().getOrDefault("TrustStoreKey", "");
        String concatenatedCert = String.join("\n", ((List<String>) input.getResourceProperties().getOrDefault("Certs", emptyList())));

        try {
            if (requestType == null | concatenatedCert.isEmpty()) {
                throw new RuntimeException();
            }

            Runnable r = () -> {
                switch (requestType) {
                    case "Create": {
                        logger.log("CREATE!");

                        responseData.put("Message", "Resource creation successful!");
                        PutObjectResponse putObjectResponse = S3_CLIENT.putObject(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(), RequestBody.fromString(concatenatedCert));
                        context.getLogger().log(putObjectResponse.toString());
                        responseData.put("TrustStoreUri", String.format("s3://%s/%s", bucket, key));
                        responseData.put("ObjectVersion", putObjectResponse.versionId());
                        sendResponse(input, context, "SUCCESS", responseData);
                        break;
                    }

                    case "Update": {
                        logger.log("UPDATE!");

                        PutObjectResponse putObjectResponse = S3_CLIENT.putObject(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(), RequestBody.fromString(concatenatedCert));

                        context.getLogger().log(putObjectResponse.toString());
                        responseData.put("Message", "Resource update successful!");
                        responseData.put("TrustStoreUri", String.format("s3://%s/%s", bucket, key));
                        responseData.put("ObjectVersion", putObjectResponse.versionId());
                        sendResponse(input, context, "SUCCESS", responseData);
                        break;
                    }

                    case "Delete": {
                        logger.log("DELETE!");
                        sendResponse(input, context, "SUCCESS", responseData);
                        break;
                    }

                    default: {
                        logger.log("FAILURE!");
                        sendResponse(input, context, "FAILED", responseData);
                    }
                }
            };
            Future<?> f = service.submit(r);
            f.get(context.getRemainingTimeInMillis() - 1000, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException | InterruptedException
                | ExecutionException e) {
            logger.log("FAILURE!");
            sendResponse(input, context, "FAILED", responseData);
            // Took too long!
        } finally {
            service.shutdown();
        }
        return null;
    }

    /**
     * Send a response to CloudFormation regarding progress in creating resource.
     */
    public final Object sendResponse(
            final CloudFormationCustomResourceEvent input,
            final Context context,
            final String responseStatus,
            JSONObject responseData) {

        String responseUrl = input.getResponseUrl();
        context.getLogger().log("ResponseURL: " + responseUrl);

        URL url;
        try {
            url = new URL(responseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");

            JSONObject responseBody = new JSONObject();
            responseBody.put("Status", responseStatus);
            responseBody.put("PhysicalResourceId", context.getLogStreamName());
            responseBody.put("StackId", input.getStackId());
            responseBody.put("RequestId", input.getRequestId());
            responseBody.put("LogicalResourceId", input.getLogicalResourceId());
            responseBody.put("Data", responseData);

            OutputStreamWriter response = new OutputStreamWriter(connection.getOutputStream());
            response.write(responseBody.toString());
            response.close();
            context.getLogger().log("Response Code: " + connection.getResponseCode());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
