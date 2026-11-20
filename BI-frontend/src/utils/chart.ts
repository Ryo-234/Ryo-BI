/**
 * ECharts 配置解析工具
 *
 * 后端把 AI 生成的图表配置放在 chart.genChart 字段里，是一段文本，
 * 可能出现三种情况：
 * 1. 标准 JSON：{"title":{"text":"xxx"},"series":[...]}；
 * 2. 带代码围栏或前后自然语言说明：```json {...}``` 或 “以下是图表配置：{...}”；
 * 3. JS 对象字面量（后端提示词要求 AI 输出 option 的 js 代码）：
 *    { title: { text: 'xxx' }, series: [{ name: '用户数', data: [1, 2] }] }
 *    键名没有双引号、字符串使用单引号，JSON.parse 无法直接解析。
 *
 * 这里统一做清洗与解析，解析不出来返回 null，由调用方决定跳过或提示，
 * 保证一条脏数据不会影响整页渲染。
 */

/** 解析出来的图表配置对象 */
export type ChartOption = Record<string, unknown>;

/**
 * 去掉 markdown 代码围栏（```json / ```js / ```）。
 */
function stripCodeFence(text: string): string {
  const fenceMatch = text.match(/```(?:json|js|javascript)?\s*([\s\S]*?)```/i);
  return fenceMatch?.[1] ? fenceMatch[1].trim() : text;
}

/**
 * 从文本中截取配置本体：从第一个 { 或 [ 到最后一个 } 或 ]，
 * 顺带去掉前后多余的自然语言说明。
 */
function extractJsonBlock(text: string): string {
  const objectStart = text.indexOf('{');
  const arrayStart = text.indexOf('[');
  let start = -1;
  if (objectStart !== -1 && arrayStart !== -1) {
    start = Math.min(objectStart, arrayStart);
  } else {
    start = objectStart !== -1 ? objectStart : arrayStart;
  }
  if (start === -1) {
    return '';
  }

  const objectEnd = text.lastIndexOf('}');
  const arrayEnd = text.lastIndexOf(']');
  let end = -1;
  if (objectEnd !== -1 && arrayEnd !== -1) {
    end = Math.max(objectEnd, arrayEnd);
  } else {
    end = objectEnd !== -1 ? objectEnd : arrayEnd;
  }
  if (end <= start) {
    return '';
  }

  return text.slice(start, end + 1);
}

/**
 * 把 JS 对象字面量转换成合法 JSON 文本。
 *
 * 只做文本层面的转换（补引号、单引号转双引号），不执行任何代码，
 * 避免 AI 返回的内容被当作脚本运行。
 */
function jsLiteralToJsonText(text: string): string {
  let result = '';
  let index = 0;

  while (index < text.length) {
    const char = text[index];

    // 双引号字符串：原样保留
    if (char === '"') {
      const { raw, next } = readStringLiteral(text, index, '"');
      result += `"${raw}"`;
      index = next;
      continue;
    }

    // 单引号字符串：转成双引号字符串
    if (char === "'") {
      const { raw, next } = readStringLiteral(text, index, "'");
      result += JSON.stringify(raw.replace(/\\'/g, "'"));
      index = next;
      continue;
    }

    // 标识符：可能是对象键，也可能是 true / false / null
    if (/[A-Za-z_$]/.test(char)) {
      let end = index;
      while (end < text.length && /[A-Za-z0-9_$]/.test(text[end])) {
        end += 1;
      }
      const value = text.slice(index, end);
      let probe = end;
      while (probe < text.length && /\s/.test(text[probe])) {
        probe += 1;
      }
      if (text[probe] === ':') {
        // 对象键，补上双引号
        result += `"${value}"`;
      } else if (
        value === 'undefined' ||
        value === 'NaN' ||
        value === 'Infinity'
      ) {
        // JSON 没有这些字面量，统一转成 null
        result += 'null';
      } else {
        result += value;
      }
      index = end;
      continue;
    }

    result += char;
    index += 1;
  }

  return result;
}

/**
 * 读取一个字符串字面量，返回引号内部的原始文本与结束位置。
 */
function readStringLiteral(
  text: string,
  start: number,
  quote: string,
): { raw: string; next: number } {
  let raw = '';
  let index = start + 1;
  while (index < text.length) {
    const char = text[index];
    if (char === '\\') {
      raw += char;
      index += 1;
      if (index < text.length) {
        raw += text[index];
        index += 1;
      }
      continue;
    }
    if (char === quote) {
      index += 1;
      break;
    }
    raw += char;
    index += 1;
  }
  return { raw, next: index };
}

/** 去掉对象、数组结尾多余的逗号（JS 允许，JSON 不允许） */
function removeTrailingCommas(text: string): string {
  return text.replace(/,(\s*[}\]])/g, '$1');
}

/** 尝试解析文本，解析成功且是对象（非数组）时返回，否则返回 null */
function tryParse(text: string): ChartOption | null {
  try {
    const parsed: unknown = JSON.parse(text);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null;
    }
    return parsed as ChartOption;
  } catch {
    // JSON 非法，返回 null，交给调用方处理
    return null;
  }
}

/**
 * 解析后端返回的图表配置文本。
 *
 * @param raw 原始文本，可能带代码围栏、中文说明或使用 JS 字面量写法
 * @returns 解析成功返回配置对象，失败返回 null（不抛异常）
 */
export function parseChartOption(raw?: string): ChartOption | null {
  if (!raw) {
    return null;
  }

  let text = raw.trim();
  if (!text) {
    return null;
  }

  // 1. 去掉代码围栏
  text = stripCodeFence(text);

  // 2. 截取配置本体，去掉前后自然语言说明
  text = extractJsonBlock(text);
  if (!text) {
    return null;
  }

  // 3. 先按标准 JSON 解析
  const direct = tryParse(text);
  if (direct) {
    return direct;
  }

  // 4. 退化处理：按 JS 对象字面量转换后再解析一次
  return tryParse(removeTrailingCommas(jsLiteralToJsonText(text)));
}

/**
 * 去掉图表标题配置。
 * 列表页里标题由卡片头部展示，图表内部再画一遍会重复。
 */
export function removeChartTitle(option: ChartOption): ChartOption {
  const next: ChartOption = { ...option };
  delete next.title;
  return next;
}
