import { z } from 'zod'

export const createPostSchema = z.object({
  title: z.string().min(1, '제목 입력하세요'),
  content: z.string().min(1, '내용 입력하세요'),
})

export type CreatePostFormData = z.infer<typeof createPostSchema>
