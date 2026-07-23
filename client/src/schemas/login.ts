import { z } from 'zod'

export const loginSchema = z.object({
  username: z.string().min(1, '아이디 입력하세요').max(8, '너무 길어요'),
  password: z.string().min(1, '비밀번호 입력하세요').max(8, '너무 길어요'),
})

export type LoginFormData = z.infer<typeof loginSchema>
