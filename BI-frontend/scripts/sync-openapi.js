/**
 * 同步后端接口文档（OpenAPI 3）
 *
 * 后端使用的是 Swagger2 规范（knife4j-openapi2），而 umi 的 openapi 插件按 OpenAPI 3 生成代码，
 * 因此这里做三件事：
 * 1. 拉取后端 /api/v2/api-docs（Swagger2 文档）；
 * 2. 用 swagger2openapi 转换成 OpenAPI 3；
 * 3. 修正 multipart/form-data 接口的参数位置（Swagger2 把普通字段标成了 query，
 *    实际后端是通过表单字段接收的），并写出 config/oneapi.json。
 *
 * 用法：node scripts/sync-openapi.js
 */
const fs = require('node:fs');
const path = require('node:path');
const swagger2openapi = require('swagger2openapi');

/**
 * 后端接口文档地址，可通过环境变量覆盖
 */
const DOC_URL =
  process.env.OPENAPI_DOC_URL ||
  'http://localhost:8123/api/v2/api-docs?group=默认分组';
/** 生成后的 OpenAPI 3 文档位置，与 config/config.ts 中的 schemaPath 保持一致 */
const OUTPUT_FILE = path.join(__dirname, '..', 'config', 'oneapi.json');

/**
 * 中文分组名与英文文件名的映射。
 * umi 的 openapi 插件用分组名作为生成的文件名，中文拼音可读性较差，这里统一转成英文。
 */
const TAG_NAME_MAP = {
  图表管理: 'chart',
  用户管理: 'user',
  '异步生成图表（MQ）': 'asyncMq',
};

/**
 * 处理 multipart/form-data 接口的参数位置：
 * 1. Swagger2 转换后 requestBody 可能是 $ref 组件引用，这里先内联展开；
 * 2. Swagger2 把 goal/name/chartType 这类普通字段标成了 query，
 *    但后端 controller 用普通 DTO 接收（Spring 从表单字段取值），
 *    这里把它们迁移到 requestBody 的表单字段里。
 */
function fixMultipartParams(openapi) {
  const paths = openapi.paths || {};
  Object.keys(paths).forEach((pathKey) => {
    const operation = paths[pathKey]?.post;
    if (!operation) {
      return;
    }

    // 1. 内联展开 requestBody 的 $ref 引用
    const ref = operation.requestBody?.$ref;
    if (ref) {
      const name = ref.split('/').pop();
      const component = openapi.components?.requestBodies?.[name];
      if (component) {
        // 深拷贝，避免多个接口共用同一组件时互相影响
        operation.requestBody = JSON.parse(JSON.stringify(component));
      }
    }

    const multipart = operation.requestBody?.content?.['multipart/form-data'];
    if (!multipart) {
      return;
    }
    const queryParams = (operation.parameters || []).filter(
      (param) => param.in === 'query',
    );
    if (queryParams.length === 0) {
      return;
    }
    const schema = multipart.schema || { type: 'object', properties: {} };
    schema.properties = schema.properties || {};
    queryParams.forEach((param) => {
      schema.properties[param.name] = {
        type: param.schema?.type || param.type || 'string',
        ...(param.description ? { description: param.description } : {}),
      };
    });
    multipart.schema = schema;
    // 表单字段已经承载这些参数，query 中不再重复声明
    operation.parameters = (operation.parameters || []).filter(
      (param) => param.in !== 'query',
    );
  });
  return openapi;
}

/**
 * 把中文分组名换成英文，生成的请求文件更易读（src/services/api/chart.ts 等）。
 */
function renameTags(openapi) {
  (openapi.tags || []).forEach((tag) => {
    if (TAG_NAME_MAP[tag.name]) {
      tag.name = TAG_NAME_MAP[tag.name];
    }
  });
  Object.keys(openapi.paths || {}).forEach((pathKey) => {
    const pathItem = openapi.paths[pathKey] || {};
    Object.keys(pathItem).forEach((method) => {
      const operation = pathItem[method];
      if (operation?.tags?.length) {
        operation.tags = operation.tags.map((tag) => TAG_NAME_MAP[tag] || tag);
      }
    });
  });
  return openapi;
}

async function main() {
  const response = await fetch(DOC_URL);
  if (!response.ok) {
    throw new Error(`拉取接口文档失败：${DOC_URL} 返回 ${response.status}`);
  }
  const swagger = await response.json();
  const converted = await new Promise((resolve, reject) => {
    swagger2openapi.convertObj(
      swagger,
      { patch: true, warnOnly: true },
      (error, result) => {
        if (error) {
          reject(error);
          return;
        }
        resolve(result);
      },
    );
  });
  // convertObj 返回的是转换选项，真正的 OpenAPI 3 文档在 openapi 字段上
  const openapi = converted.openapi || converted;

  openapi.servers = [{ url: 'http://localhost:8123' }];
  fixMultipartParams(openapi);
  renameTags(openapi);

  fs.writeFileSync(
    OUTPUT_FILE,
    `${JSON.stringify(openapi, null, 2)}\n`,
    'utf8',
  );
  console.log(`接口文档已写入：${OUTPUT_FILE}`);
}

main().catch((error) => {
  console.error('同步接口文档失败：', error);
  process.exit(1);
});
