// @ts-ignore
import { TestBrowser } from '@@/testBrowser';
import { fireEvent, render, waitFor } from '@testing-library/react';
import React, { act } from 'react';
import { getLoginUser, userLogin } from '@/services/user';

jest.mock('@/services/user', () => ({
  userLogin: jest.fn(),
  getLoginUser: jest.fn(),
  userRegister: jest.fn(),
  userLogout: jest.fn(),
  getBizErrorMessage: jest.fn(
    (error: any, fallback = '请求失败') =>
      error?.info?.errorMessage || error?.message || fallback,
  ),
}));

const mockedUserLogin = userLogin as unknown as jest.Mock;
const mockedGetLoginUser = getLoginUser as unknown as jest.Mock;

const renderLogin = async () => {
  const historyRef = React.createRef<any>();
  const rootContainer = render(
    <TestBrowser
      historyRef={historyRef}
      location={{
        pathname: '/user/login',
      }}
    />,
  );
  await rootContainer.findAllByText('智能BI');
  return { rootContainer, historyRef };
};

const getInput = (rootContainer: HTMLElement, id: string) => {
  const input = rootContainer.querySelector(`#${id}`) as HTMLInputElement;
  expect(input).toBeTruthy();
  return input;
};

const fillLoginForm = (
  rootContainer: HTMLElement,
  account: string,
  password: string,
) => {
  const accountInput = getInput(rootContainer, 'userAccount');
  const passwordInput = getInput(rootContainer, 'userPassword');
  act(() => {
    fireEvent.change(accountInput, { target: { value: account } });
    fireEvent.change(passwordInput, { target: { value: password } });
  });
};

const submitLogin = async (rootContainer: HTMLElement) => {
  // LoginForm 的提交按钮是 type="button" 的主按钮
  const submitButton = rootContainer.querySelector(
    'button.ant-btn-primary',
  ) as HTMLButtonElement;
  expect(submitButton).toBeTruthy();
  await act(async () => {
    fireEvent.click(submitButton);
  });
};

describe('Login Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // 未登录：后端返回 code = 40100
    mockedGetLoginUser.mockResolvedValue({
      code: 40100,
      data: null,
      message: '未登录',
    });
    mockedUserLogin.mockResolvedValue({
      code: 0,
      data: {
        id: 1,
        userAccount: 'admin',
        userName: '管理员',
        userRole: 'admin',
      },
      message: 'ok',
    });
  });

  it('should show login form', async () => {
    const { rootContainer } = await renderLogin();

    expect(getInput(rootContainer.baseElement, 'userAccount')).toBeTruthy();
    expect(getInput(rootContainer.baseElement, 'userPassword')).toBeTruthy();

    const registerLink = rootContainer.baseElement?.querySelector(
      'a[href="/user/register"]',
    );
    expect(registerLink).toBeTruthy();

    rootContainer.unmount();
  });

  it('should call login api with userAccount and userPassword', async () => {
    const { rootContainer } = await renderLogin();

    fillLoginForm(rootContainer.baseElement, 'admin', '12345678');
    await submitLogin(rootContainer.baseElement);

    await waitFor(() => {
      expect(mockedUserLogin).toHaveBeenCalled();
    });
    expect(mockedUserLogin).toHaveBeenCalledWith(
      { userAccount: 'admin', userPassword: '12345678' },
      { skipErrorHandler: true },
    );

    rootContainer.unmount();
  });

  it('should show backend error message when login fails', async () => {
    mockedUserLogin.mockRejectedValue({
      name: 'BizError',
      message: '用户不存在或密码错误',
      info: { errorCode: 40000, errorMessage: '用户不存在或密码错误' },
    });

    const { rootContainer } = await renderLogin();

    fillLoginForm(rootContainer.baseElement, 'admin', 'wrongpass');
    await submitLogin(rootContainer.baseElement);

    // 后端返回的错误信息直接展示在表单内
    await rootContainer.findByText('用户不存在或密码错误');

    rootContainer.unmount();
  });
});
