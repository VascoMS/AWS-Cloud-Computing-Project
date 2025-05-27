package pt.ulisboa.tecnico.cnv.storage;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import pt.ulisboa.tecnico.cnv.javassist.model.Statistics;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StorageUtil {

    private static final String OUTPUT_FILE = "statistics.json";

    public static final String TABLE_NAME = "metrics";

    public static final String PARTITION_KEY = "game";

    public static final String SORT_KEY = "parameters";

    private static final String AWS_REGION = "us-east-2";

    private static final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

    public static void createTable() throws InterruptedException {
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_NAME)
                .withKeySchema(
                        new KeySchemaElement(PARTITION_KEY, KeyType.HASH),     // Partition key
                        new KeySchemaElement(SORT_KEY, KeyType.RANGE) // Sort key
                )
                .withAttributeDefinitions(
                        new AttributeDefinition(PARTITION_KEY, ScalarAttributeType.S),
                        new AttributeDefinition(SORT_KEY, ScalarAttributeType.S)
                )
                .withProvisionedThroughput(
                        new ProvisionedThroughput(1L, 1L)
                );
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
        TableUtils.waitUntilActive(dynamoDB, TABLE_NAME);
        System.out.println("Table created and ready: " + TABLE_NAME);
    }

    public static String serializeParameters(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Ensure consistent ordering
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("#"));
    }

    public static void storeStatistics(Map<String, String> parameters, Statistics statistics, String game) {
        String paramKey = serializeParameters(parameters);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(PARTITION_KEY, new AttributeValue(game)); // Partition key
        item.put(SORT_KEY, new AttributeValue(paramKey)); // Sort key

        // Add metrics as a nested map
        Map<String, AttributeValue> metricsMap = new HashMap<>();
        metricsMap.put("nblocks", new AttributeValue().withN(String.valueOf(statistics.getNblocks())));
        metricsMap.put("nmethod", new AttributeValue().withN(String.valueOf(statistics.getNmethod())));
        metricsMap.put("ninsts", new AttributeValue().withN(String.valueOf(statistics.getNinsts())));
        metricsMap.put("nDataWrites", new AttributeValue().withN(String.valueOf(statistics.getNdataWrites())));
        metricsMap.put("nDataReads", new AttributeValue().withN(String.valueOf(statistics.getNdataReads())));
        metricsMap.put("complexity", new AttributeValue().withN(String.valueOf(statistics.computeComplexity())));

        item.put("metrics", new AttributeValue().withM(metricsMap));

        PutItemRequest putRequest = new PutItemRequest()
                .withTableName(TABLE_NAME)
                .withItem(item);

        dynamoDB.putItem(putRequest);

    }
}