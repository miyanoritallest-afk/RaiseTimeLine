import { useState, useEffect, useRef, useCallback } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { getTimeline, updatePost, deletePost, toggleLike } from '../api/posts'
import type { PostResponse } from '../types/post'
import PostForm from '../components/PostForm'
import PostCard from '../components/PostCard'
import EditPostModal from '../components/EditPostModal'

const POLL_INTERVAL = 30_000

export default function HomePage() {
  const { user } = useAuth()
  const [posts, setPosts] = useState<PostResponse[]>([])
  const [feed, setFeed] = useState<'all' | 'following'>('all')
  const [hasMore, setHasMore] = useState(false)
  const [nextCursor, setNextCursor] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [editingPost, setEditingPost] = useState<PostResponse | null>(null)
  const [newCount, setNewCount] = useState(0)
  const [pendingPosts, setPendingPosts] = useState<PostResponse[]>([])

  const sentinelRef = useRef<HTMLDivElement>(null)
  const topPostIdRef = useRef<number | null>(null)
  const feedRef = useRef(feed)
  feedRef.current = feed

  const loadInitial = useCallback(async (f: 'all' | 'following') => {
    setLoading(true)
    setNewCount(0)
    setPendingPosts([])
    try {
      const data = await getTimeline(f)
      setPosts(data.items)
      setHasMore(data.hasMore)
      setNextCursor(data.nextCursor)
      topPostIdRef.current = data.items[0]?.id ?? null
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadInitial(feed)
  }, [feed, loadInitial])

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore || nextCursor == null) return
    setLoadingMore(true)
    try {
      const data = await getTimeline(feedRef.current, nextCursor)
      setPosts((prev) => [...prev, ...data.items])
      setHasMore(data.hasMore)
      setNextCursor(data.nextCursor)
    } finally {
      setLoadingMore(false)
    }
  }, [loadingMore, hasMore, nextCursor])

  // 無限スクロール
  useEffect(() => {
    const sentinel = sentinelRef.current
    if (!sentinel) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) loadMore()
      },
      { threshold: 0.1 },
    )
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [loadMore])

  // 新着ポーリング（30秒間隔）
  useEffect(() => {
    const timer = setInterval(async () => {
      try {
        const data = await getTimeline(feedRef.current)
        const currentTopId = topPostIdRef.current
        const fresh = currentTopId == null
          ? data.items
          : data.items.filter((p) => p.id > currentTopId)
        if (fresh.length > 0) {
          setPendingPosts(fresh)
          setNewCount(fresh.length)
        }
      } catch {
        // ポーリングエラーは無視
      }
    }, POLL_INTERVAL)
    return () => clearInterval(timer)
  }, [])

  const handleShowNew = () => {
    setPosts((prev) => {
      const merged = [...pendingPosts, ...prev]
      // 重複除去
      const seen = new Set<number>()
      return merged.filter((p) => {
        if (seen.has(p.id)) return false
        seen.add(p.id)
        return true
      })
    })
    topPostIdRef.current = pendingPosts[0]?.id ?? topPostIdRef.current
    setPendingPosts([])
    setNewCount(0)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handlePosted = (post: PostResponse) => {
    setPosts((prev) => [post, ...prev])
    topPostIdRef.current = post.id
    setNewCount(0)
    setPendingPosts([])
  }

  const handleLike = async (id: number, liked: boolean) => {
    // 楽観的更新
    setPosts((prev) =>
      prev.map((p) =>
        p.id === id
          ? { ...p, likedByMe: !liked, likeCount: p.likeCount + (liked ? -1 : 1) }
          : p,
      ),
    )
    try {
      await toggleLike(id, liked)
    } catch {
      // ロールバック
      setPosts((prev) =>
        prev.map((p) =>
          p.id === id
            ? { ...p, likedByMe: liked, likeCount: p.likeCount + (liked ? 1 : -1) }
            : p,
        ),
      )
    }
  }

  const handleEdit = (post: PostResponse) => {
    setEditingPost(post)
  }

  const handleSaveEdit = async (content: string) => {
    if (!editingPost) return
    const updated = await updatePost(editingPost.id, content)
    setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
    setEditingPost(null)
  }

  const handleDelete = async (id: number) => {
    await deletePost(id)
    setPosts((prev) => prev.filter((p) => p.id !== id))
  }

  if (!user) return null

  return (
    <>
      <main id="app-main">
        <PostForm onPosted={handlePosted} />

        <div className="tabs">
          <button
            className={`tab-btn${feed === 'all' ? ' active' : ''}`}
            onClick={() => setFeed('all')}
          >
            おすすめ
          </button>
          <button
            className={`tab-btn${feed === 'following' ? ' active' : ''}`}
            onClick={() => setFeed('following')}
          >
            フォロー中
          </button>
        </div>

        {newCount > 0 && (
          <button className="new-posts-banner" onClick={handleShowNew}>
            ↑ {newCount}件の新しい投稿を表示
          </button>
        )}

        {loading ? (
          <div className="empty-state">
            <p>読み込み中...</p>
          </div>
        ) : posts.length === 0 ? (
          <div className="empty-state">
            <h3>まだ投稿がありません</h3>
            <p>最初の投稿をしてみましょう！</p>
          </div>
        ) : (
          posts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              currentUserId={user.id}
              onLike={handleLike}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))
        )}

        <div ref={sentinelRef} style={{ height: 1 }} />
        {loadingMore && (
          <div className="empty-state" style={{ padding: '16px' }}>
            <p>読み込み中...</p>
          </div>
        )}
      </main>

      {editingPost && (
        <EditPostModal
          post={editingPost}
          onSave={handleSaveEdit}
          onClose={() => setEditingPost(null)}
        />
      )}
    </>
  )
}
