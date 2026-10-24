/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.documentmanagement.contentrepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.fineract.infrastructure.core.domain.Base64EncodedImage;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.apache.fineract.infrastructure.documentmanagement.data.FileData;
import org.apache.fineract.infrastructure.documentmanagement.data.ImageData;
import org.apache.fineract.infrastructure.documentmanagement.domain.StorageType;
import org.apache.fineract.infrastructure.documentmanagement.exception.ContentManagementException;
import org.apache.fineract.infrastructure.documentmanagement.exception.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS SDK v2. v1 is end of life. Two behaviours of the v1 client are reproduced
 * explicitly here: it signed with SigV4 (v2 always does, so the old system
 * property is gone) and it reached buckets in any region through the global
 * endpoint, which is what cross-region access gives us.
 */
public class S3ContentRepository implements ContentRepository {

    private final static Logger logger = LoggerFactory.getLogger(S3ContentRepository.class);

    private final String s3BucketName;
    private final S3Client s3Client;

    public S3ContentRepository(final String bucketName, final String secretKey, final String accessKey) {
        this.s3BucketName = bucketName;
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.US_EAST_1).crossRegionAccessEnabled(true).build();
    }

    @Override
    public String saveFile(final InputStream toUpload, final DocumentCommand documentCommand) {
        final String fileName = documentCommand.getFileName();
        ContentRepositoryUtils.validateFileSizeWithinPermissibleRange(documentCommand.getSize(), fileName);

        final String uploadDocFolder = generateFileParentDirectory(documentCommand.getParentEntityType(),
                documentCommand.getParentEntityId());
        final String uploadDocFullPath = uploadDocFolder + File.separator + fileName;

        uploadDocument(fileName, toUpload, uploadDocFullPath);
        return uploadDocFullPath;
    }

    @Override
    public void deleteFile(final String documentName, final String documentPath) {
        try {
            deleteObjectFromS3(documentPath);
        } catch (final SdkException ace) {
            throw new ContentManagementException(documentName, ace.getMessage());
        }
    }

    @Override
    public String saveImage(final InputStream toUploadInputStream, final Long resourceId, final String imageName, final Long fileSize) {
        ContentRepositoryUtils.validateFileSizeWithinPermissibleRange(fileSize, imageName);
        final String uploadImageLocation = generateClientImageParentDirectory(resourceId);
        final String fileLocation = uploadImageLocation + File.separator + imageName;

        uploadDocument(imageName, toUploadInputStream, fileLocation);
        return fileLocation;
    }

    @Override
    public String saveImage(final Base64EncodedImage base64EncodedImage, final Long resourceId, final String imageName) {
        final String uploadImageLocation = generateClientImageParentDirectory(resourceId);
        final String fileLocation = uploadImageLocation + File.separator + imageName + base64EncodedImage.getFileExtension();
        final InputStream toUploadInputStream = new ByteArrayInputStream(
                Base64.getDecoder().decode(base64EncodedImage.getBase64EncodedString()));

        uploadDocument(imageName, toUploadInputStream, fileLocation);
        return fileLocation;
    }

    @Override
    public void deleteImage(final Long resourceId, final String location) {
        try {
            deleteObjectFromS3(location);
        } catch (final AwsServiceException ase) {
            deleteObjectAwsServiceExceptionMessage(ase);
            logger.warn("Unable to delete image associated with clients with Id " + resourceId);
        } catch (final SdkException ace) {
            deleteObjectSdkExceptionMessage(ace);
            logger.warn("Unable to delete image associated with clients with Id " + resourceId);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.S3;
    }

    @Override
    public FileData fetchFile(final DocumentData documentData) throws DocumentNotFoundException {
        FileData fileData = null;
        final String fileName = documentData.fileName();
        try {
            logger.info("Downloading an object");
            final InputStream objectContent = this.s3Client.getObject(
                    GetObjectRequest.builder().bucket(this.s3BucketName).key(documentData.fileLocation()).build());
            fileData = new FileData(objectContent, fileName, documentData.contentType());
        } catch (final SdkException ace) {
            logger.error(ace.getMessage());
            throw new DocumentNotFoundException(documentData.getParentEntityType(), documentData.getParentEntityId(), documentData.getId());
        }
        return fileData;
    }

    @Override
    public ImageData fetchImage(final ImageData imageData) {
        try {
            final InputStream objectContent = this.s3Client
                    .getObject(GetObjectRequest.builder().bucket(this.s3BucketName).key(imageData.location()).build());
            imageData.updateContent(objectContent);
        } catch (final SdkException e) {
            logger.error(e.getMessage());
        }
        return imageData;
    }

    private void deleteObjectSdkExceptionMessage(final SdkException ace) {
        final String message = "Caught an SdkException." + "Error Message: " + ace.getMessage();
        logger.error(message);
    }

    private void deleteObjectAwsServiceExceptionMessage(final AwsServiceException ase) {
        final String message = "Caught an AwsServiceException." + "Error Message:    " + ase.getMessage() + "HTTP Status Code: "
                + ase.statusCode() + "AWS Error Code:   "
                + (ase.awsErrorDetails() == null ? "" : ase.awsErrorDetails().errorCode()) + "Request ID:       " + ase.requestId();
        logger.error(message);
    }

    private String generateFileParentDirectory(final String entityType, final Long entityId) {
        return "documents" + File.separator + entityType + File.separator + entityId + File.separator
                + ContentRepositoryUtils.generateRandomString();
    }

    private String generateClientImageParentDirectory(final Long resourceId) {
        return "images" + File.separator + "clients" + File.separator + resourceId;
    }

    private void deleteObjectFromS3(final String location) {
        this.s3Client.deleteObject(DeleteObjectRequest.builder().bucket(this.s3BucketName).key(location).build());
    }

    private void uploadDocument(final String filename, final InputStream inputStream, final String s3UploadLocation)
            throws ContentManagementException {
        try {
            logger.info("Uploading a new object to S3 from a file to " + s3UploadLocation);
            // v2 needs the content length up front. The callers have already bounded
            // the size through validateFileSizeWithinPermissibleRange.
            final byte[] content = inputStream.readAllBytes();
            this.s3Client.putObject(PutObjectRequest.builder().bucket(this.s3BucketName).key(s3UploadLocation).build(),
                    RequestBody.fromBytes(content));
        } catch (final SdkException | IOException ace) {
            throw new ContentManagementException(filename, ace.getMessage());
        }
    }
}
