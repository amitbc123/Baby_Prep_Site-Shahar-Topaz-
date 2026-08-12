import { createHashRouter } from 'react-router'
import { lazy, Suspense } from 'react'
import { RootLayout } from '@/app/layout'
import { PageLoading } from '@/components/layout/page-loading'

const HomePage = lazy(() => import('@/features/home/home-page'))
const ShoppingPage = lazy(() => import('@/features/shopping/shopping-page'))
const TasksPage = lazy(() => import('@/features/tasks/tasks-page'))
const DatesPage = lazy(() => import('@/features/dates/dates-page'))
const SettingsPage = lazy(() => import('@/features/settings/settings-page'))

function withSuspense(el: React.ReactNode) {
  return <Suspense fallback={<PageLoading />}>{el}</Suspense>
}

export const router = createHashRouter([
  {
    element: <RootLayout />,
    children: [
      { path: '/', element: withSuspense(<HomePage />) },
      { path: '/shopping', element: withSuspense(<ShoppingPage />) },
      { path: '/tasks', element: withSuspense(<TasksPage />) },
      { path: '/dates', element: withSuspense(<DatesPage />) },
      { path: '/settings', element: withSuspense(<SettingsPage />) },
    ],
  },
])
