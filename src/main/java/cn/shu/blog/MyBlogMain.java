package cn.shu.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @作者 舒新胜
 * @项目 MyBlog
 * @创建时间 2020-5-22 18:01
 */
@SpringBootApplication
@MapperScan("cn.shu.blog.dao")
//开启监听器包扫描
//修改代码测试GIT
@ServletComponentScan(basePackages = "cn.shu.blog.listener.*")
public class MyBlogMain {
    public static void main(String[] args) {
        SpringApplication.run(MyBlogMain.class,args);
    }

}
