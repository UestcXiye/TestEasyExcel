package com.example.testeasyexcel.controller;

import com.example.testeasyexcel.service.StudentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.controller
 * @Author:UestcXiye
 * @CreateTime:2024-04-26 15:08:17
 * @Description:
 */

@Api(tags = "学生信息导入、导出")
@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @ApiOperation("导入")
    @PostMapping(value ="/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String importStudent(@RequestPart("file") MultipartFile file) {
        boolean flag = studentService.importStudent(file);
        if (flag) {
            return "导入成功";
        } else {
            return "导入失败";
        }
    }

    @ApiOperation("导出")
    @GetMapping("/export")
    public String exportStudent(HttpServletResponse response) throws IOException {
        boolean flag = studentService.exportStudent(response);
        if (flag) {
            return "导出成功";
        } else {
            return "导出失败";
        }
    }
}
