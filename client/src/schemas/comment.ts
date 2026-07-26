import { z } from 'zod'

export const createCommentSchema = z.object({
  content: z.string().min(1, '댓글 입력하세요').max(100, '너무 길어요'),
})

export type CreateCommentFormData = z.infer<typeof createCommentSchema>
