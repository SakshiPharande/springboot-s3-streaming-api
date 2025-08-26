package com.example.fileuploadservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class S3Config {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${access.key}")   // Injected directly from Parameter Store
    private String accessKey;

    @Value("${secret.key}")   // Injected directly from Parameter Store
    private String secretKey;

    @Value("${bucket.name}")  // Injected directly from Parameter Store
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    // Optional: expose bucket name for other beans
    public String getBucketName() {
        return bucketName;
    }


    // public S3Client s3Client() {
    //     return S3Client.builder()
    //             .region(Region.of(region))
    //             .credentialsProvider(
    //                     StaticCredentialsProvider.create(
    //                             AwsBasicCredentials.create(accessKeyParam, secretKeyParam)
    //                     )
    //             )
    //             .build();
    // }
}
