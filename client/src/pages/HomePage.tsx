import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPosts, type PostResponse } from '../api/post'

function HomePage() {
  const [posts, setPosts] = useState<PostResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      try {
        const data = await getPosts()
        if (!cancelled) {
          setPosts(data)
          setError(null)
        }
      } catch {
        if (!cancelled) {
          setError('글을 불러오지 못했습니다')
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
      {isLoading && <p className="text-center text-2xl">글 불러오는 중...</p>}
      {error && <p className="text-center text-2xl">{error}</p>}
      {!isLoading && !error && posts.length === 0 && <p className="text-center text-2xl">글이 없습니다</p>}

      <div className="max-w-2xl mx-auto space-y-5 mb-5">
        {posts.map((post) => (
          <div key={post.id} className="flex items-center">
            <Link to={`/posts/${post.id}`} className="flex-1 block cursor-pointer group">
              <h4 className="text-2xl text-blue-800 group-hover:text-black group-hover:underline">{post.title}</h4>
              <p className="text-sm text-gray-500 mt-1">{`${post.author} · ${formatDate(post.createdAt)}`}</p>
            </Link>
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

export default HomePage
