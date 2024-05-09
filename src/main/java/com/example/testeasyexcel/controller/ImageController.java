package com.example.testeasyexcel.controller;

import com.example.testeasyexcel.service.ImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

import static java.util.Objects.isNull;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.controller
 * @Author:UestcXiye
 * @CreateTime:2024-04-30 09:26:31
 * @Description:
 */
@Api(tags = "生成学生信息图片")
@Controller
@RequestMapping("/iamge")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @ApiOperation("生成图片")
    @GetMapping("/getStudentInfoImage")
    public String getStudentInfoImage(HttpServletResponse response) {
        try {
            byte[] studentInfoImage = imageService.getStudentInfoImage();
            if (isNull(studentInfoImage)) {
                response.setContentType("application/json");
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            response.getOutputStream().write(studentInfoImage);
            response.getOutputStream().flush();
            return "生成图片成功";
        } catch (Exception e) {
            return "生成图片失败：" + e.getMessage();
        }
    }
}
