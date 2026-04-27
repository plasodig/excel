// Helper upload gambar ke R2. Object key konvensi: `excels/<id>.<ext>`.
// Cache: bucket R2 + custom domain bisa di-cache CDN otomatis.

import type { GeneratedImageBytes } from "../types";

export async function uploadExcelImage(
  bucket: R2Bucket,
  excelId: string,
  image: GeneratedImageBytes,
): Promise<string> {
  const key = `excels/${excelId}.${image.extension}`;
  await bucket.put(key, image.bytes, {
    httpMetadata: {
      contentType: image.contentType,
      cacheControl: "public, max-age=31536000, immutable",
    },
  });
  return key;
}

export async function deleteExcelImage(bucket: R2Bucket, key: string): Promise<void> {
  await bucket.delete(key);
}
