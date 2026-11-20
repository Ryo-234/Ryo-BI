declare namespace API {
  type adminDeleteChartUsingDELETEParams = {
    /** id */
    id: number;
  };

  type adminDeleteUserUsingDELETEParams = {
    /** id */
    id: number;
  };

  type adminGetChartByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type adminGetUserByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type adminListChartUsingGETParams = {
    chartData?: string;
    chartType?: string;
    current?: number;
    genChart?: string;
    genResult?: string;
    goal?: string;
    id?: number;
    name?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userId?: number;
  };

  type adminListUserUsingGETParams = {
    current?: number;
    id?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userAccount?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type BaseResponseBiResponse_ = {
    code?: number;
    data?: BiResponse;
    message?: string;
  };

  type BaseResponseBoolean_ = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseChartVO_ = {
    code?: number;
    data?: ChartVO;
    message?: string;
  };

  type BaseResponseListUserLoginVO_ = {
    code?: number;
    data?: UserLoginVO[];
    message?: string;
  };

  type BaseResponseLong_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponsePageResultVOChartVO_ = {
    code?: number;
    data?: PageResultVOChartVO_;
    message?: string;
  };

  type BaseResponsePageResultVOUserLoginVO_ = {
    code?: number;
    data?: PageResultVOUserLoginVO_;
    message?: string;
  };

  type BaseResponseUser_ = {
    code?: number;
    data?: User;
    message?: string;
  };

  type BaseResponseUserLoginVO_ = {
    code?: number;
    data?: UserLoginVO;
    message?: string;
  };

  type BiResponse = {
    chartId?: number;
    genChart?: string;
    genResult?: string;
  };

  type ChartCreateRequest = {
    chartData?: string;
    chartType?: string;
    genChart?: string;
    genResult?: string;
    goal?: string;
    name?: string;
  };

  type ChartUpdateRequest = {
    chartData?: string;
    chartType?: string;
    genChart?: string;
    genResult?: string;
    goal?: string;
    id?: number;
    name?: string;
  };

  type ChartVO = {
    chartData?: string;
    chartType?: string;
    createTime?: string;
    execMessage?: string;
    genChart?: string;
    genResult?: string;
    goal?: string;
    id?: number;
    name?: string;
    status?: string;
    updateTime?: string;
    userId?: number;
  };

  type deleteMyChartUsingDELETEParams = {
    /** id */
    id: number;
  };

  type getMyChartByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type getTaskStatusUsingGETParams = {
    /** id */
    id: number;
  };

  type getUserByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type listMyChartByPageUsingGETParams = {
    chartData?: string;
    chartType?: string;
    current?: number;
    genChart?: string;
    genResult?: string;
    goal?: string;
    id?: number;
    name?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userId?: number;
  };

  type listUserByPageUsingGETParams = {
    current?: number;
    id?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userAccount?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type PageResultVOChartVO_ = {
    current?: number;
    pageSize?: number;
    pages?: number;
    records?: ChartVO[];
    total?: number;
  };

  type PageResultVOUserLoginVO_ = {
    current?: number;
    pageSize?: number;
    pages?: number;
    records?: UserLoginVO[];
    total?: number;
  };

  type User = {
    createTime?: string;
    editTime?: string;
    id?: number;
    isDelete?: number;
    updateTime?: string;
    userAccount?: string;
    userAvatar?: string;
    userName?: string;
    userPassword?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserCreateRequest = {
    userAccount?: string;
    userPassword?: string;
    userRole?: string;
  };

  type UserLoginRequest = {
    userAccount?: string;
    userPassword?: string;
  };

  type UserLoginVO = {
    id?: number;
    userAccount?: string;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserRegisterRequest = {
    checkPassword?: string;
    userAccount?: string;
    userPassword?: string;
  };

  type UserUpdateRequest = {
    id?: number;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
  };
}
