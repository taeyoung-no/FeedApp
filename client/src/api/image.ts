import { apiClient } from './client'
import type { components } from './generated/schema'

export type PresignedUpload = components['schemas']['PresignedUpload']
export type CreateUploadUrlRequest = components['schemas']['CreateUploadUrlRequest']
export type PresignedDownload = components['schemas']['PresignedDownload']

export async function createUploadUrl(contentType: string): Promise<PresignedUpload> {
  const response = await apiClient.post<PresignedUpload>('/images/upload-url', { contentType })
  return response.data
}

export async function putImageToS3(uploadUrl: string, file: File): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  })

  if (!response.ok) {
    throw new Error('이미지 업로드 실패')
  }
}

export async function deleteImage(key: string): Promise<void> {
  await apiClient.delete('/images', {
    params: { key },
  })
}
