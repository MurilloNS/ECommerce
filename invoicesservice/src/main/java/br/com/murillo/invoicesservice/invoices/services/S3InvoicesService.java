package br.com.murillo.invoicesservice.invoices.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
public class S3InvoicesService {
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3InvoicesService(S3Presigner s3Presigner, @Value("${invoices.bucket.name}") String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }
}