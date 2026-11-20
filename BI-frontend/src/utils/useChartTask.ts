import { useEffect, useRef, useState } from 'react';

/** 图表任务状态：wait 排队中、running 执行中、succeed 已完成、failed 失败 */
export type ChartTaskStatus = 'wait' | 'running' | 'succeed' | 'failed';

/** 轮询相关配置 */
type UseChartTaskOptions = {
  /** 查询任务状态，返回任务的图表数据（ChartVO） */
  fetchStatus: (chartId: string) => Promise<API.ChartVO | undefined>;
  /** 任务完成回调 */
  onSuccess?: (task: API.ChartVO) => void;
  /** 任务失败回调 */
  onFailed?: (execMessage?: string) => void;
  /** 查询出错回调 */
  onError?: (message: string) => void;
  /** 轮询间隔，默认 2 秒 */
  interval?: number;
  /** 最大轮询次数，默认 150 次（2 秒一次，约 5 分钟） */
  maxTimes?: number;
};

/**
 * 图表异步任务轮询
 *
 * 后端的异步生成接口（线程池版 / MQ 版）提交后立即返回任务 id，
 * AI 分析在后台执行，这里负责按固定间隔查询任务状态并驱动界面更新。
 */
export function useChartTask(options: UseChartTaskOptions) {
  const {
    fetchStatus,
    onSuccess,
    onFailed,
    onError,
    interval = 2000,
    maxTimes = 150,
  } = options;

  /** 任务 id（雪花 ID，按字符串保存） */
  const [taskId, setTaskId] = useState<string | undefined>(undefined);
  /** 任务状态 */
  const [status, setStatus] = useState<ChartTaskStatus | undefined>(undefined);
  /** 是否正在轮询 */
  const [polling, setPolling] = useState(false);

  /** 轮询定时器 */
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  /** 已轮询次数，用于超时保护 */
  const count = useRef(0);

  /** 停止轮询 */
  const stop = () => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    setPolling(false);
  };

  /** 清空任务状态（提交新任务前或重置时调用） */
  const reset = () => {
    stop();
    count.current = 0;
    setTaskId(undefined);
    setStatus(undefined);
  };

  /** 查询一次任务状态，必要时继续轮询 */
  const poll = async (chartId: string) => {
    count.current += 1;
    if (count.current > maxTimes) {
      stop();
      onError?.('任务超时未完成，请稍后在「我的图表」中查看');
      return;
    }

    try {
      const task = await fetchStatus(chartId);
      if (!task) {
        stop();
        onError?.('获取任务状态失败');
        return;
      }

      const taskStatus = (task.status ?? 'wait') as ChartTaskStatus;
      setStatus(taskStatus);

      if (taskStatus === 'succeed') {
        stop();
        onSuccess?.(task);
        return;
      }

      if (taskStatus === 'failed') {
        stop();
        onFailed?.(task.execMessage);
        return;
      }

      // 排队中 / 执行中：继续轮询
      timer.current = setTimeout(() => {
        poll(chartId);
      }, interval);
    } catch (error) {
      stop();
      const message = error instanceof Error ? error.message : '未知错误';
      onError?.(`获取任务状态失败，${message}`);
    }
  };

  /** 开始轮询指定任务 */
  const start = (chartId: string) => {
    stop();
    count.current = 0;
    setTaskId(chartId);
    setStatus('wait');
    setPolling(true);
    poll(chartId);
  };

  // 组件卸载时清理定时器，避免内存泄漏与状态更新警告
  useEffect(() => stop, []);

  return { taskId, status, polling, start, stop, reset };
}
