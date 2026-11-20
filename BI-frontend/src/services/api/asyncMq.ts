// @ts-ignore
/* eslint-disable */
import { request } from "@umijs/max";

/** 提交 AI 生成图表任务 POST /api/async/mq/gen */
export async function genChartByAiUsingPost(
  body: {
    chartType?: string;
    goal?: string;
    name?: string;
  },
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (file) {
    formData.append("file", file);
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele];

    if (item !== undefined && item !== null) {
      if (typeof item === "object" && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ""));
        } else {
          formData.append(
            ele,
            new Blob([JSON.stringify(item)], { type: "application/json" })
          );
        }
      } else {
        formData.append(ele, item);
      }
    }
  });

  return request<API.BaseResponseBiResponse_>("/api/async/mq/gen", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** 查询异步任务状态 GET /api/async/mq/status/${param0} */
export async function getTaskStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTaskStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseChartVO_>(`/api/async/mq/status/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
