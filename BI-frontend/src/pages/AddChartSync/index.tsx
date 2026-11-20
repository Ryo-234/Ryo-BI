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
  Upload,
} from 'antd';
import type { EChartsOption } from 'echarts';
import ReactECharts from 'echarts-for-react';
import { useState } from 'react';
import { parseChartOption } from '@/utils/chart';
import { genChartByAiBlock } from './service';
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

/**
 * 表单字段，与后端 GenChartByAiRequest 一一对应。
 * file 是 antd Upload 在表单中收集到的值，
 * 这里给 Form.Item 配置了 valuePropName="fileList"，
 * 表单拿到的是文件列表（UploadFile[]），原始文件对象在 originFileObj 上。
 */
type AddChartSyncFormValues = {
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
function getOriginFile(
  value?: AddChartSyncFormValues['file'],
): File | undefined {
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
 * 智能分析（同步阻塞版）页面。
 *
 * 与线程池版（/add_chart）、MQ 版（/add_chart_mq）的区别：
 * 接口会同步等待 AI 分析完成，响应里直接带回图表配置与分析结论，
 * 不需要轮询任务状态。
 */
const AddChartSync: React.FC = () => {
  const [form] = Form.useForm<AddChartSyncFormValues>();
  const [messageApi, contextHolder] = message.useMessage();

  /** 后端返回的分析结论 */
  const [genResult, setGenResult] = useState<string | undefined>(undefined);
  /** 解析后的 ECharts 配置 */
  const [option, setOption] = useState<EChartsOption | undefined>(undefined);
  /** 是否正在提交 */
  const [submitting, setSubmitting] = useState(false);

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

  /** 重置表单时同步清空右侧结果 */
  const handleReset = () => {
    setGenResult(undefined);
    setOption(undefined);
  };

  /** 提交表单，同步等待 AI 返回结果 */
  const handleFinish = async (values: AddChartSyncFormValues) => {
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

    setSubmitting(true);
    // 提交前先清空上一次的结果，避免新旧图表叠加
    setGenResult(undefined);
    setOption(undefined);

    try {
      // 普通字段走第一个参数，文件单独走第二个参数
      const params = { ...values, file: undefined, goal };
      const res = await genChartByAiBlock(params, file, {
        // 错误统一在下面的 catch 处理，避免全局与本地重复提示
        skipErrorHandler: true,
      });

      if (!res?.data) {
        messageApi.error('分析失败');
        return;
      }

      const chartOption = parseChartOption(res.data.genChart);
      if (!chartOption) {
        messageApi.error('图表代码解析错误，请重试');
        return;
      }

      messageApi.success('分析成功');
      setGenResult(res.data.genResult);
      // 解析结果是普通对象，这里断言成 EChartsOption 交给图表组件渲染
      setOption(chartOption as unknown as EChartsOption);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      messageApi.error(`分析失败，${errorMessage}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="add-chart-sync">
      {contextHolder}
      <Row gutter={24}>
        <Col xs={24} md={12}>
          <Card title="智能分析（同步）" className="add-chart-sync__card">
            <Spin spinning={submitting}>
              <Form<AddChartSyncFormValues>
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
                      disabled={submitting}
                    >
                      提交
                    </Button>
                    <Button htmlType="reset" disabled={submitting}>
                      重置
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            </Spin>
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card title="分析结论" className="add-chart-sync__card">
            {genResult ? (
              <div className="add-chart-sync__result">{genResult}</div>
            ) : (
              <div className="add-chart-sync__placeholder">
                {PLACEHOLDER_TEXT}
              </div>
            )}
          </Card>

          <Divider />

          <Card title="可视化图表" className="add-chart-sync__card">
            {option ? (
              <ReactECharts
                className="add-chart-sync__chart"
                option={option}
                // 不与上一次配置合并，避免两次提交的图表叠加
                notMerge
                lazyUpdate
              />
            ) : (
              <div className="add-chart-sync__placeholder">
                {PLACEHOLDER_TEXT}
              </div>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AddChartSync;
