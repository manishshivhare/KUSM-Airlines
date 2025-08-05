package com.kusm.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.kusm.exceptions.FileUploadException;

@Service
public class S3Service {

    @Autowired
    private AmazonS3 amazonS3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public String uploadProfilePhoto(MultipartFile file, Long userId) {
        validateFile(file);
        
        try {
            String fileName = generateFileName(file, userId);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.addUserMetadata("user-id", userId.toString());
            metadata.addUserMetadata("upload-type", "profile-photo");

            PutObjectRequest request = new PutObjectRequest(bucketName, fileName, 
                    file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead);

            amazonS3Client.putObject(request);
            
            return generatePublicUrl(fileName);
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload profile photo: " + e.getMessage());
        }
    }

    public void deleteProfilePhoto(String photoUrl) {
        if (photoUrl != null && photoUrl.contains(bucketName)) {
            try {
                String fileName = extractFileNameFromUrl(photoUrl);
                DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName, fileName);
                amazonS3Client.deleteObject(deleteRequest);
            } catch (Exception e) {
                // Log the error but don't throw exception as it's not critical
                System.err.println("Failed to delete old profile photo: " + e.getMessage());
            }
        }
    }

    public boolean doesFileExist(String photoUrl) {
        if (photoUrl == null || !photoUrl.contains(bucketName)) {
            return false;
        }
        
        try {
            String fileName = extractFileNameFromUrl(photoUrl);
            return amazonS3Client.doesObjectExist(bucketName, fileName);
        } catch (Exception e) {
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null");
        }

        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new FileUploadException("File size must not exceed 5MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("image/jpeg") && 
             !contentType.equals("image/png") && 
             !contentType.equals("image/jpg"))) {
            throw new FileUploadException("Only JPEG, JPG and PNG files are allowed");
        }

        // Validate file extension
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !hasValidImageExtension(originalFileName)) {
            throw new FileUploadException("Invalid file extension. Only .jpg, .jpeg, .png are allowed");
        }
    }

    private boolean hasValidImageExtension(String fileName) {
        String extension = fileName.toLowerCase();
        return extension.endsWith(".jpg") || 
               extension.endsWith(".jpeg") || 
               extension.endsWith(".png");
    }

    private String generateFileName(MultipartFile file, Long userId) {
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String uniqueId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        return String.format("profile-photos/user-%d/%d-%s%s", userId, timestamp, uniqueId, extension);
    }

    private String generatePublicUrl(String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
    }

    private String extractFileNameFromUrl(String url) {
        // Extract the file name from S3 URL format
        // Example: https://bucket.s3.region.amazonaws.com/profile-photos/user-1/file.jpg
        String[] parts = url.split(".amazonaws.com/");
        if (parts.length > 1) {
            return parts[1];
        }
        // Fallback - extract everything after the last slash
        return url.substring(url.lastIndexOf('/') + 1);
    }
}