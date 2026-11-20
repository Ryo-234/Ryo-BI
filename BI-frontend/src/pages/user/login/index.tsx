import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { Helmet, Link, SelectLang, useIntl, useModel } from '@umijs/max';
import { Alert, App } from 'antd';
import { createStyles } from 'antd-style';
import React, { useState } from 'react';
import { flushSync } from 'react-dom';
import { Footer } from '@/components';
import { getBizErrorMessage, userLogin } from '@/services/user';
import Settings from '../../../../config/defaultSettings';

const useStyles = createStyles(({ token }) => {
  return {
    lang: {
      width: 42,
      height: 42,
      lineHeight: '42px',
      position: 'fixed',
      right: 16,
      borderRadius: token.borderRadius,
      ':hover': {
        backgroundColor: token.colorBgTextHover,
      },
    },
    container: {
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'auto',
      backgroundImage:
        "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
      backgroundSize: '100% 100%',
    },
    registerLink: {
      display: 'block',
      textAlign: 'center',
      marginTop: 12,
    },
  };
});

const Lang = () => {
  const { styles } = useStyles();

  return (
    <div className={styles.lang} data-lang>
      {SelectLang && <SelectLang />}
    </div>
  );
};

const Login: React.FC = () => {
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const { initialState, setInitialState } = useModel('@@initialState');
  const { styles } = useStyles();
  const { message } = App.useApp();
  const intl = useIntl();

  /**
   * Validate redirect URL to prevent open redirect attacks
   * Only allow same-origin relative paths starting with '/'
   */
  const getSafeRedirectUrl = (redirect: string | null): string => {
    if (!redirect?.startsWith('/')) return '/';

    // Block protocol-relative URLs (//example.com)
    if (redirect.startsWith('//')) return '/';

    try {
      const parsed = new URL(redirect, window.location.origin);
      // Only allow same-origin URLs
      if (parsed.origin !== window.location.origin) return '/';
      // Return the path with query and hash preserved
      return `${parsed.pathname}${parsed.search}${parsed.hash}`;
    } catch {
      return '/';
    }
  };

  const fetchUserInfo = async () => {
    const userInfo = await initialState?.fetchUserInfo?.();
    if (userInfo) {
      flushSync(() => {
        setInitialState((s) => ({
          ...s,
          currentUser: userInfo,
        }));
      });
    }
  };

  const handleSubmit = async (values: {
    userAccount?: string;
    userPassword?: string;
  }) => {
    setErrorMessage('');
    setSubmitting(true);
    try {
      const res = await userLogin(
        {
          userAccount: values.userAccount?.trim() ?? '',
          userPassword: values.userPassword ?? '',
        },
        // 错误信息在表单内展示，不弹全局提示
        { skipErrorHandler: true },
      );
      if (res.code === 0 && res.data) {
        message.success(
          intl.formatMessage({
            id: 'pages.login.success',
            defaultMessage: '登录成功！',
          }),
        );
        await fetchUserInfo();
        const urlParams = new URL(window.location.href).searchParams;
        const redirectUrl = getSafeRedirectUrl(urlParams.get('redirect'));
        window.location.href = redirectUrl;
        return;
      }
      setErrorMessage(
        res.message ||
          intl.formatMessage({
            id: 'pages.login.failure',
            defaultMessage: '登录失败，请重试！',
          }),
      );
    } catch (error) {
      setErrorMessage(
        getBizErrorMessage(
          error,
          intl.formatMessage({
            id: 'pages.login.failure',
            defaultMessage: '登录失败，请重试！',
          }),
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.container}>
      <Helmet>
        <title>
          {intl.formatMessage({
            id: 'menu.login',
            defaultMessage: '登录页',
          })}
          {Settings.title && ` - ${Settings.title}`}
        </title>
      </Helmet>
      <Lang />
      <div
        style={{
          flex: '1',
          padding: '32px 0',
        }}
      >
        <LoginForm
          contentStyle={{
            minWidth: 280,
            maxWidth: '75vw',
          }}
          logo={<img alt="logo" src="/logo.svg" />}
          title="智能BI"
          subTitle={intl.formatMessage({
            id: 'pages.layouts.userLayout.title',
            defaultMessage: '智能 BI 数据分析平台',
          })}
          initialValues={{
            userAccount: '',
            userPassword: '',
          }}
          submitter={{
            searchConfig: {
              submitText: intl.formatMessage({
                id: 'pages.login.submit',
                defaultMessage: '登录',
              }),
            },
            submitButtonProps: {
              loading: submitting,
              size: 'large',
            },
          }}
          onFinish={async (values) => {
            await handleSubmit(values as any);
          }}
        >
          {errorMessage && (
            <Alert
              style={{
                marginBottom: 24,
              }}
              title={errorMessage}
              type="error"
              showIcon
            />
          )}

          <ProFormText
            name="userAccount"
            fieldProps={{
              size: 'large',
              prefix: <UserOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.login.account.placeholder',
              defaultMessage: '请输入账号',
            })}
            rules={[
              {
                required: true,
                message: intl.formatMessage({
                  id: 'pages.login.account.required',
                  defaultMessage: '账号是必填项！',
                }),
              },
              {
                min: 4,
                message: intl.formatMessage({
                  id: 'pages.login.account.min',
                  defaultMessage: '账号至少 4 个字符',
                }),
              },
            ]}
          />
          <ProFormText.Password
            name="userPassword"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.login.password.placeholder',
              defaultMessage: '请输入密码',
            })}
            rules={[
              {
                required: true,
                message: intl.formatMessage({
                  id: 'pages.login.password.required',
                  defaultMessage: '密码是必填项！',
                }),
              },
              {
                min: 8,
                message: intl.formatMessage({
                  id: 'pages.login.password.min',
                  defaultMessage: '密码至少 8 个字符',
                }),
              },
            ]}
          />

          <Link className={styles.registerLink} to="/user/register" prefetch>
            {intl.formatMessage({
              id: 'pages.login.registerAccount',
              defaultMessage: '注册账户',
            })}
          </Link>
        </LoginForm>
      </div>
      <Footer />
    </div>
  );
};

export default Login;
