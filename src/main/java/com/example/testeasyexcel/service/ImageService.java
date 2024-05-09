package com.example.testeasyexcel.service;

import com.alibaba.fastjson.JSONArray;
import com.example.testeasyexcel.entity.Cell;
import com.example.testeasyexcel.mapper.StudentMapper;
import com.example.testeasyexcel.model.Student;
import com.example.testeasyexcel.util.DrawTableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.service
 * @Author:UestcXiye
 * @CreateTime:2024-04-30 09:33:12
 * @Description:
 */

@Service
public class ImageService {

    @Autowired(required = false)
    private StudentMapper studentMapper;

    public byte[] getStudentInfoImage() {
        // 存储各列名称的列表
        List<String> columnList = new ArrayList<>();
        columnList.add("学号");
        columnList.add("姓名");
        columnList.add("年龄");

        // 获取所有学生信息
        List<Student> studentList = this.studentMapper.selectAll();
        if (studentList == null)
            return null;
        // 存储数据的数组
        JSONArray jsonArray = new JSONArray();
        com.alibaba.fastjson.JSONObject defaultJson = new com.alibaba.fastjson.JSONObject(true);
        for (String column : columnList)
            defaultJson.put(column, 0);

        for (Student student : studentList) {
            com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject(true);
            jsonObject.putAll(defaultJson);
            jsonObject.put("学号", student.getStudentId());
            jsonObject.put("姓名", student.getName());
            jsonObject.put("年龄", student.getAge());
            jsonArray.add(jsonObject);
        }

        // 图表标题和各列的名称
        List<Cell> headCells = new ArrayList<>();
        headCells.add(new Cell(1, 1, 100, 1).setTextAlign(true).setContent("学生信息汇总"));
        for (int i = 0; i < columnList.size(); i++) {
            String columnName = columnList.get(i);
            headCells.add(new Cell(2, i + 1, 100, 1).setTextAlign(true).setContent(columnName));
        }
        return DrawTableUtil.getStudentInfoImage(jsonArray, headCells);
    }
}
