package com.example.book;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsS3Config {
	@Value("${aws.access.key.id}")
	private String awsId;

	@Value("${aws.secret.access.key}")
	private String awsKey;

	@Value("${aws.s3.region}")
	private String region;

	@Bean
	public AmazonS3 s3client() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsId, awsKey);
		return AmazonS3ClientBuilder.standard()
				.withRegion(Regions.fromName(region))
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.build();
	}
}
