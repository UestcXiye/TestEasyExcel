package com.example.testeasyexcel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;
import com.example.testeasyexcel.model.Student;
import com.example.testeasyexcel.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.listener
 * @Author:UestcXiye
 * @CreateTime:2024-04-25 22:31:49
 * @Description:
 */

@Slf4j
@Component
public class StudentListener implements ReadListener<Student> {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 100;
    /**
     * 缓存的数据
     */
    private List<Student> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    //private StudentService studentService;
    //
    //public StudentListener() {
    //    studentService = new StudentService();
    //}
    //
    //public StudentListener(StudentService studentService) {
    //    this.studentService = studentService;
    //}

    // 在解析 Excel 过程中发生异常时调用的方法。可以在该方法中记录日志或者进行异常处理等操作
    @Override
    public void onException(Exception e, AnalysisContext analysisContext) throws Exception {

    }

    // 在读取 Excel 文件表头时调用的方法。可用于对表头进行校验或者记录日志等操作
    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {

    }

    /**
     * 这个函数每一条数据解析都会来调用
     *
     * @param student    one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(Student student, AnalysisContext context) {
        log.info("解析到一条数据:{}", JSON.toJSONString(student));
        cachedDataList.add(student);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            printData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    // 在读取 Excel 文件中除数据外的其他内容时调用的方法。例如，批注、超链接等。可以在该方法中进行相应的处理
    @Override
    public void extra(CellExtra cellExtra, AnalysisContext analysisContext) {

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        printData();
        log.info("所有数据解析完成！");
    }

    // 判断是否还有下一条数据需要读取。
    // 如果返回 true，会自动调用 invoke(T data, AnalysisContext analysisContext) 方法来读取下一条数据；
    // 如果返回 false，则结束读取数据的过程。
    @Override
    public boolean hasNext(AnalysisContext analysisContext) {
        return true;
    }

    private void printData() {
        cachedDataList.stream().forEach(System.out::println);
    }
}
