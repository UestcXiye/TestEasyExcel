package com.example.testeasyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.fastjson.JSON;
import com.example.testeasyexcel.listener.StudentListener;
import com.example.testeasyexcel.model.Student;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SpringBootTest
@Slf4j
class TestEasyExcelApplicationTests {

    @Test
    public void testReadStudentMethod1() throws URISyntaxException {
        // String fileName = getReadExcelUrl();
        String fileName = getExcelUrl("readExcel.xlsx");
        // System.out.println(fileName);
        // 这里默认每次会读取100条数据 然后返回过来 直接调用使用数据就行
        // 具体需要返回多少行可以在`PageReadListener`的构造函数设置
        EasyExcel.read(fileName, Student.class, new PageReadListener<Student>(dataList ->{
            for(Student data : dataList){
                log.info("读取到一条数据{}", JSON.toJSONString(data));
            }
        })).sheet().doRead();
    }

    @Test
    public void testReadStudentMethod2() throws URISyntaxException {
        String fileName = getExcelUrl("readExcel.xlsx");
        // 有个很重要的点 StudentListener 不能被 spring 管理，
        // 每次读取 excel 都要new，然后里面用到 spring 可以构造方法传进去
        // 这里需要指定读用哪个 class 去读，然后读取第一个 sheet 文件流会自动关闭
        EasyExcel.read(fileName, Student.class, new StudentListener()).sheet().doRead();
    }

    @Test
    public void testWriteStudent() throws URISyntaxException {
        String fileName = getWriteExcelUrl() + "testWrite" + ".xlsx";
        // 这里需要指定写用哪个 class 去写，然后写到第一个 sheet，名字为 easyexcel
        EasyExcel.write(fileName, Student.class).sheet("easyexcel")
                .doWrite(data());
    }

    private List<Student> data() {
        ArrayList<Student> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Student student = new Student("2018080901006", "测试人员", 22 + i);
            list.add(student);
        }
        return  list;
    }

    // 获取指定 docName 的 excel 文件目录地址
    private String getExcelUrl(String docName) throws URISyntaxException {
        return this.getClass().getClassLoader().getResource("").toURI().getPath()  + docName;
    }
    private String getWriteExcelUrl() throws URISyntaxException {
        return this.getClass().getClassLoader().getResource("").toURI().getPath();
    }
}
