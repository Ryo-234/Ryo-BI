import { useModel } from '@umijs/max';
import { Avatar, Card, Empty, Input, List, message } from 'antd';
import type { EChartsOption } from 'echarts';
import ReactECharts from 'echarts-for-react';
import { useEffect, useState } from 'react';
import { listMyChartByPageUsingGet } from '@/services/api/chart';
import {
  type ChartOption,
  parseChartOption,
  removeChartTitle,
} from '@/utils/chart';
import './index.css';

/** 初始查询条件：图表卡片占空间大，默认每页 4 条 */
const initSearchParams: API.ChartQueryRequest = {
  current: 1,
  pageSize: 4,
};

/**
 * 列表项：后端返回的图表数据 + 前端预先解析好的图表配置。
 * 预先解析是为了避免在渲染阶段反复 parse，也避免脏数据让整页崩溃。
 */
type MyChartItem = API.ChartVO & {
  /** 解析后的 ECharts 配置，解析失败的条目不会进入列表 */
  chartOption?: ChartOption;
};

/**
 * 我的图表列表页。
 * 展示当前登录用户通过「智能分析」生成的所有图表，支持按名称搜索与分页。
 */
const MyChartPage: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  // umi 的 initialState 类型由运行时决定，这里断言成带 currentUser 的结构，取当前登录用户头像
  const currentUser = (
    initialState as unknown as { currentUser?: API.CurrentUser } | undefined
  )?.currentUser;
  const [messageApi, contextHolder] = message.useMessage();

  /** 查询条件（分页 + 搜索），条件变化后自动重新请求 */
  const [searchParams, setSearchParams] = useState<API.ChartQueryRequest>({
    ...initSearchParams,
  });
  /** 当前页的图表列表 */
  const [chartList, setChartList] = useState<MyChartItem[]>([]);
  /** 图表总数 */
  const [total, setTotal] = useState(0);
  /** 是否正在加载 */
  const [loading, setLoading] = useState(true);

  /** 拉取我的图表列表，并预处理每条数据的图表配置 */
  const loadData = async (params: API.ChartQueryRequest) => {
    setLoading(true);
    try {
      const res = await listMyChartByPageUsingGet(params, {
        // 错误统一在下面处理，避免全局与页面重复弹提示
        skipErrorHandler: true,
      });

      if (!res?.data) {
        messageApi.error('获取我的图表失败');
        return;
      }

      const records = res.data.records ?? [];
      // 逐条解析图表配置：解析失败的条目直接跳过并告警，不影响其它条目渲染
      const list = records.reduce<MyChartItem[]>((result, record) => {
        const option = parseChartOption(record.genChart);
        if (!option) {
          console.warn(
            '[MyChart] 图表配置解析失败，已跳过该条数据，id：',
            record.id,
            record.genChart,
          );
          return result;
        }
        // 去掉标题，标题由卡片头部统一展示
        result.push({ ...record, chartOption: removeChartTitle(option) });
        return result;
      }, []);

      setChartList(list);
      setTotal(res.data.total ?? 0);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      messageApi.error(`获取我的图表失败，${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  // 查询条件变化后自动重新请求
  useEffect(() => {
    loadData(searchParams);
  }, [searchParams]);

  /** 搜索图表名称：重置分页到第一页，避免停留在旧页码上 */
  const handleSearch = (value: string) => {
    const keyword = value.trim();
    setSearchParams({
      ...initSearchParams,
      // 关键词为空时不过滤，等同于回到全部数据
      name: keyword || undefined,
    });
  };

  /** 翻页 / 切换每页条数 */
  const handlePageChange = (page: number, pageSize: number) => {
    const sizeChanged = pageSize !== searchParams.pageSize;
    setSearchParams({
      ...searchParams,
      // 切换每页条数时回到第一页
      current: sizeChanged ? 1 : page,
      pageSize,
    });
  };

  return (
    <div className="my-chart-page">
      {contextHolder}
      <div className="my-chart-page__toolbar margin-16">
        <Input.Search
          className="my-chart-page__search"
          placeholder="请输入图表名称"
          enterButton
          loading={loading}
          maxLength={100}
          defaultValue={searchParams.name}
          onSearch={handleSearch}
        />
      </div>

      <List<MyChartItem>
        className="my-chart-page__list"
        grid={{ gutter: 16, xs: 1, sm: 1, md: 1, lg: 2, xl: 2, xxl: 2 }}
        loading={loading}
        dataSource={chartList}
        rowKey="id"
        locale={{
          emptyText: (
            <Empty description="暂无图表，去「智能分析」生成一张试试" />
          ),
        }}
        pagination={{
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: [4, 8, 12],
          showTotal: (count) => `共 ${count} 条`,
          onChange: handlePageChange,
        }}
        renderItem={(item) => (
          <List.Item>
            <Card className="my-chart-page__card" style={{ width: '100%' }}>
              <List.Item.Meta
                avatar={<Avatar src={currentUser?.userAvatar} />}
                title={item.name || '未命名图表'}
                description={
                  item.chartType ? `图表类型：${item.chartType}` : undefined
                }
              />
              <div className="my-chart-page__goal">分析目标：{item.goal}</div>
              <ReactECharts
                className="my-chart-page__chart"
                // 解析结果是普通对象，这里断言成 EChartsOption 交给图表组件
                option={item.chartOption as unknown as EChartsOption}
                notMerge
                lazyUpdate
              />
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
};

export default MyChartPage;
