package com.example.testeasyexcel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.fastjson.JSON;
import com.example.testeasyexcel.handler.StudentHandler;
import com.example.testeasyexcel.mapper.StudentMapper;
import com.example.testeasyexcel.model.Student;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.demos.service
 * @Author:UestcXiye
 * @CreateTime:2024-04-20 22:24:33
 * @Description:
 */
@Service
public class StudentService {
    @Autowired(required = false)
    private StudentMapper studentMapper;

    /** 导入学生信息
     * @param file excel文件
     * @return
     */
    public boolean importStudent(MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(), Student.class, new StudentHandler(studentMapper)).sheet().doRead();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportStudent(HttpServletResponse response) throws IOException {
        try {
            // 设置相关参数
            // response.setContentType("application/vnd.ms-excel");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里 URLEncoder.encode 可以防止中文乱码，当然和 easyexcel 没有关系
            String fileName = URLEncoder.encode("学生信息", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=uft-8''"+ fileName + ".xlsx");
            // 获取文件
            List<Student> studentList = this.studentMapper.selectAll();
            // 写出
            EasyExcel.write(response.getOutputStream(), Student.class).autoCloseStream(Boolean.FALSE).sheet("sheet1").doWrite(studentList);
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, String> map = MapUtils.newHashMap();
            map.put("code", "-1");
            map.put("message", "导出文件失败：" + e.getMessage());
            response.getWriter().println((JSON.toJSONString(map)));
            return false;
        }
    }
}
