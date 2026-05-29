import {defineConfig} from "vitest/config";

/**
 * Vitest 配置
 */
export default defineConfig({
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.js"]
  }
});
