import React, { useState } from 'react';

const Layout = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  return (
    <div className="flex h-screen w-full bg-gray-100 overflow-hidden">
      {/* 左侧列：个人信息 + 可折叠 */}
      <aside
        className={`bg-white border-r border-gray-200 transition-all duration-300 ease-in-out flex flex-col shrink-0 ${
          isSidebarOpen ? 'w-64' : 'w-0 -translate-x-full'
        }`}
      >
        <div className="p-4 overflow-y-auto h-full">
          <h2 className="font-bold text-xl mb-4">个人信息</h2>
          <div className="space-y-4">
            <div className="w-20 h-20 bg-blue-500 rounded-full mx-auto" />
            <p className="text-center text-gray-600">用户名：Developer</p>
            {/* 更多个人信息内容 */}
          </div>
        </div>
      </aside>

      {/* 侧边栏切换按钮 - 悬浮在布局之上或嵌入边缘 */}
      <button
        onClick={() => setIsSidebarOpen(!isSidebarOpen)}
        className="fixed bottom-4 left-4 z-50 p-2 bg-indigo-600 text-white rounded-full shadow-lg hover:bg-indigo-700 transition-colors"
      >
        {isSidebarOpen ? '←' : '→'}
      </button>

      {/* 中间列：搜索框固定 + 内容滚动 */}
      <main className="flex-1 flex flex-col min-w-0 bg-white">
        {/* 固定搜索框 */}
        <header className="h-16 border-b border-gray-200 flex items-center px-6 sticky top-0 bg-white z-10">
          <div className="w-full max-w-2xl mx-auto">
            <input
              type="text"
              placeholder="搜索内容..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </header>

        {/* 可扩展内容区 */}
        <section className="flex-1 overflow-y-auto p-6 space-y-4">
          <div className="max-w-4xl mx-auto">
            <h1 className="text-2xl font-bold mb-4">主要内容区域</h1>
            {/* 模拟长内容 */}
            {Array.from({ length: 20 }).map((_, i) => (
              <div key={i} className="p-4 bg-gray-50 border rounded-lg mb-4">
                这是第 {i + 1} 条内容动态，支持向下无限扩展滚动。
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* 右侧列：固定不动 */}
      <aside className="hidden lg:block w-80 border-l border-gray-200 bg-white shrink-0">
        <div className="p-4">
          <h3 className="font-semibold text-gray-700 mb-4">推荐/通知</h3>
          <div className="space-y-4 text-sm text-gray-500">
             <p>这里是固定不动的右侧区域。</p>
             <p>适合放置广告、推荐列表或快捷工具。</p>
          </div>
        </div>
      </aside>
    </div>
  );
};

export default Layout;