package pt.ulisboa.tecnico.cnv;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LambdaInvoker {

    private final LambdaClient awsLambda;
    private final ObjectMapper mapper = new ObjectMapper();

    public LambdaInvoker() {
        awsLambda = LambdaClient.builder().credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
    }

    public CompletableFuture<WorkerResponse> invokeLambda(URI uri) {
        String game = uri.getPath().split("/")[0];
        Map<String, String> params = queryToMap(uri.getRawQuery());
        return CompletableFuture.supplyAsync(() -> {
            try {

                byte[] jsonPayload = mapper.writeValueAsBytes(params); // only params

                InvokeRequest request = InvokeRequest.builder()
                        .functionName(game) // game is the Lambda function name
                        .payload(SdkBytes.fromByteArray(jsonPayload))
                        .build();

                InvokeResponse response = awsLambda.invoke(request);

                if (response.statusCode() != 200) {
                    System.out.println("Error invoking Lambda: " + response.statusCode());
                }

                return response;
            } catch (Exception e) {
                throw new RuntimeException("Error invoking Lambda function: " + game, e);
            }
        }).thenApply(lambdaResponse -> new WorkerResponse(lambdaResponse.statusCode(), lambdaResponse.payload().asUtf8String()));
    }

    private Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }



}
