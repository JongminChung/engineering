✖ biome check --write --unsafe:
perf-test/app/src/db/index.ts:5:27 lint/style/noNonNullAssertion ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
! Forbidden non-null assertion.
3 │ import \* as schema from './schema.ts'
4 │

> 5 │ export const db = drizzle(process.env.DATABASE_URL!, { schema })
> │ ^^^^^^^^^^^^^^^^^^^^^^^^^
> 6 │
> perf-test/app/src/integrations/better-auth/header-user.tsx:17:11 lint/performance/noImgElement ━━━━━━━━━━
> ! Don't use <img> element.
> 15 │ <div className="flex items-center gap-2">
> 16 │ {session.user.image ? (
> 17 │ <img src={session.user.image} alt="" className="h-8 w-8" />
> │ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
> 18 │ ) : (
> 19 │ <div className="h-8 w-8 bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center">
> i Using the <img> can lead to slower LCP and higher bandwidth. Consider using <Image /> from next/image to automatically optimize images.
> perf-test/app/src/routeTree.gen.ts:30:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 28 │ path: '/',
> 29 │ getParentRoute: () => rootRouteImport,
> 30 │ } as any)
> │ ^^^
> 31 │ const DemoTanstackQueryRoute = DemoTanstackQueryRouteImport.update({
> 32 │ id: '/demo/tanstack-query',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:35:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 33 │ path: '/demo/tanstack-query',
> 34 │ getParentRoute: () => rootRouteImport,
> 35 │ } as any)
> │ ^^^
> 36 │ const DemoDrizzleRoute = DemoDrizzleRouteImport.update({
> 37 │ id: '/demo/drizzle',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:40:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 38 │ path: '/demo/drizzle',
> 39 │ getParentRoute: () => rootRouteImport,
> 40 │ } as any)
> │ ^^^
> 41 │ const DemoBetterAuthRoute = DemoBetterAuthRouteImport.update({
> 42 │ id: '/demo/better-auth',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:45:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 43 │ path: '/demo/better-auth',
> 44 │ getParentRoute: () => rootRouteImport,
> 45 │ } as any)
> │ ^^^
> 46 │ const DemoStartServerFuncsRoute = DemoStartServerFuncsRouteImport.update({
> 47 │ id: '/demo/start/server-funcs',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:50:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 48 │ path: '/demo/start/server-funcs',
> 49 │ getParentRoute: () => rootRouteImport,
> 50 │ } as any)
> │ ^^^
> 51 │ const DemoStartApiRequestRoute = DemoStartApiRequestRouteImport.update({
> 52 │ id: '/demo/start/api-request',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:55:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 53 │ path: '/demo/start/api-request',
> 54 │ getParentRoute: () => rootRouteImport,
> 55 │ } as any)
> │ ^^^
> 56 │ const DemoApiTqTodosRoute = DemoApiTqTodosRouteImport.update({
> 57 │ id: '/demo/api/tq-todos',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:60:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 58 │ path: '/demo/api/tq-todos',
> 59 │ getParentRoute: () => rootRouteImport,
> 60 │ } as any)
> │ ^^^
> 61 │ const DemoApiNamesRoute = DemoApiNamesRouteImport.update({
> 62 │ id: '/demo/api/names',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:65:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 63 │ path: '/demo/api/names',
> 64 │ getParentRoute: () => rootRouteImport,
> 65 │ } as any)
> │ ^^^
> 66 │ const ApiAuthSplatRoute = ApiAuthSplatRouteImport.update({
> 67 │ id: '/api/auth/$',
i any disables many type checking rules. Its use should be avoided.
perf-test/app/src/routeTree.gen.ts:70:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
! Unexpected any. Specify a different type.
68 │   path: '/api/auth/$',
> 69 │ getParentRoute: () => rootRouteImport,
> 70 │ } as any)
> │ ^^^
> 71 │ const DemoStartSsrIndexRoute = DemoStartSsrIndexRouteImport.update({
> 72 │ id: '/demo/start/ssr/',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:75:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 73 │ path: '/demo/start/ssr/',
> 74 │ getParentRoute: () => rootRouteImport,
> 75 │ } as any)
> │ ^^^
> 76 │ const DemoStartSsrSpaModeRoute = DemoStartSsrSpaModeRouteImport.update({
> 77 │ id: '/demo/start/ssr/spa-mode',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:80:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 78 │ path: '/demo/start/ssr/spa-mode',
> 79 │ getParentRoute: () => rootRouteImport,
> 80 │ } as any)
> │ ^^^
> 81 │ const DemoStartSsrFullSsrRoute = DemoStartSsrFullSsrRouteImport.update({
> 82 │ id: '/demo/start/ssr/full-ssr',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:85:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 83 │ path: '/demo/start/ssr/full-ssr',
> 84 │ getParentRoute: () => rootRouteImport,
> 85 │ } as any)
> │ ^^^
> 86 │ const DemoStartSsrDataOnlyRoute = DemoStartSsrDataOnlyRouteImport.update({
> 87 │ id: '/demo/start/ssr/data-only',
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routeTree.gen.ts:90:6 lint/suspicious/noExplicitAny ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Unexpected any. Specify a different type.
> 88 │ path: '/demo/start/ssr/data-only',
> 89 │ getParentRoute: () => rootRouteImport,
> 90 │ } as any)
> │ ^^^
> 91 │
> 92 │ export interface FileRoutesByFullPath {
> i any disables many type checking rules. Its use should be avoided.
> perf-test/app/src/routes/demo/better-auth.tsx:41:15 lint/performance/noImgElement ━━━━━━━━━━━━━━━━━━
> ! Don't use <img> element.
> 39 │ <div className="flex items-center gap-3">
> 40 │ {session.user.image ? (
> 41 │ <img src={session.user.image} alt="" className="h-10 w-10" />
> │ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
> 42 │ ) : (
> 43 │ <div className="h-10 w-10 bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center">
> i Using the <img> can lead to slower LCP and higher bandwidth. Consider using <Image /> from next/image to automatically optimize images.
> perf-test/app/src/routes/demo/drizzle.tsx:76:15 lint/performance/noImgElement ━━━━━━━━━━━━━━━━━━━━━━
> ! Don't use <img> element.
> 74 │ <div className="absolute -inset-2 bg-gradient-to-r from-indigo-500 via-purple-500 to-indigo-500 rounded-lg blur-lg opacity-60 group-hover:opacity-100 transition duration-500"></div>
> 75 │ <div className="relative bg-gradient-to-br from-indigo-600 to-purple-600 p-3 rounded-lg">
> 76 │ <img
> │ ^^^^
> 77 │ src="/drizzle.svg"
> 78 │ alt="Drizzle Logo"
> 79 │ className="w-8 h-8 transform group-hover:scale-110 transition-transform duration-300"
> 80 │ />
> │ ^^
> 81 │ </div>
> 82 │ </div>
> i Using the <img> can lead to slower LCP and higher bandwidth. Consider using <Image /> from next/image to automatically optimize images.
> perf-test/app/src/routes/index.tsx:59:13 lint/performance/noImgElement ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
> ! Don't use <img> element.
> 57 │ <div className="relative max-w-5xl mx-auto">
> 58 │ <div className="flex items-center justify-center gap-6 mb-6">
> 59 │ <img
> │ ^^^^
> 60 │ src="/tanstack-circle-logo.png"
> 61 │ alt="TanStack Logo"
> 62 │ className="w-24 h-24 md:w-32 md:h-32"
> 63 │ />
> │ ^^
> 64 │ <h1 className="text-6xl md:text-7xl font-black text-white [letter-spacing:-0.08em]">
> 65 │ <span className="text-gray-300">TANSTACK</span>{' '}
> i Using the <img> can lead to slower LCP and higher bandwidth. Consider using <Image /> from next/image to automatically optimize images.
> perf-test/app/src/integrations/better-auth/header-user.tsx:25:9 lint/a11y/useButtonType ━━━━━━━━━━━━
> × Provide an explicit type prop for the button element.
> 23 │ </div>
> 24 │ )}
> 25 │ <button
> │ ^^^^^^^
> 26 │ onClick={() => authClient.signOut()}
> 27 │ className="flex-1 h-9 px-4 text-sm font-medium bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 border border-neutral-300 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
> 28 │ >
> │ ^
> 29 │ Sign out
> 30 │ </button>
> i The default type of a button is submit, which causes the submission of a form when placed inside a `form` element. This is likely not the behaviour that you want inside a React application.
> i Allowed button types are: submit, button or reset
> perf-test/app/src/routes/demo/better-auth.tsx:59:11 lint/a11y/useButtonType ━━━━━━━━━━━━━━━━━━━━━━━━
> × Provide an explicit type prop for the button element.
> 57 │ </div>
> 58 │
> 59 │ <button
> │ ^^^^^^^
> 60 │ onClick={() => authClient.signOut()}
> 61 │ className="w-full h-9 px-4 text-sm font-medium border border-neutral-300 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
> 62 │ >
> │ ^
> 63 │ Sign out
> 64 │ </button>
> i The default type of a button is submit, which causes the submission of a form when placed inside a `form` element. This is likely not the behaviour that you want inside a React application.
> i Allowed button types are: submit, button or reset
