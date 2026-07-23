import { z } from 'zod'

export const signupSchema = z
  .object({
    username: z.string().min(1, '아이디 입력하세요').max(8, '너무 길어요'),
    password: z.string().min(1, '비밀번호 입력하세요').max(8, '너무 길어요'),
    passwordConfirm: z.string().min(1, '비밀번호 한 번 더 입력하세요'),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: '비밀번호랑 다름',
    path: ['passwordConfirm'],
  })

export type SignupFormData = z.infer<typeof signupSchema>
