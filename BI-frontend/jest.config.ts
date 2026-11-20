import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { configUmiAlias, createConfig } from '@umijs/max/test.js';

const readPkgVersion = (pkg: string) =>
  JSON.parse(readFileSync(join('node_modules', pkg, 'package.json'), 'utf-8'))
    .version;

export default async (): Promise<any> => {
  const config = await configUmiAlias({
    ...createConfig({
      target: 'browser',
    }),
  });
  return {
    ...config,
    testPathIgnorePatterns: ['/node_modules/', '/.worktrees/'],
    moduleNameMapper: {
      '\\.md$': '<rootDir>/tests/__mocks__/raw.js',
      ...(config.moduleNameMapper || {}),
      '^mermaid$': '<rootDir>/tests/__mocks__/mermaid.js',
    },
    testEnvironmentOptions: {
      ...(config?.testEnvironmentOptions || {}),
      url: 'http://localhost:8000',
    },
    setupFiles: [...(config.setupFiles || []), './tests/setupTests.jsx'],
    globals: {
      ...config.globals,
      localStorage: null,
      /**
       * 使用 automatic JSX runtime。
       * umi 的 jest 转换器对包含 `jest.mock(` 的文件会先执行一次 babel 的 CJS 转换，
       * 此时 `import React from 'react'` 会被改名为 `_react`，
       * 而 JSX 若仍按 classic runtime 编译成 `React.createElement` 就会报
       * “React is not defined”。automatic runtime 不依赖 React 全局变量。
       */
      'jest-esbuild': {
        ...(config.globals?.['jest-esbuild'] || {}),
        jsx: 'automatic',
      },
      __APP_VERSION__: 'test',
      __UMI_VERSION__: readPkgVersion('@umijs/max'),
      __UTOO_VERSION__: readPkgVersion('@utoo/pack'),
    },
  };
};
