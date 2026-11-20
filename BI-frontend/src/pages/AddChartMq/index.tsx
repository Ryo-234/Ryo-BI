import { UploadOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import {
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  message,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Upload,
} from 'antd';
import type { EChartsOption } from 'echarts';
import ReactECharts from 'echarts-for-react';
import { useState } from 'react';
import {
  genChartByAiUsingPost,
  getTaskStatusUsingGet,
} from '@/services/api/asyncMq';
import { parseChartOption } from '@/utils/chart';
import { type ChartTaskStatus, useChartTask } from '@/utils/useChartTask';
import './index.css';

/** 支持的图表类型，与后端约定的枚举一致 */
const CHART_TYPE_OPTIONS = [
  { label: '折线图', value: '折线图' },
  { label: '柱状图', value: '柱状图' },
  { label: '堆叠图', value: '堆叠图' },
  { label: '饼图', value: '饼图' },
  { label: '雷达图', value: '雷达图' },
];

/** 允许上传的文件后缀 */
const ALLOWED_EXTENSIONS = ['csv', 'xlsx', 'xls'];

/** 文件大小上限：10MB */
const MAX_FILE_SIZE = 10 * 1024 * 1024;

/** 右侧无数据时的占位文案 */
const PLACEHOLDER_TEXT = '请先在左侧进行提交';

/** 任务状态对应的展示文案与颜色 */
const STATUS_MAP: Record<ChartTaskStatus, { text: string; color: string }> = {
  wait: { text: '排队中', color: 'default' },
  running: { text: 'AI 分析中', color: 'processing' },
  succeed: { text: '已完成', color: 'success' },
  failed: { text: '执行失败', color: 'error' },
};

/**
 * 表单字段，与后端 GenChartByAiRequest 一一对应。
 * file 是 antd Upload 在表单中收集到的值，
 * 这里给 Form.Item 配置了 valuePropName="fileList"，
 * 表单拿到的是文件列表（UploadFile[]），原始文件对象在 originFileObj 上。
 */
type AddChartMqFormValues = {
  goal?: string;
  name?: string;
  chartType?: string;
  file?:
    | UploadFile[]
    | {
        file?: { originFileObj?: File };
        fileList?: UploadFile[];
      };
};

/**
 * 从表单值中取出上传的原始文件（originFileObj）。
 * 兼容文件列表与 { file, fileList } 两种结构。
 */
function getOriginFile(value?: AddChartMqFormValues['file']): File | undefined {
  if (!value) {
    return undefined;
  }
  const fileList: UploadFile[] = Array.isArray(value)
    ? value
    : (value.fileList ?? []);
  const candidates = [
    ...fileList.map((item) => item?.originFileObj),
    ...(Array.isArray(value) ? [] : [value.file?.originFileObj]),
  ];
  return candidates.find((item): item is File => item instanceof File);
}

/** 取文件后缀（不含点），并转成小写 */
function getFileExtension(fileName: string): string {
  return fileName.split('.').pop()?.toLowerCase() || '';
}

/**
 * 智能分析（MQ 异步版）页面。
 *
 * 与同步版（/add_chart_sync）的区别：提交后后端立即返回任务 id，
 * AI 分析由 RabbitMQ 消费者异步完成，前端轮询任务状态获取结果。
 */
const AddChartMq: React.FC = () => {
  const [form] = Form.useForm<AddChartMqFormValues>();
  const [messageApi, contextHolder] = message.useMessage();

  /** 任务失败原因 */
  const [execMessage, setExecMessage] = useState<string | undefined>(undefined);
  /** 任务完成后的图表数据 */
  const [chart, setChart] = useState<API.ChartVO | undefined>(undefined);
  /** 解析后的 ECharts 配置 */
  const [option, setOption] = useState<EChartsOption | undefined>(undefined);
  /** 是否正在提交任务 */
  const [submitting, setSubmitting] = useState(false);

  /** 异步任务轮询：查询任务状态并驱动界面更新 */
  const { taskId, status, polling, start, reset } = useChartTask({
    fetchStatus: async (chartId) => {
      const res = await getTaskStatusUsingGet(
        // 生成代码里的 id 类型是 number，实际传入的是字符串形式的雪花 ID
        { id: chartId as unknown as number },
        { skipErrorHandler: true },
      );
      return res?.data;
    },
    onSuccess: (task) => {
      const chartOption = parseChartOption(task.genChart);
      if (!chartOption) {
        messageApi.error('图表代码解析错误，请重试');
        return;
      }
      messageApi.success('分析完成');
      setChart(task);
      // 解析结果是普通对象，这里断言成 EChartsOption 交给图表组件渲染
      setOption(chartOption as unknown as EChartsOption);
    },
    onFailed: (failedMessage) => {
      setExecMessage(failedMessage);
      messageApi.error(failedMessage || '分析失败');
    },
    onError: (errorMessage) => {
      messageApi.error(errorMessage);
    },
  });

  /** 上传前校验：只允许 csv/xlsx/xls，且不超过 10MB */
  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    if (!ALLOWED_EXTENSIONS.includes(getFileExtension(file.name))) {
      messageApi.error('仅支持 CSV / XLSX / XLS 格式的文件');
      return Upload.LIST_IGNORE;
    }
    if (file.size > MAX_FILE_SIZE) {
      messageApi.error('文件大小不能超过 10MB');
      return Upload.LIST_IGNORE;
    }
    // 返回 false 表示不自动上传，文件随表单一起提交
    return false;
  };

  /** 重置表单时同步清空任务与结果 */
  const handleReset = () => {
    reset();
    setExecMessage(undefined);
    setChart(undefined);
    setOption(undefined);
  };

  /** 提交任务 */
  const handleFinish = async (values: AddChartMqFormValues) => {
    // 防止重复提交
    if (submitting) {
      return;
    }

    const goal = values.goal?.trim();
    if (!goal) {
      messageApi.error('请输入分析目标');
      return;
    }

    const file = getOriginFile(values.file);
    if (!file) {
      messageApi.error('请上传原始数据文件');
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      messageApi.error('文件大小不能超过 10MB');
      return;
    }
    if (!ALLOWED_EXTENSIONS.includes(getFileExtension(file.name))) {
      messageApi.error('仅支持 CSV / XLSX / XLS 格式的文件');
      return;
    }

    // 提交新任务前，先结束上一轮轮询并清空旧结果
    reset();
    setExecMessage(undefined);
    setChart(undefined);
    setOption(undefined);

    setSubmitting(true);
    try {
      // 普通字段走第一个参数，文件单独走第二个参数
      const params = { ...values, file: undefined, goal };
      const res = await genChartByAiUsingPost(params, file, {
        // 错误统一在下面的 catch 处理，避免全局与本地重复提示
        skipErrorHandler: true,
      });

      // chartId 是超出 JS 安全整数范围的雪花 ID，运行时为字符串
      const chartId = (res?.data as unknown as { chartId?: string } | undefined)
        ?.chartId;
      if (!chartId) {
        messageApi.error('任务提交失败');
        return;
      }

      messageApi.success('任务已提交，正在排队等待分析');
      // 开始轮询任务状态
      start(chartId);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      messageApi.error(`任务提交失败，${errorMessage}`);
    } finally {
      setSubmitting(false);
    }
  };

  const statusInfo = status ? STATUS_MAP[status] : undefined;

  return (
    <div className="add-chart-mq">
      {contextHolder}
      <Row gutter={24}>
        <Col xs={24} md={12}>
          <Card title="智能分析（MQ 异步）" className="add-chart-mq__card">
            <Spin spinning={submitting}>
              <Form<AddChartMqFormValues>
                form={form}
                layout="horizontal"
                labelAlign="left"
                labelCol={{ span: 4 }}
                wrapperCol={{ span: 16 }}
                onFinish={handleFinish}
                onReset={handleReset}
              >
                <Form.Item
                  name="goal"
                  label="分析目标"
                  rules={[
                    { required: true, message: '请输入分析目标' },
                    {
                      // 只有全空格时才额外提示，避免与必填提示重复
                      validator: (_, value: string) =>
                        !value || value.trim()
                          ? Promise.resolve()
                          : Promise.reject(new Error('分析目标不能为空格')),
                    },
                  ]}
                >
                  <Input.TextArea
                    rows={4}
                    placeholder="请输入你的分析需求，比如：分析网站用户的增长情况"
                    maxLength={500}
                    showCount
                  />
                </Form.Item>

                <Form.Item
                  name="name"
                  label="图表名称"
                  rules={[{ max: 100, message: '图表名称不能超过 100 个字符' }]}
                >
                  <Input placeholder="请输入图表名称" />
                </Form.Item>

                <Form.Item name="chartType" label="图表类型">
                  <Select
                    allowClear
                    placeholder="请选择图表类型"
                    options={CHART_TYPE_OPTIONS}
                  />
                </Form.Item>

                <Form.Item
                  name="file"
                  label="原始数据"
                  extra="支持 CSV / XLSX / XLS 格式，文件大小不超过 10MB"
                  // antd v6 的 Upload 使用 fileList 属性受控，这里把列表同步给表单
                  valuePropName="fileList"
                  getValueFromEvent={(event) => event?.fileList}
                >
                  <Upload
                    accept=".csv,.xlsx,.xls"
                    maxCount={1}
                    beforeUpload={beforeUpload}
                  >
                    <Button icon={<UploadOutlined />}>上传 CSV 文件</Button>
                  </Upload>
                </Form.Item>

                <Form.Item wrapperCol={{ span: 16, offset: 4 }}>
                  <Space>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={submitting}
                      disabled={submitting || polling}
                    >
                      提交
                    </Button>
                    <Button htmlType="reset" disabled={submitting || polling}>
                      重置
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            </Spin>
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card title="任务状态" className="add-chart-mq__card">
            <Spin spinning={polling}>
              {taskId ? (
                <div className="add-chart-mq__status">
                  <div>
                    任务 ID：
                    <span className="add-chart-mq__task-id">{taskId}</span>
                  </div>
                  <div className="margin-top-16">
                    当前状态：
                    <Tag color={statusInfo?.color}>{statusInfo?.text}</Tag>
                  </div>
                  <div className="add-chart-mq__hint">
                    {status === 'wait' && '任务已进入消息队列，等待消费者处理…'}
                    {status === 'running' && 'AI 正在分析数据，请稍候…'}
                    {status === 'succeed' && '分析完成，结果如下：'}
                    {status === 'failed' &&
                      (execMessage || '分析失败，请稍后重试')}
                  </div>
                </div>
              ) : (
                <div className="add-chart-mq__placeholder">
                  提交任务后这里会显示排队与执行状态
                </div>
              )}
            </Spin>
          </Card>

          <Divider />

          <Card title="分析结论" className="add-chart-mq__card">
            {chart?.genResult ? (
              <div className="add-chart-mq__result">{chart.genResult}</div>
            ) : (
              <div className="add-chart-mq__placeholder">
                {PLACEHOLDER_TEXT}
              </div>
            )}
          </Card>

          <Divider />

          <Card title="可视化图表" className="add-chart-mq__card">
            {option ? (
              <ReactECharts
                className="add-chart-mq__chart"
                option={option}
                // 不与上一次配置合并，避免两次任务的图表叠加
                notMerge
                lazyUpdate
              />
            ) : (
              <div className="add-chart-mq__placeholder">
                {PLACEHOLDER_TEXT}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AddChartMq;
