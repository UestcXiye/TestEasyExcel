package com.example.testeasyexcel.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.demos.model
 * @Author:UestcXiye
 * @CreateTime:2024-04-13 11:11:19
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Student {
    @ExcelProperty(value = "学生学号", index = 0)
    private String studentId;
    @ExcelProperty(value = "学生姓名", index = 1)
    private String name;
    @ExcelProperty(value = "学生年龄", index = 2)
    private int age;
}
