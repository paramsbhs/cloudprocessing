#!/bin/sh
# Runs inside LocalStack on startup — creates the S3 bucket for local dev.
set -e

echo "Creating S3 bucket: cloud-processing-files"
awslocal s3 mb s3://cloud-processing-files
awslocal s3api put-bucket-cors --bucket cloud-processing-files --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }]
}'
echo "LocalStack S3 bucket ready."
