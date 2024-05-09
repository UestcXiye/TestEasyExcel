package com.example.testeasyexcel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.demos.controller
 * @Author:UestcXiye
 * @CreateTime:2024-04-16 21:15:03
 * @Description:
 */

@Controller
public class SpringBootController {
    @RequestMapping(value = "/springBoot/index")
    @ResponseBody
    public String index(){
        return "Hello World!";
    }
}
