package com.ryo.ryopicturebackend.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.ryo.ryopicturebackend.exception.BusinessException;
import com.ryo.ryopicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Excel 工具类
 * 提供 Excel 转 CSV 的能力，基于 EasyExcel 实现
 */
@Slf4j
public class ExcelUtils {

    /**
     * CSV 分隔符
     */
    private static final char DELIMITER = ',';

    /**
     * 字符集
     */
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * CSV 行结束符
     */
    private static final String LINE_SEPARATOR = "\r\n";

    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * 将上传的 Excel 文件转换为 CSV 字符串
     * 先把 Excel 按流式写出为 CSV，再把 CSV 逐行读出拼接成字符串返回
     *
     * @param multipartFile 上传的 Excel 文件
     * @return CSV 字符串
     */
    public static String excelToCsv(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件为空，无法转换");
        }
        byte[] csvBytes;
        try (InputStream in = multipartFile.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeCsv(in, out);
            csvBytes = out.toByteArray();
        } catch (IOException e) {
            log.error("excel 文件转换 csv 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Excel 文件解析失败");
        }
        StringBuilder csv = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                csv.append(line).append('\n');
            }
        } catch (IOException e) {
            log.error("读取 csv 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Excel 文件解析失败");
        }
        return csv.toString();
    }

    /**
     * 读取 Excel 并写出 CSV
     *
     * @param excelStream Excel 输入流
     * @param csvOut      CSV 输出流
     * @throws IOException 读取或写出失败
     */
    private static void writeCsv(InputStream excelStream, ByteArrayOutputStream csvOut) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(csvOut, CHARSET), BUFFER_SIZE);
        // headRowNumber 设置为 0，表头行按普通数据行输出，CSV 中自然保留表头
        ExcelReader reader = EasyExcel.read(excelStream)
                .headRowNumber(0)
                .registerReadListener(new RowWriteListener(writer))
                .build();
        try {
            reader.read(EasyExcel.readSheet(0).build());
        } finally {
            reader.finish();
            writer.flush();
        }
    }

    /**
     * 按 RFC 4180 转义字段
     * 字段中含有分隔符、双引号或换行时用双引号包裹，字段内的双引号写成两个双引号
     *
     * @param value 字段值
     * @return 转义后的字段
     */
    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needQuote = value.indexOf(DELIMITER) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * 行监听器，每读到一行 Excel 数据就写出一行 CSV
     */
    private static class RowWriteListener implements ReadListener<Map<Integer, String>> {

        /**
         * CSV 写出器
         */
        private final BufferedWriter writer;

        /**
         * 当前工作表中出现过的最大列下标，用于补齐右侧缺失的空单元格
         */
        private int maxColumnIndex = -1;

        RowWriteListener(BufferedWriter writer) {
            this.writer = writer;
        }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            // 表头行已按普通数据行输出，此处无需处理
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            if (data == null || data.isEmpty()) {
                // 跳过空行
                return;
            }
            writeRow(data);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 读取完成，无需额外处理
        }

        /**
         * 写出一行数据
         */
        private void writeRow(Map<Integer, String> row) {
            for (Integer columnIndex : row.keySet()) {
                if (columnIndex != null && columnIndex > maxColumnIndex) {
                    maxColumnIndex = columnIndex;
                }
            }
            StringBuilder line = new StringBuilder();
            for (int i = 0; i <= maxColumnIndex; i++) {
                if (i > 0) {
                    line.append(DELIMITER);
                }
                line.append(escape(row.get(i)));
            }
            try {
                writer.write(line.toString());
                writer.write(LINE_SEPARATOR);
            } catch (IOException e) {
                throw new IllegalStateException("写出 CSV 失败", e);
            }
        }
    }
}
