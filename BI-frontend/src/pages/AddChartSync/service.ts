import { request } from '@umijs/max';

/**
 * 阻塞式（同步）生成图表的请求封装
 *
 * 后端 POST /api/chart/gen/block 会同步等待 AI 返回结果，
 * 响应里直接带上 genChart、genResult 与 chartId。
 *
 * 说明：该接口目前还没有出现在运行中后端的 Swagger 文档里
 * （后端加了接口但实例未重启），所以这里先手写请求；
 * 后端重启后执行 `npm run openapi` 即可自动生成，
 * 届时把这里的调用替换成生成代码即可。
 */

/** 提交参数，与后端 GenChartByAiRequest 一一对应 */
export type GenChartBlockParams = {
  chartType?: string;
  goal?: string;
  name?: string;
};

/** 提交阻塞式生成图表任务，同步返回图表配置与分析结论 */
export async function genChartByAiBlock(
  body: GenChartBlockParams,
  file?: File,
  options?: { [key: string]: any },
) {
  const formData = new FormData();
  if (file) {
    formData.append('file', file);
  }
  Object.keys(body).forEach((key) => {
    const value = body[key as keyof GenChartBlockParams];
    if (value !== undefined && value !== null) {
      formData.append(key, value);
    }
  });

  return request<API.BaseResponseBiResponse_>('/api/chart/gen/block', {
    method: 'POST',
    data: formData,
    requestType: 'form',
    ...(options || {}),
  });
}
