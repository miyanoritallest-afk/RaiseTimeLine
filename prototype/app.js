'use strict';

/* =====================================================
   DB 層 — localStorage ラッパー
===================================================== */
const DB_KEYS = {
  users: 'rtl_users',
  posts: 'rtl_posts',
  comments: 'rtl_comments',
  likes: 'rtl_likes',
  follows: 'rtl_follows',
  currentUserId: 'rtl_current_user_id',
};

const db = {
  users: [],
  posts: [],
  comments: [],
  likes: [],
  follows: [],
};

function initDb() {
  db.users    = JSON.parse(localStorage.getItem(DB_KEYS.users)    || '[]');
  db.posts    = JSON.parse(localStorage.getItem(DB_KEYS.posts)    || '[]');
  db.comments = JSON.parse(localStorage.getItem(DB_KEYS.comments) || '[]');
  db.likes    = JSON.parse(localStorage.getItem(DB_KEYS.likes)    || '[]');
  db.follows  = JSON.parse(localStorage.getItem(DB_KEYS.follows)  || '[]');
}

function saveDb() {
  try {
    localStorage.setItem(DB_KEYS.users,    JSON.stringify(db.users));
    localStorage.setItem(DB_KEYS.posts,    JSON.stringify(db.posts));
    localStorage.setItem(DB_KEYS.comments, JSON.stringify(db.comments));
    localStorage.setItem(DB_KEYS.likes,    JSON.stringify(db.likes));
    localStorage.setItem(DB_KEYS.follows,  JSON.stringify(db.follows));
  } catch (e) {
    if (e.name === 'QuotaExceededError') {
      alert('ストレージの空き容量が不足しています。古い投稿や画像を削除してください。');
    }
  }
}

function generateId(prefix) {
  return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 100000)}`;
}

function getCurrentUser() {
  const id = localStorage.getItem(DB_KEYS.currentUserId);
  return id ? db.users.find(u => u.id === id) || null : null;
}

function setCurrentUser(userId) {
  localStorage.setItem(DB_KEYS.currentUserId, userId);
}

function clearCurrentUser() {
  localStorage.removeItem(DB_KEYS.currentUserId);
}

/* =====================================================
   初期ダミーデータ
===================================================== */
function initDummyData() {
  if (localStorage.getItem(DB_KEYS.users)) return; // 二重初期化防止

  const now = new Date();
  const ts = (minutesAgo) => new Date(now - minutesAgo * 60000).toISOString();

  const users = [
    { id: 'u_001', email: 'taro@example.com', password: 'password123', username: 'Taro_Yamada', bio: 'エンジニア志望。毎日コツコツ学習中！', avatarDataUrl: '', createdAt: ts(10000) },
    { id: 'u_002', email: 'hanako@example.com', password: 'password123', username: 'Hanako_Sato', bio: 'デザインとコードが好きです。', avatarDataUrl: '', createdAt: ts(9000) },
    { id: 'u_003', email: 'kenji@example.com', password: 'password123', username: 'Kenji_Suzuki', bio: 'バックエンド勉強中。Spring Boot 学んでます。', avatarDataUrl: '', createdAt: ts(8000) },
    { id: 'u_004', email: 'yuki@example.com', password: 'password123', username: 'Yuki_Tanaka', bio: 'フロントエンド大好き。React 勢。', avatarDataUrl: '', createdAt: ts(7000) },
  ];

  const posts = [
    { id: 'p_001', userId: 'u_001', content: 'RaiseTimeLine へようこそ！みなさんよろしくお願いします🎉', imageDataUrls: [], createdAt: ts(200), updatedAt: ts(200) },
    { id: 'p_002', userId: 'u_002', content: '最近 Figma でデザインシステムを作り始めました。コンポーネント設計が楽しいですね。', imageDataUrls: [], createdAt: ts(180), updatedAt: ts(180) },
    { id: 'p_003', userId: 'u_003', content: 'Spring Boot 3.x で JWT 認証を実装しました。Spring Security の設定がかなり変わってて最初戸惑いましたが、慣れてくるとすっきり書けますね。', imageDataUrls: [], createdAt: ts(150), updatedAt: ts(150) },
    { id: 'p_004', userId: 'u_004', content: 'React 18 の Concurrent Mode、最高ですね。useTransition を使うと重い処理も UX を損なわずに書けます。', imageDataUrls: [], createdAt: ts(120), updatedAt: ts(120) },
    { id: 'p_005', userId: 'u_001', content: 'PostgreSQL の EXPLAIN ANALYZE を初めてちゃんと読んでみた。インデックスがないクエリがどれだけ遅いか一目瞭然で勉強になった。', imageDataUrls: [], createdAt: ts(90), updatedAt: ts(90) },
    { id: 'p_006', userId: 'u_002', content: 'CSSカスタムプロパティ（変数）をフル活用したデザインシステム、かなり管理しやすい！テーマ切り替えも一瞬でできる。', imageDataUrls: [], createdAt: ts(60), updatedAt: ts(60) },
    { id: 'p_007', userId: 'u_003', content: 'Docker Compose で PostgreSQL + Spring Boot + React の環境をまとめて立ち上げられるようにした。開発効率が上がった！', imageDataUrls: [], createdAt: ts(45), updatedAt: ts(45) },
    { id: 'p_008', userId: 'u_004', content: 'TypeScript の satisfies 演算子、最近知った。型チェックしつつ型推論も活かせるのが便利すぎる。', imageDataUrls: [], createdAt: ts(30), updatedAt: ts(30) },
    { id: 'p_009', userId: 'u_001', content: 'Git の rebase -i で古いコミットメッセージを修正した。歴史を綺麗に保つって大事だと実感。', imageDataUrls: [], createdAt: ts(15), updatedAt: ts(15) },
    { id: 'p_010', userId: 'u_002', content: 'アクセシビリティって難しいけど大事！WCAG 2.1 を読みながらコントラスト比を確認し直してる。', imageDataUrls: [], createdAt: ts(5), updatedAt: ts(5) },
  ];

  const comments = [
    { id: 'c_001', postId: 'p_001', userId: 'u_002', content: 'こちらこそよろしく！楽しみましょう😊', createdAt: ts(195), updatedAt: ts(195) },
    { id: 'c_002', postId: 'p_001', userId: 'u_003', content: 'よろしくお願いします！', createdAt: ts(190), updatedAt: ts(190) },
    { id: 'c_003', postId: 'p_003', userId: 'u_004', content: 'Spring Security 6 系の設定、本当に書き方が変わりましたよね。ラムダ DSL に統一された感じ。', createdAt: ts(145), updatedAt: ts(145) },
    { id: 'c_004', postId: 'p_004', userId: 'u_001', content: 'useTransition 便利ですよね。Suspense と組み合わせると最強。', createdAt: ts(115), updatedAt: ts(115) },
    { id: 'c_005', postId: 'p_005', userId: 'u_003', content: 'EXPLAIN ANALYZE は必須スキルですよね。Seq Scan が出たら即インデックス検討！', createdAt: ts(85), updatedAt: ts(85) },
    { id: 'c_006', postId: 'p_008', userId: 'u_002', content: 'satisfies 演算子、私もつい先週知りました！型エラー出しながら補完も効くの最高ですよね。', createdAt: ts(25), updatedAt: ts(25) },
  ];

  const likes = [
    { postId: 'p_001', userId: 'u_002', createdAt: ts(194) },
    { postId: 'p_001', userId: 'u_003', createdAt: ts(193) },
    { postId: 'p_001', userId: 'u_004', createdAt: ts(192) },
    { postId: 'p_003', userId: 'u_001', createdAt: ts(148) },
    { postId: 'p_003', userId: 'u_004', createdAt: ts(147) },
    { postId: 'p_004', userId: 'u_001', createdAt: ts(118) },
    { postId: 'p_004', userId: 'u_003', createdAt: ts(117) },
    { postId: 'p_008', userId: 'u_001', createdAt: ts(28) },
  ];

  const follows = [
    { followerId: 'u_001', followingId: 'u_002', createdAt: ts(500) },
    { followerId: 'u_001', followingId: 'u_003', createdAt: ts(490) },
    { followerId: 'u_002', followingId: 'u_001', createdAt: ts(480) },
    { followerId: 'u_003', followingId: 'u_001', createdAt: ts(470) },
  ];

  db.users    = users;
  db.posts    = posts;
  db.comments = comments;
  db.likes    = likes;
  db.follows  = follows;
  saveDb();
}

/* =====================================================
   ユーティリティ
===================================================== */
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatDate(isoString) {
  if (!isoString) return '';
  const d = new Date(isoString);
  const now = new Date();
  const diffMs = now - d;
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);
  if (diffMin < 1) return 'たった今';
  if (diffMin < 60) return `${diffMin}分前`;
  if (diffHour < 24) return `${diffHour}時間前`;
  if (diffDay < 7) return `${diffDay}日前`;
  return `${d.getFullYear()}年${d.getMonth()+1}月${d.getDate()}日`;
}

function debounce(fn, ms) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), ms);
  };
}

function getDefaultAvatar(username) {
  const initial = (username || '?').charAt(0).toUpperCase();
  const colors = ['#1d9bf0','#7856ff','#ff7a00','#00ba7c','#ff3b3b','#f091a0'];
  const color = colors[initial.charCodeAt(0) % colors.length];
  return `data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40"><rect width="40" height="40" fill="${color}"/><text x="20" y="26" text-anchor="middle" font-size="18" font-family="sans-serif" font-weight="bold" fill="white">${initial}</text></svg>`)}`;
}

function avatarSrc(user) {
  return (user && user.avatarDataUrl) ? user.avatarDataUrl : getDefaultAvatar(user ? user.username : '?');
}

function getUserById(id) {
  return db.users.find(u => u.id === id) || null;
}

/* =====================================================
   認証
===================================================== */
function requireAuth() {
  if (!getCurrentUser()) {
    location.hash = '#login';
    return false;
  }
  return true;
}

function handleLogin(email, password) {
  const user = db.users.find(u => u.email === email && u.password === password);
  if (!user) return false;
  setCurrentUser(user.id);
  return true;
}

function handleRegister(username, email, password) {
  const id = generateId('u');
  const now = new Date().toISOString();
  const newUser = { id, email, password, username, bio: '', avatarDataUrl: '', createdAt: now };
  db.users.push(newUser);
  saveDb();
  setCurrentUser(id);
}

function handleLogout() {
  clearCurrentUser();
  location.hash = '#login';
}

/* =====================================================
   いいね
===================================================== */
function getLikeCount(postId) {
  return db.likes.filter(l => l.postId === postId).length;
}

function isLikedByMe(postId) {
  const me = getCurrentUser();
  return me ? db.likes.some(l => l.postId === postId && l.userId === me.id) : false;
}

function toggleLike(postId) {
  const me = getCurrentUser();
  if (!me) return;
  const idx = db.likes.findIndex(l => l.postId === postId && l.userId === me.id);
  if (idx >= 0) {
    db.likes.splice(idx, 1);
  } else {
    db.likes.push({ postId, userId: me.id, createdAt: new Date().toISOString() });
  }
  saveDb();
}

/* =====================================================
   フォロー
===================================================== */
function isFollowing(targetUserId) {
  const me = getCurrentUser();
  return me ? db.follows.some(f => f.followerId === me.id && f.followingId === targetUserId) : false;
}

function getFollowerCount(userId) {
  return db.follows.filter(f => f.followingId === userId).length;
}

function getFollowingCount(userId) {
  return db.follows.filter(f => f.followerId === userId).length;
}

function getFollowers(userId) {
  return db.follows.filter(f => f.followingId === userId).map(f => getUserById(f.followerId)).filter(Boolean);
}

function getFollowing(userId) {
  return db.follows.filter(f => f.followerId === userId).map(f => getUserById(f.followingId)).filter(Boolean);
}

function toggleFollow(targetUserId) {
  const me = getCurrentUser();
  if (!me || me.id === targetUserId) return;
  const idx = db.follows.findIndex(f => f.followerId === me.id && f.followingId === targetUserId);
  if (idx >= 0) {
    db.follows.splice(idx, 1);
  } else {
    db.follows.push({ followerId: me.id, followingId: targetUserId, createdAt: new Date().toISOString() });
  }
  saveDb();
}

/* =====================================================
   投稿
===================================================== */
function handleCreatePost(content, imageDataUrls) {
  const me = getCurrentUser();
  if (!me) return;
  const now = new Date().toISOString();
  const post = {
    id: generateId('p'),
    userId: me.id,
    content,
    imageDataUrls: imageDataUrls || [],
    createdAt: now,
    updatedAt: now,
  };
  db.posts.unshift(post);
  saveDb();
  return post;
}

function handleEditPost(postId, newContent) {
  const post = db.posts.find(p => p.id === postId);
  if (!post) return;
  post.content = newContent;
  post.updatedAt = new Date().toISOString();
  saveDb();
}

function handleDeletePost(postId) {
  db.posts = db.posts.filter(p => p.id !== postId);
  db.comments = db.comments.filter(c => c.postId !== postId);
  db.likes = db.likes.filter(l => l.postId !== postId);
  saveDb();
}

/* =====================================================
   コメント
===================================================== */
function handleCreateComment(postId, content) {
  const me = getCurrentUser();
  if (!me) return;
  const now = new Date().toISOString();
  const comment = { id: generateId('c'), postId, userId: me.id, content, createdAt: now, updatedAt: now };
  db.comments.push(comment);
  saveDb();
  return comment;
}

function handleEditComment(commentId, newContent) {
  const comment = db.comments.find(c => c.id === commentId);
  if (!comment) return;
  comment.content = newContent;
  comment.updatedAt = new Date().toISOString();
  saveDb();
}

function handleDeleteComment(commentId) {
  db.comments = db.comments.filter(c => c.id !== commentId);
  saveDb();
}

/* =====================================================
   画像選択 (FileReader)
===================================================== */
let _pendingImages = []; // プレビュー用 DataURL の配列

function handleImageSelect(files, onUpdate) {
  const remaining = 4 - _pendingImages.length;
  const selected = Array.from(files).slice(0, remaining);
  if (selected.length === 0) {
    if (Array.from(files).length > 0) alert('画像は最大4枚まで添付できます。');
    return;
  }
  let loaded = 0;
  for (const file of selected) {
    const reader = new FileReader();
    reader.onload = (e) => {
      _pendingImages.push(e.target.result);
      loaded++;
      if (loaded === selected.length) onUpdate(_pendingImages);
    };
    reader.readAsDataURL(file);
  }
}

function removePendingImage(index) {
  _pendingImages.splice(index, 1);
}

function clearPendingImages() {
  _pendingImages = [];
}

/* =====================================================
   プロフィールタブ用データ収集ヘルパー
===================================================== */
function buildProfileTabData(userId) {
  const userMap = new Map(db.users.map(u => [u.id, u]));

  const userPosts = db.posts
    .filter(p => p.userId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  const userComments = db.comments
    .filter(c => c.userId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  const commentPostIds = new Set(userComments.map(c => c.postId));
  const commentPostMap = new Map(
    db.posts.filter(p => commentPostIds.has(p.id)).map(p => [p.id, p])
  );

  const likedEntries = db.likes
    .filter(l => l.userId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const likedPostIds = likedEntries.map(l => l.postId);
  const likedPostIdSet = new Set(likedPostIds);
  const likedPosts = db.posts
    .filter(p => likedPostIdSet.has(p.id))
    .sort((a, b) => likedPostIds.indexOf(a.id) - likedPostIds.indexOf(b.id));

  const likeCountMap = new Map();
  db.likes.forEach(l => {
    likeCountMap.set(l.postId, (likeCountMap.get(l.postId) || 0) + 1);
  });

  const commentCountMap = new Map();
  db.comments.forEach(c => {
    commentCountMap.set(c.postId, (commentCountMap.get(c.postId) || 0) + 1);
  });

  const me = getCurrentUser();
  const myLikedPostIds = new Set(
    db.likes.filter(l => l.userId === (me ? me.id : null)).map(l => l.postId)
  );

  return { userMap, userPosts, userComments, commentPostMap, likedPosts, likeCountMap, commentCountMap, myLikedPostIds };
}

/* =====================================================
   HTML ビルダー（再利用部品）
===================================================== */
function buildAvatarHtml(user, sizeClass = '') {
  const src = avatarSrc(user);
  const username = escapeHtml(user ? user.username : '?');
  return `<div class="avatar ${sizeClass}" data-action="go-profile" data-user-id="${user ? user.id : ''}">
    <img src="${src}" alt="${username}">
  </div>`;
}

function buildPostCard(post, currentUser, cache = null) {
  const author = cache ? cache.userMap.get(post.userId) : getUserById(post.userId);
  if (!author) return '';
  const isOwn = currentUser && currentUser.id === post.userId;
  const liked = cache ? cache.myLikedPostIds.has(post.id) : isLikedByMe(post.id);
  const likeCount = cache ? (cache.likeCountMap.get(post.id) || 0) : getLikeCount(post.id);
  const commentCount = cache ? (cache.commentCountMap.get(post.id) || 0) : db.comments.filter(c => c.postId === post.id).length;

  let imagesHtml = '';
  if (post.imageDataUrls && post.imageDataUrls.length > 0) {
    const count = post.imageDataUrls.length;
    const imgItems = post.imageDataUrls.map((src, i) =>
      `<div class="post-img-wrap">
        <img class="post-img" src="${src}" alt="投稿画像${i+1}" data-action="view-image" data-src="${src}">
      </div>`
    ).join('');
    imagesHtml = `<div class="post-images" data-count="${count}">${imgItems}</div>`;
  }

  const menuHtml = isOwn ? `
    <div class="post-menu-btn" data-action="toggle-post-menu" data-post-id="${post.id}">⋯
      <div class="post-menu-dropdown hidden" id="post-menu-${post.id}">
        <button class="post-menu-item" data-action="edit-post" data-post-id="${post.id}">編集</button>
        <button class="post-menu-item danger" data-action="delete-post" data-post-id="${post.id}">削除</button>
      </div>
    </div>` : '';

  return `<div class="post-card" data-post-id="${post.id}">
    ${buildAvatarHtml(author)}
    <div class="post-card-right">
      <div class="post-card-header">
        <span class="post-username" data-action="go-profile" data-user-id="${author.id}">${escapeHtml(author.username)}</span>
        <span class="post-date">${formatDate(post.createdAt)}</span>
        ${menuHtml}
      </div>
      <div class="post-content">${escapeHtml(post.content)}</div>
      ${imagesHtml}
      <div class="post-actions">
        <button class="action-btn ${liked ? 'liked' : ''}" data-action="toggle-like" data-post-id="${post.id}">
          <span class="action-icon">${liked ? '♥' : '♡'}</span>
          <span class="action-count" id="like-count-${post.id}">${likeCount}</span>
        </button>
        <button class="action-btn" data-action="go-post-detail" data-post-id="${post.id}">
          <span class="action-icon">💬</span>
          <span class="action-count" id="comment-count-${post.id}">${commentCount}</span>
        </button>
      </div>
    </div>
  </div>`;
}

function buildUserCard(user, showFollowBtn) {
  const me = getCurrentUser();
  const following = isFollowing(user.id);
  let followBtn = '';
  if (showFollowBtn && me && me.id !== user.id) {
    if (following) {
      followBtn = `<button class="btn-unfollow" data-action="toggle-follow" data-user-id="${user.id}">フォロー解除</button>`;
    } else {
      followBtn = `<button class="btn-follow" data-action="toggle-follow" data-user-id="${user.id}">フォローする</button>`;
    }
  }
  return `<div class="user-card" data-action="go-profile" data-user-id="${user.id}">
    ${buildAvatarHtml(user)}
    <div class="user-card-info">
      <div class="user-card-name">${escapeHtml(user.username)}</div>
      ${user.bio ? `<div class="user-card-bio">${escapeHtml(user.bio)}</div>` : ''}
    </div>
    ${followBtn}
  </div>`;
}

function buildCommentItem(comment, currentUser) {
  const author = getUserById(comment.userId);
  if (!author) return '';
  const isOwn = currentUser && currentUser.id === comment.userId;
  const menuHtml = isOwn ? `
    <div class="comment-menu-btn" data-action="toggle-comment-menu" data-comment-id="${comment.id}">⋯
      <div class="post-menu-dropdown hidden" id="comment-menu-${comment.id}">
        <button class="post-menu-item" data-action="edit-comment" data-comment-id="${comment.id}">編集</button>
        <button class="post-menu-item danger" data-action="delete-comment" data-comment-id="${comment.id}">削除</button>
      </div>
    </div>` : '';

  return `<div class="comment-item" data-comment-id="${comment.id}">
    ${buildAvatarHtml(author, 'avatar')}
    <div class="comment-right">
      <div class="comment-header">
        <span class="comment-username" data-action="go-profile" data-user-id="${author.id}" style="cursor:pointer">${escapeHtml(author.username)}</span>
        <span class="comment-date">${formatDate(comment.createdAt)}</span>
        ${menuHtml}
      </div>
      <div class="comment-content">${escapeHtml(comment.content)}</div>
    </div>
  </div>`;
}

function buildProfileCommentItem(comment, post, currentUser, cache = null) {
  const author = cache ? cache.userMap.get(comment.userId) : getUserById(comment.userId);
  if (!author) return '';
  const postAuthor = post
    ? (cache ? cache.userMap.get(post.userId) : getUserById(post.userId))
    : null;

  const postPreview = post
    ? `<div class="profile-comment-post-link" data-action="go-post-detail" data-post-id="${post.id}">
        <span class="profile-comment-post-label">返信先:</span>
        <span class="profile-comment-post-author">${postAuthor ? escapeHtml(postAuthor.username) : '不明'}</span>
        <span class="profile-comment-post-snippet">${escapeHtml(post.content.slice(0, 50))}${post.content.length > 50 ? '…' : ''}</span>
      </div>`
    : '';

  return `<div class="comment-item profile-comment-item" data-comment-id="${comment.id}">
    ${buildAvatarHtml(author, 'avatar')}
    <div class="comment-right">
      <div class="comment-header">
        <span class="comment-username" data-action="go-profile" data-user-id="${author.id}" style="cursor:pointer">${escapeHtml(author.username)}</span>
        <span class="comment-date">${formatDate(comment.createdAt)}</span>
      </div>
      ${postPreview}
      <div class="comment-content">${escapeHtml(comment.content)}</div>
    </div>
  </div>`;
}

/* =====================================================
   ヘッダー描画
===================================================== */
function renderHeader() {
  const header = document.getElementById('app-header');
  const me = getCurrentUser();
  if (!me) { header.innerHTML = ''; return; }
  header.innerHTML = `
    <div class="header-inner">
      <span class="header-logo" data-action="go-home">RaiseTimeLine</span>
      <div class="header-actions">
        <button class="header-icon-btn" data-action="go-search" title="ユーザー検索">🔍</button>
        <div class="user-menu-wrapper">
          <div class="user-menu-trigger" data-action="toggle-user-menu">
            <div class="user-avatar-small">
              <img src="${avatarSrc(me)}" alt="${escapeHtml(me.username)}">
            </div>
            <span class="user-menu-name">${escapeHtml(me.username)}</span>
            <span class="user-menu-caret">▼</span>
          </div>
          <div class="dropdown-menu hidden" id="user-dropdown">
            <button class="dropdown-item" data-action="go-my-profile">プロフィール</button>
            <button class="dropdown-item danger" data-action="logout">ログアウト</button>
          </div>
        </div>
      </div>
    </div>`;
}

/* =====================================================
   画面: ログイン
===================================================== */
function renderLogin() {
  const header = document.getElementById('app-header');
  header.innerHTML = '';
  document.getElementById('app-main').innerHTML = `
    <div class="auth-wrapper">
      <div class="auth-card page-fade">
        <div class="auth-logo">RaiseTimeLine</div>
        <div id="login-error" class="alert-error"></div>
        <div class="form-group">
          <label class="form-label" for="login-email">メールアドレス</label>
          <input class="form-input" type="email" id="login-email" placeholder="example@mail.com" autocomplete="email">
          <div class="form-error" id="err-login-email"></div>
        </div>
        <div class="form-group">
          <label class="form-label" for="login-password">パスワード</label>
          <input class="form-input" type="password" id="login-password" placeholder="パスワード" autocomplete="current-password">
          <div class="form-error" id="err-login-password"></div>
        </div>
        <button class="btn btn-primary btn-full" data-action="do-login">ログイン</button>
        <div class="auth-link">アカウントをお持ちでない方は <a href="#register">新規登録</a></div>
      </div>
    </div>`;
}

function doLogin() {
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  const errorEl = document.getElementById('login-error');
  errorEl.classList.remove('visible');
  if (!email || !password) {
    errorEl.textContent = 'メールアドレスとパスワードを入力してください。';
    errorEl.classList.add('visible');
    return;
  }
  if (!handleLogin(email, password)) {
    errorEl.textContent = 'メールアドレスまたはパスワードが正しくありません。';
    errorEl.classList.add('visible');
    return;
  }
  location.hash = '#timeline';
}

/* =====================================================
   画面: 会員登録
===================================================== */
function renderRegister() {
  const header = document.getElementById('app-header');
  header.innerHTML = '';
  document.getElementById('app-main').innerHTML = `
    <div class="auth-wrapper">
      <div class="auth-card page-fade">
        <div class="auth-logo">RaiseTimeLine</div>
        <div class="auth-title">新規アカウント登録</div>
        <div id="register-error" class="alert-error"></div>
        <div class="form-group">
          <label class="form-label" for="reg-username">ユーザー名 *</label>
          <input class="form-input" type="text" id="reg-username" maxlength="50" placeholder="ユーザー名（最大50文字）">
          <div class="form-error" id="err-reg-username"></div>
        </div>
        <div class="form-group">
          <label class="form-label" for="reg-email">メールアドレス *</label>
          <input class="form-input" type="email" id="reg-email" placeholder="example@mail.com">
          <div class="form-error" id="err-reg-email"></div>
        </div>
        <div class="form-group">
          <label class="form-label" for="reg-password">パスワード *</label>
          <input class="form-input" type="password" id="reg-password" placeholder="8文字以上">
          <div class="form-error" id="err-reg-password"></div>
        </div>
        <div class="form-group">
          <label class="form-label" for="reg-password2">パスワード（確認）*</label>
          <input class="form-input" type="password" id="reg-password2" placeholder="パスワードを再入力">
          <div class="form-error" id="err-reg-password2"></div>
        </div>
        <button class="btn btn-primary btn-full" data-action="do-register">登録する</button>
        <div class="auth-link">すでにアカウントをお持ちの方は <a href="#login">ログイン</a></div>
      </div>
    </div>`;
}

function doRegister() {
  const username = document.getElementById('reg-username').value.trim();
  const email    = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  const password2= document.getElementById('reg-password2').value;
  const errorEl  = document.getElementById('register-error');
  errorEl.classList.remove('visible');

  let valid = true;
  const setErr = (id, msg) => {
    const el = document.getElementById(id);
    if (msg) { el.textContent = msg; el.classList.add('visible'); valid = false; }
    else { el.textContent = ''; el.classList.remove('visible'); }
  };

  setErr('err-reg-username', !username ? 'ユーザー名を入力してください。' : username.length > 50 ? 'ユーザー名は50文字以内で入力してください。' : '');
  setErr('err-reg-email', !email ? 'メールアドレスを入力してください。' : db.users.some(u => u.email === email) ? 'このメールアドレスはすでに使用されています。' : '');
  setErr('err-reg-password', password.length < 8 ? 'パスワードは8文字以上で入力してください。' : '');
  setErr('err-reg-password2', password !== password2 ? 'パスワードが一致しません。' : '');

  if (!valid) return;
  handleRegister(username, email, password);
  location.hash = '#timeline';
}

/* =====================================================
   画面: タイムライン
===================================================== */
let _currentTab = 'all'; // 'all' | 'following'
let _currentProfileTab = 'posts'; // 'posts' | 'comments' | 'likes'

function renderTimeline() {
  if (!requireAuth()) return;
  renderHeader();
  clearPendingImages();
  const me = getCurrentUser();

  document.getElementById('app-main').innerHTML = `
    <div class="page-fade">
      <div class="post-form-wrapper">
        ${buildAvatarHtml(me)}
        <div class="post-form-main">
          <textarea class="post-textarea" id="post-textarea" placeholder="テキストを入力..." maxlength="280"></textarea>
          <div id="post-image-preview" class="post-image-preview" style="display:none"></div>
          <div class="post-form-divider"></div>
          <div class="post-form-footer">
            <div class="post-form-tools">
              <label class="post-tool-btn" title="画像を追加（最大4枚）">
                📷<input type="file" id="post-image-input" accept="image/*" multiple style="display:none">
              </label>
            </div>
            <div class="post-form-right">
              <span class="char-count" id="post-char-count">280</span>
              <div class="char-divider"></div>
              <button class="btn btn-primary" id="post-submit-btn" data-action="do-create-post">投稿する</button>
            </div>
          </div>
        </div>
      </div>
      <div class="tabs">
        <button class="tab-btn ${_currentTab === 'all' ? 'active' : ''}" data-action="switch-tab" data-tab="all">おすすめ</button>
        <button class="tab-btn ${_currentTab === 'following' ? 'active' : ''}" data-action="switch-tab" data-tab="following">フォロー中</button>
      </div>
      <div id="post-list"></div>
    </div>`;

  renderPostList();
  setupPostForm();
}

function setupPostForm() {
  const textarea = document.getElementById('post-textarea');
  const charCount = document.getElementById('post-char-count');
  const submitBtn = document.getElementById('post-submit-btn');
  const imageInput = document.getElementById('post-image-input');

  textarea.addEventListener('input', () => {
    const remaining = 280 - textarea.value.length;
    charCount.textContent = remaining;
    charCount.className = 'char-count' + (remaining < 0 ? ' danger' : remaining < 20 ? ' warning' : '');
    submitBtn.disabled = remaining < 0;
  });

  imageInput.addEventListener('change', () => {
    if (_pendingImages.length >= 4) {
      alert('画像は最大4枚まで添付できます。');
      imageInput.value = '';
      return;
    }
    handleImageSelect(imageInput.files, updateImagePreview);
    imageInput.value = '';
  });
}

function updateImagePreview(images) {
  const container = document.getElementById('post-image-preview');
  if (!container) return;
  if (images.length === 0) {
    container.style.display = 'none';
    container.innerHTML = '';
    return;
  }
  container.style.display = 'grid';
  container.setAttribute('data-count', images.length);
  container.innerHTML = images.map((src, i) =>
    `<div class="preview-img-wrapper" data-count="${images.length}">
      <img class="preview-img" src="${src}" alt="プレビュー${i+1}">
      <button class="preview-remove-btn" data-action="remove-preview-image" data-index="${i}">×</button>
    </div>`
  ).join('');
}

function renderPostList() {
  const me = getCurrentUser();
  const listEl = document.getElementById('post-list');
  if (!listEl || !me) return;

  let posts = [...db.posts].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  if (_currentTab === 'following') {
    const followingIds = db.follows.filter(f => f.followerId === me.id).map(f => f.followingId);
    posts = posts.filter(p => followingIds.includes(p.userId));
  }

  if (posts.length === 0) {
    listEl.innerHTML = `<div class="empty-state">
      <h3>${_currentTab === 'following' ? 'フォロー中のユーザーの投稿がありません' : 'まだ投稿がありません'}</h3>
      <p>${_currentTab === 'following' ? 'ユーザーをフォローして投稿を見てみましょう。' : '最初の投稿をしてみましょう！'}</p>
    </div>`;
    return;
  }
  listEl.innerHTML = posts.map(p => buildPostCard(p, me)).join('');
}

function doCreatePost() {
  const textarea = document.getElementById('post-textarea');
  const content = textarea.value.trim();
  if (!content && _pendingImages.length === 0) {
    alert('テキストまたは画像を入力してください。');
    return;
  }
  if (content.length > 280) {
    alert('280文字以内で入力してください。');
    return;
  }
  handleCreatePost(content, [..._pendingImages]);
  textarea.value = '';
  clearPendingImages();
  updateImagePreview([]);
  document.getElementById('post-char-count').textContent = '280';
  renderPostList();
}

/* =====================================================
   画面: 投稿詳細
===================================================== */
function renderPostDetail(postId) {
  if (!requireAuth()) return;
  renderHeader();
  const me = getCurrentUser();
  const post = db.posts.find(p => p.id === postId);
  if (!post) { render404(); return; }
  const author = getUserById(post.userId);
  if (!author) { render404(); return; }
  const isOwn = me && me.id === post.userId;
  const liked = isLikedByMe(post.id);
  const likeCount = getLikeCount(post.id);
  const comments = db.comments.filter(c => c.postId === postId).sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

  let imagesHtml = '';
  if (post.imageDataUrls && post.imageDataUrls.length > 0) {
    const count = post.imageDataUrls.length;
    const imgItems = post.imageDataUrls.map((src, i) =>
      `<div class="post-img-wrap">
        <img class="post-img" src="${src}" alt="投稿画像${i+1}" data-action="view-image" data-src="${src}">
      </div>`
    ).join('');
    imagesHtml = `<div class="post-images" data-count="${count}">${imgItems}</div>`;
  }

  const menuHtml = isOwn ? `
    <div class="post-detail-meta-menu">
      <div class="post-menu-btn" data-action="toggle-post-menu" data-post-id="${post.id}">⋯
        <div class="post-menu-dropdown hidden" id="post-menu-${post.id}">
          <button class="post-menu-item" data-action="edit-post" data-post-id="${post.id}">編集</button>
          <button class="post-menu-item danger" data-action="delete-post" data-post-id="${post.id}">削除</button>
        </div>
      </div>
    </div>` : '';

  const commentsHtml = comments.length === 0
    ? '<div class="empty-comments">コメントはまだありません。最初のコメントを投稿しましょう！</div>'
    : comments.map(c => buildCommentItem(c, me)).join('');

  document.getElementById('app-main').innerHTML = `
    <div class="page-fade">
      <div class="page-header">
        <button class="back-btn" data-action="go-back">←</button>
        <h2>投稿</h2>
      </div>
      <div class="post-detail-card">
        <div class="post-detail-user">
          ${buildAvatarHtml(author, 'avatar-lg')}
          <div style="flex:1;min-width:0;">
            <div style="display:flex;align-items:baseline;gap:8px;justify-content:space-between;">
              <span class="post-detail-username" data-action="go-profile" data-user-id="${author.id}" style="cursor:pointer">${escapeHtml(author.username)}</span>
              <span class="post-date" style="flex-shrink:0;">${formatDate(post.createdAt)}</span>
            </div>
          </div>
          ${menuHtml}
        </div>
        <div class="post-detail-content">${escapeHtml(post.content)}</div>
        ${imagesHtml}
        <div class="post-detail-actions">
          <button class="action-btn ${liked ? 'liked' : ''}" data-action="toggle-like" data-post-id="${post.id}">
            <span class="action-icon">${liked ? '♥' : '♡'}</span>
            <span id="like-count-${post.id}">${likeCount}</span>&nbsp;いいね
          </button>
          <button class="action-btn" style="cursor:default">
            <span class="action-icon">💬</span>
            <span id="comment-count-${post.id}">${comments.length}</span>&nbsp;コメント
          </button>
        </div>
      </div>
      <div class="comment-section-title">コメント (${comments.length}件)</div>
      <div class="comment-form-wrapper">
        ${buildAvatarHtml(me)}
        <textarea class="comment-textarea" id="comment-textarea" placeholder="コメントを入力..." rows="2"></textarea>
        <button class="btn btn-primary comment-form-btn" data-action="do-create-comment" data-post-id="${post.id}">コメントする</button>
      </div>
      <div id="comment-list">${commentsHtml}</div>
    </div>`;
}

function doCreateComment(postId) {
  const textarea = document.getElementById('comment-textarea');
  const content = textarea.value.trim();
  if (!content) { alert('コメントを入力してください。'); return; }
  handleCreateComment(postId, content);
  renderPostDetail(postId);
}

function refreshCommentList(postId) {
  const me = getCurrentUser();
  const comments = db.comments.filter(c => c.postId === postId).sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  const listEl = document.getElementById('comment-list');
  if (listEl) {
    listEl.innerHTML = comments.length === 0
      ? '<div class="empty-comments">コメントはまだありません。最初のコメントを投稿しましょう！</div>'
      : comments.map(c => buildCommentItem(c, me)).join('');
  }
  const titleEl = document.querySelector('.comment-section-title');
  if (titleEl) titleEl.textContent = `コメント (${comments.length}件)`;
  const countEl = document.getElementById(`comment-count-${postId}`);
  if (countEl) countEl.textContent = comments.length;
}

/* =====================================================
   画面: ユーザー検索
===================================================== */
function renderSearch() {
  if (!requireAuth()) return;
  renderHeader();
  document.getElementById('app-main').innerHTML = `
    <div class="page-fade">
      <div class="search-wrapper">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
          <input class="search-input" type="text" id="search-input" placeholder="ユーザー名を入力..." autocomplete="off">
        </div>
        <div id="search-results" class="search-results">
          <div class="search-hint">ユーザー名を入力して検索してください。</div>
        </div>
      </div>
    </div>`;

  const input = document.getElementById('search-input');
  input.addEventListener('input', debounce(doSearch, 300));
  input.focus();
}

function doSearch() {
  const query = document.getElementById('search-input').value.trim();
  const resultsEl = document.getElementById('search-results');
  if (!resultsEl) return;
  if (!query) {
    resultsEl.innerHTML = '<div class="search-hint">ユーザー名を入力して検索してください。</div>';
    return;
  }
  const me = getCurrentUser();
  const results = db.users.filter(u => u.username.toLowerCase().includes(query.toLowerCase()) && u.id !== (me ? me.id : ''));
  if (results.length === 0) {
    resultsEl.innerHTML = '<div class="search-empty">ユーザーが見つかりませんでした。</div>';
    return;
  }
  resultsEl.innerHTML = results.map(u => buildUserCard(u, true)).join('');
}

/* =====================================================
   画面: プロフィール
===================================================== */
function renderProfile(userId) {
  if (!requireAuth()) return;
  renderHeader();
  const me = getCurrentUser();
  const user = getUserById(userId);
  if (!user) { render404(); return; }
  const isMe = me && me.id === userId;
  const following = isFollowing(userId);
  const followerCount = getFollowerCount(userId);
  const followingCount = getFollowingCount(userId);

  const cache = buildProfileTabData(userId);

  let actionBtn = '';
  if (isMe) {
    actionBtn = `<button class="btn btn-outline" data-action="go-edit-profile" data-user-id="${userId}">プロフィール編集</button>`;
  } else {
    actionBtn = following
      ? `<button class="btn-unfollow" data-action="toggle-follow" data-user-id="${userId}" id="follow-btn">フォロー解除</button>`
      : `<button class="btn-follow" data-action="toggle-follow" data-user-id="${userId}" id="follow-btn">フォローする</button>`;
  }

  const tabContentHtml = buildProfileTabContent(_currentProfileTab, userId, me, cache);

  document.getElementById('app-main').innerHTML = `
    <div class="page-fade">
      <div class="page-header">
        <button class="back-btn" data-action="go-back">←</button>
        <h2>${escapeHtml(user.username)}</h2>
      </div>
      <div class="profile-header">
        <div class="profile-top">
          <div class="avatar avatar-xl">
            <img src="${avatarSrc(user)}" alt="${escapeHtml(user.username)}">
          </div>
          ${actionBtn}
        </div>
        <div class="profile-username">${escapeHtml(user.username)}</div>
        ${user.bio ? `<div class="profile-bio">${escapeHtml(user.bio)}</div>` : ''}
        <div class="profile-stats">
          <div class="profile-stat" data-action="show-following" data-user-id="${userId}">
            <span class="profile-stat-num">${followingCount}</span>
            <span class="profile-stat-label">フォロー中</span>
          </div>
          <div class="profile-stat" data-action="show-followers" data-user-id="${userId}">
            <span class="profile-stat-num">${followerCount}</span>
            <span class="profile-stat-label">フォロワー</span>
          </div>
        </div>
      </div>
      <div class="profile-tabs tabs" data-user-id="${userId}">
        <button class="tab-btn profile-tab-btn ${_currentProfileTab === 'posts' ? 'active' : ''}" data-action="switch-profile-tab" data-tab="posts">投稿 (${cache.userPosts.length})</button>
        <button class="tab-btn profile-tab-btn ${_currentProfileTab === 'comments' ? 'active' : ''}" data-action="switch-profile-tab" data-tab="comments">コメント (${cache.userComments.length})</button>
        <button class="tab-btn profile-tab-btn ${_currentProfileTab === 'likes' ? 'active' : ''}" data-action="switch-profile-tab" data-tab="likes">いいね (${cache.likedPosts.length})</button>
      </div>
      <div id="profile-tab-content">${tabContentHtml}</div>
    </div>`;
}

function buildProfileTabContent(tab, userId, me, cache) {
  if (tab === 'posts') {
    if (cache.userPosts.length === 0) return '<div class="profile-empty">まだ投稿がありません。</div>';
    return cache.userPosts.map(p => buildPostCard(p, me, cache)).join('');
  }
  if (tab === 'comments') {
    if (cache.userComments.length === 0) return '<div class="profile-empty">まだコメントがありません。</div>';
    return cache.userComments.map(c => buildProfileCommentItem(c, cache.commentPostMap.get(c.postId) || null, me, cache)).join('');
  }
  if (tab === 'likes') {
    if (cache.likedPosts.length === 0) return '<div class="profile-empty">まだいいねした投稿がありません。</div>';
    return cache.likedPosts.map(p => buildPostCard(p, me, cache)).join('');
  }
  return '';
}

/* =====================================================
   画面: プロフィール編集
===================================================== */
let _editAvatarDataUrl = null;

function renderProfileEdit(userId) {
  if (!requireAuth()) return;
  const me = getCurrentUser();
  if (!me || me.id !== userId) { location.hash = `#users/${userId}`; return; }
  renderHeader();
  _editAvatarDataUrl = me.avatarDataUrl || null;

  document.getElementById('app-main').innerHTML = `
    <div class="page-fade">
      <div class="page-header">
        <button class="back-btn" data-action="go-back">←</button>
        <h2>プロフィール編集</h2>
      </div>
      <div class="edit-profile-wrapper">
        <div class="edit-avatar-section">
          <div class="edit-avatar-label">アバター画像</div>
          <div class="edit-avatar-row">
            <div class="edit-avatar-preview" id="edit-avatar-preview">
              <img src="${avatarSrc(me)}" alt="アバター" id="edit-avatar-img">
            </div>
            <label class="btn btn-outline" style="cursor:pointer">
              画像を変更する
              <input type="file" id="edit-avatar-input" accept="image/*" style="display:none">
            </label>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label" for="edit-username">ユーザー名 *</label>
          <input class="form-input" type="text" id="edit-username" maxlength="50" value="${escapeHtml(me.username)}">
          <div class="form-error" id="err-edit-username"></div>
        </div>
        <div class="form-group">
          <label class="form-label" for="edit-bio">自己紹介</label>
          <textarea class="form-input edit-textarea" id="edit-bio" maxlength="160" rows="3">${escapeHtml(me.bio || '')}</textarea>
          <div class="bio-char-count"><span id="bio-char-count">${160 - (me.bio || '').length}</span> 文字残り</div>
        </div>
        <div class="edit-form-actions">
          <button class="btn btn-outline" data-action="go-back">キャンセル</button>
          <button class="btn btn-primary" data-action="do-save-profile" data-user-id="${userId}">保存する</button>
        </div>
      </div>
    </div>`;

  document.getElementById('edit-bio').addEventListener('input', (e) => {
    document.getElementById('bio-char-count').textContent = 160 - e.target.value.length;
  });

  document.getElementById('edit-avatar-input').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      _editAvatarDataUrl = ev.target.result;
      document.getElementById('edit-avatar-img').src = _editAvatarDataUrl;
    };
    reader.readAsDataURL(file);
  });
}

function doSaveProfile(userId) {
  const username = document.getElementById('edit-username').value.trim();
  const bio      = document.getElementById('edit-bio').value.trim();
  const errEl    = document.getElementById('err-edit-username');
  if (!username) { errEl.textContent = 'ユーザー名を入力してください。'; errEl.classList.add('visible'); return; }
  if (username.length > 50) { errEl.textContent = 'ユーザー名は50文字以内で入力してください。'; errEl.classList.add('visible'); return; }
  errEl.classList.remove('visible');

  const user = db.users.find(u => u.id === userId);
  if (!user) return;
  user.username = username;
  user.bio = bio;
  if (_editAvatarDataUrl !== null) user.avatarDataUrl = _editAvatarDataUrl;
  saveDb();
  _editAvatarDataUrl = null;
  location.hash = `#users/${userId}`;
}

/* =====================================================
   画面: 404
===================================================== */
function render404() {
  renderHeader();
  document.getElementById('app-main').innerHTML = `
    <div class="not-found page-fade">
      <h2>404</h2>
      <p>ページが見つかりませんでした。</p>
      <button class="btn btn-primary" data-action="go-home">ホームへ戻る</button>
    </div>`;
}

/* =====================================================
   モーダル
===================================================== */
function openModal(...ids) {
  document.getElementById('modal-overlay').classList.remove('hidden');
  ids.forEach(id => document.getElementById(id).classList.remove('hidden'));
}

function closeAllModals() {
  document.getElementById('modal-overlay').classList.add('hidden');
  ['modal-follow-list','modal-image-viewer','modal-edit-post','modal-edit-comment'].forEach(id => {
    document.getElementById(id).classList.add('hidden');
  });
  _editingPostId = null;
  _editingCommentId = null;
}

function showFollowersModal(userId) {
  const user = getUserById(userId);
  if (!user) return;
  const followers = getFollowers(userId);
  document.getElementById('modal-follow-title').textContent = 'フォロワー';
  document.getElementById('modal-follow-body').innerHTML = followers.length === 0
    ? '<div class="empty-state" style="padding:24px;"><p>フォロワーはまだいません。</p></div>'
    : followers.map(u => `<div class="modal-user-item" data-action="go-profile" data-user-id="${u.id}">
        ${buildAvatarHtml(u)}
        <span class="modal-user-name">${escapeHtml(u.username)}</span>
      </div>`).join('');
  openModal('modal-follow-list');
}

function showFollowingModal(userId) {
  const user = getUserById(userId);
  if (!user) return;
  const following = getFollowing(userId);
  document.getElementById('modal-follow-title').textContent = 'フォロー中';
  document.getElementById('modal-follow-body').innerHTML = following.length === 0
    ? '<div class="empty-state" style="padding:24px;"><p>フォロー中のユーザーはいません。</p></div>'
    : following.map(u => `<div class="modal-user-item" data-action="go-profile" data-user-id="${u.id}">
        ${buildAvatarHtml(u)}
        <span class="modal-user-name">${escapeHtml(u.username)}</span>
      </div>`).join('');
  openModal('modal-follow-list');
}

function showImageViewer(src) {
  document.getElementById('modal-image-src').src = src;
  openModal('modal-image-viewer');
}

let _editingPostId = null;
function showEditPostModal(postId) {
  const post = db.posts.find(p => p.id === postId);
  if (!post) return;
  _editingPostId = postId;
  const textarea = document.getElementById('edit-post-textarea');
  textarea.value = post.content;
  document.getElementById('edit-post-char-count').textContent = 280 - post.content.length;
  openModal('modal-edit-post');
  textarea.focus();
}

let _editingCommentId = null;
function showEditCommentModal(commentId) {
  const comment = db.comments.find(c => c.id === commentId);
  if (!comment) return;
  _editingCommentId = commentId;
  const textarea = document.getElementById('edit-comment-textarea');
  textarea.value = comment.content;
  openModal('modal-edit-comment');
  textarea.focus();
}

/* =====================================================
   ルーター
===================================================== */
function route() {
  const hash = location.hash || '';
  closeAllModals();

  // 全ドロップダウンを閉じる
  document.querySelectorAll('.dropdown-menu, .post-menu-dropdown').forEach(el => el.classList.add('hidden'));

  const routes = [
    { pattern: /^#login$/,             handler: () => renderLogin() },
    { pattern: /^#register$/,          handler: () => renderRegister() },
    { pattern: /^(#timeline)?$/,       handler: () => { _currentTab = 'all'; renderTimeline(); } },
    { pattern: /^#post\/(.+)$/,        handler: (m) => renderPostDetail(m[1]) },
    { pattern: /^#search$/,            handler: () => renderSearch() },
    { pattern: /^#users\/(.+)\/edit$/, handler: (m) => renderProfileEdit(m[1]) },
    { pattern: /^#users\/(.+)$/,       handler: (m) => { _currentProfileTab = 'posts'; renderProfile(m[1]); } },
  ];

  for (const r of routes) {
    const m = hash.match(r.pattern);
    if (m) { r.handler(m); return; }
  }
  render404();
}

/* =====================================================
   グローバルイベント委譲
===================================================== */
document.addEventListener('click', (e) => {
  // ドロップダウン・メニュー外クリックで閉じる
  if (!e.target.closest('[data-action="toggle-user-menu"]') && !e.target.closest('#user-dropdown')) {
    const dd = document.getElementById('user-dropdown');
    if (dd) dd.classList.add('hidden');
  }
  if (!e.target.closest('[data-action^="toggle-post-menu"]') && !e.target.closest('[data-action^="toggle-comment-menu"]')) {
    document.querySelectorAll('.post-menu-dropdown').forEach(el => el.classList.add('hidden'));
  }

  const el = e.target.closest('[data-action]');
  if (!el) return;
  const action = el.dataset.action;
  e.stopPropagation();

  switch (action) {
    // ナビゲーション
    case 'go-home':       location.hash = '#timeline'; break;
    case 'go-search':     location.hash = '#search'; break;
    case 'go-back':       history.back(); break;
    case 'go-post-detail': {
      const card = el.closest('.post-card') || el.closest('[data-post-id]');
      const postId = el.dataset.postId || (card && card.dataset.postId);
      if (postId) location.hash = `#post/${postId}`;
      break;
    }
    case 'go-profile': {
      const uid = el.dataset.userId;
      if (uid) { closeAllModals(); location.hash = `#users/${uid}`; }
      break;
    }
    case 'go-my-profile': {
      const me = getCurrentUser();
      if (me) { document.getElementById('user-dropdown').classList.add('hidden'); location.hash = `#users/${me.id}`; }
      break;
    }
    case 'go-edit-profile': {
      const uid = el.dataset.userId;
      if (uid) location.hash = `#users/${uid}/edit`;
      break;
    }

    // 認証
    case 'do-login':    doLogin(); break;
    case 'do-register': doRegister(); break;
    case 'logout':      handleLogout(); break;

    // ヘッダードロップダウン
    case 'toggle-user-menu': {
      const dd = document.getElementById('user-dropdown');
      if (dd) dd.classList.toggle('hidden');
      break;
    }

    // タブ
    case 'switch-tab': {
      _currentTab = el.dataset.tab;
      document.querySelectorAll('.tab-btn:not(.profile-tab-btn)').forEach(b => b.classList.toggle('active', b.dataset.tab === _currentTab));
      renderPostList();
      break;
    }

    // プロフィールタブ
    case 'switch-profile-tab': {
      _currentProfileTab = el.dataset.tab;
      document.querySelectorAll('.profile-tab-btn').forEach(b =>
        b.classList.toggle('active', b.dataset.tab === _currentProfileTab)
      );
      const tabContainer = el.closest('[data-user-id]');
      const profileUserId = tabContainer ? tabContainer.dataset.userId : null;
      if (!profileUserId) break;
      const profileMe = getCurrentUser();
      const profileCache = buildProfileTabData(profileUserId);
      const contentEl = document.getElementById('profile-tab-content');
      if (contentEl) contentEl.innerHTML = buildProfileTabContent(_currentProfileTab, profileUserId, profileMe, profileCache);
      break;
    }

    // 投稿
    case 'do-create-post': doCreatePost(); break;
    case 'toggle-post-menu': {
      const postId = el.dataset.postId;
      const menu = document.getElementById(`post-menu-${postId}`);
      if (menu) { menu.classList.toggle('hidden'); e.stopPropagation(); }
      break;
    }
    case 'edit-post': {
      const postId = el.dataset.postId;
      document.querySelectorAll('.post-menu-dropdown').forEach(m => m.classList.add('hidden'));
      showEditPostModal(postId);
      break;
    }
    case 'save-edit-post': {
      if (!_editingPostId) break;
      const newContent = document.getElementById('edit-post-textarea').value.trim();
      if (!newContent) { alert('投稿内容を入力してください。'); break; }
      if (newContent.length > 280) { alert('280文字以内で入力してください。'); break; }
      const savedPostId = _editingPostId; // closeAllModals でリセットされる前に退避
      handleEditPost(savedPostId, newContent);
      closeAllModals();
      // 現在の画面を更新
      if (location.hash.startsWith('#post/')) {
        renderPostDetail(savedPostId);
      } else {
        renderPostList();
      }
      break;
    }
    case 'delete-post': {
      const postId = el.dataset.postId;
      document.querySelectorAll('.post-menu-dropdown').forEach(m => m.classList.add('hidden'));
      if (!confirm('この投稿を削除しますか？')) break;
      handleDeletePost(postId);
      if (location.hash === `#post/${postId}`) {
        location.hash = '#timeline';
      } else {
        renderPostList();
      }
      break;
    }

    // いいね
    case 'toggle-like': {
      const postId = el.dataset.postId;
      if (!postId) break;
      e.preventDefault();
      toggleLike(postId);
      const liked = isLikedByMe(postId);
      const count = getLikeCount(postId);
      // ボタン状態更新（複数箇所対応）
      document.querySelectorAll(`[data-action="toggle-like"][data-post-id="${postId}"]`).forEach(btn => {
        btn.classList.toggle('liked', liked);
        const icon = btn.querySelector('.action-icon');
        if (icon) icon.textContent = liked ? '♥' : '♡';
        const cnt = btn.querySelector('.action-count') || document.getElementById(`like-count-${postId}`);
        if (cnt) cnt.textContent = count;
      });
      const likeCountEl = document.getElementById(`like-count-${postId}`);
      if (likeCountEl) likeCountEl.textContent = count;
      break;
    }

    // コメント
    case 'do-create-comment': {
      const postId = el.dataset.postId;
      if (postId) doCreateComment(postId);
      break;
    }
    case 'toggle-comment-menu': {
      const commentId = el.dataset.commentId;
      const menu = document.getElementById(`comment-menu-${commentId}`);
      if (menu) { menu.classList.toggle('hidden'); e.stopPropagation(); }
      break;
    }
    case 'edit-comment': {
      const commentId = el.dataset.commentId;
      document.querySelectorAll('.post-menu-dropdown').forEach(m => m.classList.add('hidden'));
      showEditCommentModal(commentId);
      break;
    }
    case 'save-edit-comment': {
      if (!_editingCommentId) break;
      const newContent = document.getElementById('edit-comment-textarea').value.trim();
      if (!newContent) { alert('コメント内容を入力してください。'); break; }
      const comment = db.comments.find(c => c.id === _editingCommentId);
      const postId = comment ? comment.postId : null;
      handleEditComment(_editingCommentId, newContent);
      closeAllModals();
      if (postId) refreshCommentList(postId);
      break;
    }
    case 'delete-comment': {
      const commentId = el.dataset.commentId;
      document.querySelectorAll('.post-menu-dropdown').forEach(m => m.classList.add('hidden'));
      if (!confirm('このコメントを削除しますか？')) break;
      const comment = db.comments.find(c => c.id === commentId);
      const postId = comment ? comment.postId : null;
      handleDeleteComment(commentId);
      if (postId) refreshCommentList(postId);
      break;
    }

    // フォロー
    case 'toggle-follow': {
      const targetId = el.dataset.userId;
      if (!targetId) break;
      toggleFollow(targetId);
      const nowFollowing = isFollowing(targetId);
      // ボタンテキスト・スタイル切り替え
      document.querySelectorAll(`[data-action="toggle-follow"][data-user-id="${targetId}"]`).forEach(btn => {
        if (nowFollowing) {
          btn.textContent = 'フォロー解除';
          btn.className = 'btn-unfollow';
        } else {
          btn.textContent = 'フォローする';
          btn.className = 'btn-follow';
        }
        btn.dataset.action = 'toggle-follow';
        btn.dataset.userId = targetId;
      });
      // プロフィール画面のフォロワー数を更新
      const followerNumEl = document.querySelector(`[data-action="show-followers"][data-user-id="${targetId}"] .profile-stat-num`);
      if (followerNumEl) followerNumEl.textContent = getFollowerCount(targetId);
      break;
    }
    case 'show-followers': {
      const uid = el.dataset.userId;
      if (uid) showFollowersModal(uid);
      break;
    }
    case 'show-following': {
      const uid = el.dataset.userId;
      if (uid) showFollowingModal(uid);
      break;
    }
    case 'do-save-profile': {
      const uid = el.dataset.userId;
      if (uid) doSaveProfile(uid);
      break;
    }

    // 画像
    case 'view-image': {
      const src = el.dataset.src;
      if (src) showImageViewer(src);
      break;
    }
    case 'remove-preview-image': {
      const index = parseInt(el.dataset.index, 10);
      removePendingImage(index);
      updateImagePreview(_pendingImages);
      break;
    }

    // モーダル閉じる
    case 'close-modal': closeAllModals(); break;

    // 投稿カード全体クリック（アクション以外の部分）
    default: {
      const card = e.target.closest('.post-card');
      if (card && !e.target.closest('[data-action]') && !e.target.closest('.post-menu-dropdown')) {
        const postId = card.dataset.postId;
        if (postId) location.hash = `#post/${postId}`;
      }
    }
  }
});

// 投稿カード本体クリック（data-action なしの領域）
document.getElementById('app-main').addEventListener('click', (e) => {
  if (e.target.closest('[data-action]')) return;
  const card = e.target.closest('.post-card');
  if (card) {
    const postId = card.dataset.postId;
    if (postId) location.hash = `#post/${postId}`;
  }
});

// 投稿編集テキストエリアの文字数更新
document.getElementById('edit-post-textarea').addEventListener('input', (e) => {
  const el = document.getElementById('edit-post-char-count');
  if (el) el.textContent = 280 - e.target.value.length;
});

/* =====================================================
   起動
===================================================== */
initDb();
initDummyData();
window.addEventListener('hashchange', route);
route();
