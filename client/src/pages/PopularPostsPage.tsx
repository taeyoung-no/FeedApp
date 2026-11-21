import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPopularPosts, type PostResponse } from '../api/post'

function PopularPostsPage() {
  const [posts, setPosts] = useState<PostResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      setIsLoading(true)
      try {
        const data = await getPopularPosts()
        if (!cancelled) {
          setPosts(data)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('인기글을 불러오지 못했습니다')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main>
      <h2 className="max-w-2xl mx-auto text-3xl mb-5">인기글</h2>

      {isLoading && <p className="text-center text-2xl">인기글 불러오는 중...</p>}
      {error && <p className="text-center text-2xl">{error}</p>}
      {!isLoading && !error && posts.length === 0 && <p className="text-center text-2xl">인기글이 없습니다</p>}

      <div className="max-w-2xl mx-auto space-y-5 mb-5">
        {posts.map((post, index) => (
          <div key={post.id} className="flex items-center gap-4">
            <p className="shrink-0 w-8 text-2xl text-gray-500">{index + 1}</p>
            <Link to={`/posts/${post.id}`} className="flex-1 min-w-0 block cursor-pointer group">
              <h4 className="text-2xl text-blue-800 group-hover:text-black group-hover:underline">{post.title}</h4>
              <p className="text-sm text-gray-500 mt-1">{`${post.author} | ${formatDate(post.createdAt)}`}</p>
            </Link>
            <p className="shrink-0 text-lg text-black" aria-label={`좋아요 ${post.likeCount}개`}>
              좋아요 {post.likeCount}
            </p>
          </div>
        ))}
      </div>
    </main>
  )
}

function formatDate(iso: string) {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default PopularPostsPage
