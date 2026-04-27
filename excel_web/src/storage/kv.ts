// Helper upload gambar ke KV. Object key konvensi: `excels/<id>.<ext>`.
// KV digunakan sebagai pengganti R2 jika user tidak memiliki kartu kredit.

import type { GeneratedImageBytes } from "../types";

export async function uploadExcelImage(
    kv: KVNamespace,
    excelId: string,
    image: GeneratedImageBytes,
): Promise<string> {
    const key = `excels/${excelId}.${image.extension}`;
    // Simpan bytes gambar. KV bisa menyimpan ArrayBuffer langsung.
    // Kita tambahkan metadata contentType agar bisa disajikan dengan benar nanti.
    await kv.put(key, image.bytes, {
        metadata: { contentType: image.contentType },
    });
    return key;
}

export async function deleteExcelImage(kv: KVNamespace, key: string): Promise<void> {
    await kv.delete(key);
}
