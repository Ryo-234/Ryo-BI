import { Link, useSearchParams } from '@umijs/max';
import { Button, Result } from 'antd';
import React from 'react';
import useStyles from './style.style';

const RegisterResult: React.FC<Record<string, unknown>> = () => {
  const { styles } = useStyles();
  const [params] = useSearchParams();

  const actions = (
    <div className={styles.actions}>
      <Link to="/user/login" prefetch>
        <Button size="large" type="primary">
          <span>去登录</span>
        </Button>
      </Link>
      <Link to="/" prefetch>
        <Button size="large">返回首页</Button>
      </Link>
    </div>
  );

  const account = params?.get('account') || '';
  return (
    <Result
      className={styles.registerResult}
      status="success"
      title={
        <div className={styles.title}>
          <span>你的账户：{account} 注册成功</span>
        </div>
      }
      subTitle="注册成功后即可使用该账号登录系统。"
      extra={actions}
    />
  );
};
export default RegisterResult;
