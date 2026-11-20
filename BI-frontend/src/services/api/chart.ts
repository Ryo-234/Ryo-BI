// @ts-ignore
/* eslint-disable */
import { request } from "@umijs/max";

/** 创建图表 POST /api/chart/add */
export async function addChartUsingPost(
  body: API.ChartCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/chart/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 管理员删除图表 DELETE /api/chart/admin/delete/${param0} */
export async function adminDeleteChartUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.adminDeleteChartUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/chart/admin/delete/${param0}`,
    {
      method: "DELETE",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 管理员查看图表详情 GET /api/chart/admin/get/${param0} */
export async function adminGetChartByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.adminGetChartByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseChartVO_>(`/api/chart/admin/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 管理员分页查询图表列表 GET /api/chart/admin/list */
export async function adminListChartUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.adminListChartUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageResultVOChartVO_>(
    "/api/chart/admin/list",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 管理员更新图表 PUT /api/chart/admin/update */
export async function adminUpdateChartUsingPut(
  body: API.ChartUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/chart/admin/update", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 删除自己的图表 DELETE /api/chart/delete/${param0} */
export async function deleteMyChartUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteMyChartUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(`/api/chart/delete/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** genChartByAi POST /api/chart/gen */
export async function genChartByAiUsingPost1(
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

  return request<API.BaseResponseBiResponse_>("/api/chart/gen", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** 查看自己的图表详情 GET /api/chart/get/${param0} */
export async function getMyChartByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMyChartByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseChartVO_>(`/api/chart/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 分页查询自己的图表列表 GET /api/chart/list */
export async function listMyChartByPageUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyChartByPageUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageResultVOChartVO_>("/api/chart/list", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 修改自己的图表 PUT /api/chart/update */
export async function updateMyChartUsingPut(
  body: API.ChartUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/chart/update", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
