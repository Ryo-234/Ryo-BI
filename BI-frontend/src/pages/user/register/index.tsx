import { useMutation } from '@tanstack/react-query';
import { history, Link } from '@umijs/max';
import { Button, Form, Input, message, Popover, Progress } from 'antd';
import type { Store } from 'antd/es/form/interface';
import type { FC } from 'react';
import { useState } from 'react';
import { getBizErrorMessage, userRegister } from '@/services/user';
import useStyles from './styles';

const FormItem = Form.Item;

const passwordProgressMap: {
  ok: 'success';
  pass: 'normal';
  poor: 'exception';
} = {
  ok: 'success',
  pass: 'normal',
  poor: 'exception',
};

/** 后端约束：账号至少 4 位，密码至少 8 位 */
const MIN_ACCOUNT_LENGTH = 4;
const MIN_PASSWORD_LENGTH = 8;

const Register: FC = () => {
  const { styles } = useStyles();
  const [open, setVisible] = useState(false);
  const [popover, setPopover] = useState(false);
  const confirmDirty = false;

  const passwordStatusMap = {
    ok: (
      <div className={styles.success}>
        <span>强度：强</span>
      </div>
    ),
    pass: (
      <div className={styles.warning}>
        <span>强度：中</span>
      </div>
    ),
    poor: (
      <div className={styles.error}>
        <span>强度：太短</span>
      </div>
    ),
  };

  const [form] = Form.useForm();

  const getPasswordStatus = () => {
    const value = form.getFieldValue('userPassword');
    if (value && value.length >= 12) {
      return 'ok';
    }
    if (value && value.length >= MIN_PASSWORD_LENGTH) {
      return 'pass';
    }
    return 'poor';
  };

  const { isPending: submitting, mutate: register } = useMutation({
    mutationFn: (formValues: Store) => {
      return userRegister(
        {
          userAccount: String(formValues.userAccount ?? '').trim(),
          userPassword: String(formValues.userPassword ?? ''),
          checkPassword: String(formValues.checkPassword ?? ''),
        },
        // 错误信息在表单内展示，不弹全局提示
        { skipErrorHandler: true },
      );
    },
    onSuccess: (data) => {
      if (data.code === 0) {
        message.success('注册成功，请登录！');
        history.push({
          pathname: '/user/login',
        });
      } else {
        message.error(data.message || '注册失败，请重试！');
      }
    },
    onError: (error) => {
      message.error(getBizErrorMessage(error, '注册失败，请重试！'));
    },
  });

  const onFinish = (values: Store) => {
    register(values);
  };

  const checkConfirm = (_: any, value: string) => {
    const promise = Promise;
    if (value && value !== form.getFieldValue('userPassword')) {
      return promise.reject('两次输入的密码不匹配!');
    }
    return promise.resolve();
  };

  const checkPassword = (_: any, value: string) => {
    const promise = Promise;
    // 没有值的情况
    if (!value) {
      setVisible(!!value);
      return promise.reject('请输入密码!');
    }
    // 有值的情况
    if (!open) {
      setVisible(!!value);
    }
    setPopover(!popover);
    if (value.length < MIN_PASSWORD_LENGTH) {
      return promise.reject(`密码至少 ${MIN_PASSWORD_LENGTH} 位`);
    }
    if (value && confirmDirty) {
      form.validateFields(['checkPassword']);
    }
    return promise.resolve();
  };

  const renderPasswordProgress = () => {
    const value = form.getFieldValue('userPassword');
    const passwordStatus = getPasswordStatus();
    return value?.length ? (
      <div
        className={styles[`progress-${passwordStatus}` as keyof typeof styles]}
      >
        <Progress
          status={passwordProgressMap[passwordStatus]}
          size={6}
          percent={value.length * 10 > 100 ? 100 : value.length * 10}
          showInfo={false}
        />
      </div>
    ) : null;
  };

  return (
    <div className={styles.main}>
      <h3>注册</h3>
      <Form form={form} name="UserRegister" onFinish={onFinish}>
        <FormItem
          name="userAccount"
          rules={[
            {
              required: true,
              message: '请输入账号!',
            },
            {
              min: MIN_ACCOUNT_LENGTH,
              message: `账号至少 ${MIN_ACCOUNT_LENGTH} 位`,
            },
          ]}
        >
          <Input size="large" placeholder="账号" autoComplete="username" />
        </FormItem>
        <Popover
          getPopupContainer={(node) => {
            if (node?.parentNode) {
              return node.parentNode as HTMLElement;
            }
            return node;
          }}
          content={
            open && (
              <div
                style={{
                  padding: '4px 0',
                }}
              >
                {passwordStatusMap[getPasswordStatus()]}
                {renderPasswordProgress()}
                <div
                  style={{
                    marginTop: 10,
                  }}
                >
                  <span>
                    请至少输入 {MIN_PASSWORD_LENGTH} 个字符。请不要使用容易被猜到的密码。
                  </span>
                </div>
              </div>
            )
          }
          overlayStyle={{
            width: 240,
          }}
          placement="right"
          open={open}
        >
          <FormItem
            name="userPassword"
            className={
              form.getFieldValue('userPassword') &&
              form.getFieldValue('userPassword').length > 0 &&
              styles.password
            }
            rules={[
              {
                validator: checkPassword,
              },
            ]}
          >
            <Input
              size="large"
              type="password"
              placeholder={`至少${MIN_PASSWORD_LENGTH}位密码，区分大小写`}
              autoComplete="new-password"
            />
          </FormItem>
        </Popover>
        <FormItem
          name="checkPassword"
          rules={[
            {
              required: true,
              message: '请再次输入密码',
            },
            {
              validator: checkConfirm,
            },
          ]}
        >
          <Input
            size="large"
            type="password"
            placeholder="确认密码"
            autoComplete="new-password"
          />
        </FormItem>
        <FormItem>
          <div className={styles.footer}>
            <Button
              size="large"
              loading={submitting}
              className={styles.submit}
              type="primary"
              htmlType="submit"
            >
              <span>注册</span>
            </Button>
            <Link to="/user/login" prefetch>
              <span>使用已有账户登录</span>
            </Link>
          </div>
        </FormItem>
      </Form>
    </div>
  );
};
export default Register;
