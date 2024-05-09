package com.example.testeasyexcel.mapper;

import com.example.testeasyexcel.model.Student;
import org.apache.ibatis.annotations.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.demos.mapper
 * @Author:UestcXiye
 * @CreateTime:2024-04-15 14:53:10
 * @Description:
 */
@Mapper
public interface StudentMapper {
    @Insert("INSERT INTO info (studentId, name, age) VALUES (#{studentId}, #{name}, #{age})")
    int insert(@Param("studentId") String studentId,
               @Param("name") String name,
               @Param("age") int age);

    @Results({
            @Result(property = "studentId", column = "studentId"),
            @Result(property = "name", column = "name"),
            @Result(property = "age", column = "age")
    })
    @Select("SELECT studentId, name, age FROM info")
    List<Student> selectAll();

}
