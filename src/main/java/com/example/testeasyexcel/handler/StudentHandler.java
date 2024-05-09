package com.example.testeasyexcel.handler;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;
import com.example.testeasyexcel.mapper.StudentMapper;
import com.example.testeasyexcel.model.Student;
import com.example.testeasyexcel.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.handler
 * @Author:UestcXiye
 * @CreateTime:2024-04-26 16:43:25
 * @Description:
 */

@Slf4j
@Component
public class StudentHandler extends AnalysisEventListener<Student> {

    static final String URL = "jdbc:mysql://localhost:3306/student";
    static final String USERNAME = "root";
    static final String PASSWORD = "12138";

    private static final int BATCH_COUNT = 100;
    // 缓存的数据
    private List<Student> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    @Autowired
    private StudentMapper studentMapper;

    public StudentHandler(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    @Override
    public void invoke(Student student, AnalysisContext analysisContext) {
        log.info("解析到一条数据:{}", JSON.toJSONString(student));
        cachedDataList.add(student);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            // saveData();
            batchSaveData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        // saveData();
        batchSaveData();
        log.info("所有数据解析完成！");
    }

    private void saveData() {
        if (!cachedDataList.isEmpty()) {
            // long startTime = System.currentTimeMillis(); // 开始时间
            for (Student student : cachedDataList) {
                try {
                    Student s = new Student();
                    BeanUtils.copyProperties(student, s);
                    studentMapper.insert(s.getStudentId(), s.getName(), s.getAge());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // long endTime = System.currentTimeMillis(); // 结束时间
            // 计算执行时间
            // System.out.printf("saveData 执行时长：%d 毫秒。", (endTime - startTime));
        }
    }
    private void batchSaveData() {
        if (!cachedDataList.isEmpty()) {
            // long startTime = System.currentTimeMillis(); // 开始时间
            try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
                String sql = "INSERT INTO info (studentId, name, age) VALUES (?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                for (Student student : cachedDataList) {
                    // 添加各个字段的内容
                    statement.setString(1, student.getStudentId());
                    statement.setString(2, student.getName());
                    statement.setInt(3, student.getAge());
                    statement.addBatch(); // 加入批处理
                }
                statement.executeBatch(); // 执行批处理
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // long endTime = System.currentTimeMillis(); // 结束时间
            // 计算执行时间
            // System.out.printf("batchSaveData 执行时长：%d 毫秒。", (endTime - startTime));
        }
    }
}
