package com.example.book;


import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {
	@Value("${azure.storage.account-name}")
	private String accountName;

	@Value("${azure.storage.account-key}")
	private String accountKey;


	@Bean
	public DataLakeServiceClient dataLakeServiceClient() {
		String dataLakeServiceClientURL = String.format("https://%s.dfs.core.windows.net", accountName);
		StorageSharedKeyCredential sharedKeyCredential = new StorageSharedKeyCredential(accountName, accountKey);

		return new DataLakeServiceClientBuilder()
				.endpoint(dataLakeServiceClientURL)
				.credential(sharedKeyCredential)
				.buildClient();
	}
}
