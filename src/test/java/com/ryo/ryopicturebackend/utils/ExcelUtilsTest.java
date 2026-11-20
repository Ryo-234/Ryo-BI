package com.ryo.ryopicturebackend.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.ryo.ryopicturebackend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Excel 转 CSV 工具类测试
 */
class ExcelUtilsTest {

    /**
     * 构建测试用的 Excel 文件，包含中文、数字、需要转义的字段以及缺失的单元格
     */
    private MultipartFile buildExcelFile() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("姓名", "年龄", "城市", "备注"));
        data.add(Arrays.asList("张三", 18, "北京", "喜欢,吃苹果"));
        data.add(Arrays.asList("李四", 25, "上海", "他说\"你好\""));
        // 缺失的单元格
        data.add(Arrays.asList("王五", null, "广州", null));
        EasyExcel.write(out).sheet("测试数据").doWrite(data);
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
    }

    @Test
    void excelToCsv_should_convert_all_rows_and_keep_table_header() throws IOException {
        String csv = ExcelUtils.excelToCsv(buildExcelFile());

        String[] lines = csv.split("\n");
        assertEquals(4, lines.length, "应有表头 + 3 行数据，实际内容：" + csv);
        assertEquals("姓名,年龄,城市,备注", lines[0]);
        assertEquals("张三,18,北京,\"喜欢,吃苹果\"", lines[1]);
        assertEquals("李四,25,上海,\"他说\"\"你好\"\"\"", lines[2]);
        // 缺失的单元格补空，保证每行列数一致
        assertEquals("王五,,广州,", lines[3]);
    }

    @Test
    void excelToCsv_should_support_xls_format() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("姓名", "城市"));
        data.add(Arrays.asList("张三", "北京"));
        ExcelWriter xlsWriter = EasyExcel.write(out).excelType(ExcelTypeEnum.XLS).build();
        xlsWriter.write(data, EasyExcel.writerSheet("测试数据").build());
        xlsWriter.finish();

        MultipartFile xlsFile = new MockMultipartFile("file", "test.xls", null, out.toByteArray());
        String csv = ExcelUtils.excelToCsv(xlsFile);
        assertEquals("姓名,城市\n张三,北京\n", csv);
    }

    @Test
    void excelToCsv_should_throw_exception_when_file_is_empty() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx", null, new byte[0]);
        BusinessException exception = assertThrows(
                BusinessException.class, () -> ExcelUtils.excelToCsv(emptyFile));
        assertEquals("上传文件为空，无法转换", exception.getMessage());
    }
}
