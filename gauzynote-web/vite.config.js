import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path';
import autoprefixer from "autoprefixer";
import viteCompression from 'vite-plugin-compression'
// import { visualizer } from "rollup-plugin-visualizer";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
      vue(),
    // visualizer({
    //   gzipSize: true,
    //   brotliSize: true,
    //   emitFile: false,
    //   filename: "state.html", //分析图生成的文件名
    //   open:true //如果存在本地服务端口，将在打包后自动展示
    // }),
    viteCompression({
      verbose: true, // 输出压缩日志
      disable: false, // 开启压缩
      threshold: 10240, // 大于10kb才压缩（和你assetsInlineLimit对应）
      algorithm: 'gzip', // 压缩方式
      ext: '.gz', // 后缀名
      deleteOriginFile: false, // 不删除源文件
    }),
  ],
  base: '/gauzynote/',   // 在生产中服务时的基本公共路径
  publicDir: 'public',  // 静态资源服务的文件夹, 默认"public"
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src') // 路径别名
    }
  },
  css: {
    css: {
      modules: {
        localsConvention: 'camelCaseOnly',
        generateScopedName: '[name]__[local]___[hash:base64:5]'
      }
    },
    postcss: {
      plugins: [
        autoprefixer({
          // 自动添加前缀
          overrideBrowserslist: [
            // 'Android 4.1',
            // 'iOS 7.1',
            // 'Chrome > 31',
            // 'ff > 31',
            // 'ie >= 8',
            "iOS >= 9",
            "Android >= 4.4",
            "chrome >= 87",
            "edge >= 88",
            "firefox >= 78",
            "safari >= 14"
            //'last 2 versions', // 所有主流浏览器最近2个版本
          ],
          grid: true,
        }),
      ]
    },
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        charset: false, // 关闭编译时 字符编码 报错问题
        javascriptEnabled: true,
      },
    },
  },
  esbuild: {
    // drop: ['console', 'debugger'], // 移除 console 和 debugger
  },
  // 打包配置
  build: {
    // minify: 'terser',
    // target: 'modules', // 设置最终构建的浏览器兼容目标。modules:支持原生 ES 模块的浏览器
    outDir: 'dist', // 指定输出路径
    assetsDir: 'assets', // 指定生成静态资源的存放路径
    assetsInlineLimit: 4096, // 图片转 base64 编码的阈值
    cssCodeSplit: true, // 启用/禁用CSS代码拆分，如果禁用，整个项目的所有CSS将被提取到一个CSS文件中,默认true
    // cssTarget: 'browserslist',
    sourcemap: false, // 构建后是否生成 source map 文件
    write: true,   //设置为 false 来禁用将构建后的文件写入磁盘
    emptyOutDir: true,  //默认情况下，若 outDir 在 root 目录下，则 Vite 会在构建时清空该目录。
    brotliSize: true,  //启用/禁用 brotli 压缩大小报告
    chunkSizeWarningLimit: 500,  //chunk 大小警告的限制
    rollupOptions: {
      output: {
        // 文件名格式：[name].[hash:8].[ext]
        entryFileNames: 'assets/js/[name].[hash:8].js',
        chunkFileNames: 'assets/js/[name].[hash:8].js',
        assetFileNames: 'assets/[ext]/[name].[hash:8].[ext]'
      }
    }
  },
  // 本地运行配置，及反向代理配置
  server: {
    host: "0.0.0.0", // 指定服务器主机名 'localhost' ； true 监听所有地址
    port: 8988, // 指定服务器端口
    open: true, // 在服务器启动时自动在浏览器中打开应用程序
    strictPort: false, // 设为 false 时，若端口已被占用则会尝试下一个可用端口,而不是直接退出
    https: false, // 是否开启 https
    cors: true, // 为开发服务器配置 CORS。默认启用并允许任何源
    proxy: { // 为开发服务器配置自定义代理规则 （反向代理）
      '/service/gauzynote': {
        target: 'http://localhost:8989', //代理接口
        changeOrigin: true,
        secure: false,
      },
    }
  },
})
