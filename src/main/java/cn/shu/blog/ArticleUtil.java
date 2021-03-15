package cn.shu.blog;

import cn.shu.blog.beans.Article;
import cn.shu.blog.beans.Category;
import cn.shu.blog.dao.ArticleMapper;
import cn.shu.blog.dao.CategoryMapper;
import cn.shu.blog.utils.*;
import cn.shu.blog.utils.lucene.LuceneUtilForArticle;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author 舒新胜
 * @作者 舒新胜
 * @项目 MyBlog
 * @创建时间 2020-5-21 20:34
 */
@Component
@Slf4j
public class ArticleUtil {
    @Resource
    private SpringBootJarUtil springBootJarUtil = null;
    /**
     * pdf转pdf工具路径
     */
    @Value("${swfToolsPath}")
    private  String swfToolsPath = "D:/SWFTools/pdf2swf.exe";

    /**获取配置文件的外部静态资源路径*/
    @Value("${extStaticSourcesPath}")
    private String extStaticSourcesPath = null;

    /**
     * 资源文件根路径
     */
    private  String rootPath = "";

    /**
     * 文章mapper
     */
    @Resource
    ArticleMapper articleMapper = null;

    /**
     * 分类mapper
     */
    @Resource
    CategoryMapper categoryMapper = null;
    /**
     * 文章对象列表
     */
    private List<Article> articleList = new ArrayList<>();

    @Test
    public void scanArticle() throws IOException {
        //rootPath = springBootJarUtil.getJarPath()+extStaticSourcesPath;
        rootPath = springBootJarUtil.getExtStaticSources();
        //1、创建文件对象
        createArticles(rootPath, "/note");

        //2、插入数据库
        insertArticles(articleMapper,categoryMapper);

        //3、转换swf
        for (Article article : articleList) {
            if (".swf".equals(article.getFileType())){
                turnSwf(rootPath+article.getSourceFilePath(),rootPath+article.getTargetFilePath());
            }

        }
        //4、创建索引
        LuceneUtilForArticle.createIndex(rootPath,articleList);

    }

    /**
     * 插入文章信息
     * @throws IOException
     */
    private void insertArticles(ArticleMapper articleMapper,CategoryMapper categoryMapper) throws IOException {
        //1、加载配置文件
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        //2、创建连接工厂
        SqlSessionFactory build = new SqlSessionFactoryBuilder().build(inputStream);
        //3、获取连接
        SqlSession sqlSession = build.openSession();
        //4、获取mapper
        if (articleMapper == null){
            articleMapper = sqlSession.getMapper(ArticleMapper.class);
        }
        if (categoryMapper == null){
            categoryMapper = sqlSession.getMapper(CategoryMapper.class);
        }

        for (Article article : articleList) {
            //添加分类，并获取分类id
            HashMap<String, Object> params = new HashMap<>();
            params.put("categoryName", article.getCategoryName());
            List<Category> categories = categoryMapper.selectByAll(params);
            if (categories.size() > 0) {
                article.setCategoryId(categories.get(0).getId());
            } else {
                Category category = new Category();
                category.setCategoryName(article.getCategoryName());
                category.setId(null);
                categoryMapper.insert(category);
                article.setCategoryId(category.getId());
            }
            //新增文章
            Article queryArticle = new Article();
            queryArticle.setSourceFilePath(article.getSourceFilePath());
            List<Article> articles = articleMapper.selectByAll(queryArticle);
            if (articles.isEmpty()) {
                articleMapper.insert(article);
            }

        }
        articleList = articleMapper.getArticlesForIndex();
        sqlSession.commit();
        sqlSession.close();
    }


    private void createArticles(String path, String docDir) {
        //递归遍历path下所有文件
        File dir = new File(path + docDir);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                //保存文件信息到数据库
                //转pdf、swf
                try {
                    createArticleObject(file);
                } catch (Exception e) {
                }
            } else if (file.isDirectory()) {
                //遍历子目录
                if (file.getAbsolutePath().endsWith("2020最新Git教程（2小时从入门到精通）")){
                    continue;
                }
                createArticles(file.getAbsolutePath(), "/");
            }
        }
    }

    private void createArticleObject(File file) {

        /*2020-04-22_后端_JAVA_常用设计模式
         *按_分隔 第一部分为创建日期[0]
         * 第一部分为category [1]
         * 第三部分为tag     [2]
         * 第四部分为标题 [4]
         */
        //获取文件名 不包括扩展名
        String filenameAndExt = file.getName();
        int pointLoc = filenameAndExt.lastIndexOf(".");
        String filename = filenameAndExt.substring(0, pointLoc);
        //获取扩展名
        String ext = filenameAndExt.substring(pointLoc + 1).trim();

        //分割
        String[] fileInfo = filename.split("_");

        //创建日期
        String createDate = fileInfo[0].trim();

        //标签
        String tag = fileInfo[2].trim();
        //long categoryId = insertCategoryName(tag);
        //文件类型
        String fileType = "." + ext;
        //标题
        String title = fileInfo[3].trim();
        //最后修改时间
        String updateDate = DateUtil.formatDate(new Date(file.lastModified()), "yy-MM-dd");
        //源文件目录
        String sourceFilePath = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(rootPath) + rootPath.length());
        //目标文件目录 如果是docx或doc转为swf
        String targetFilePath = "/" + ext + "/" + filename + fileType;
        if ("docx".equalsIgnoreCase(ext) || "doc".equalsIgnoreCase(ext)) {
                 fileType = ".swf";
            targetFilePath = "/swf/" + MD5.md5(filename) + fileType;
             /* //转html使用
             fileType = ".html";
            targetFilePath = "/html/"+ UUID.randomUUID()+"/" + MD5.md5(filename) + fileType;*/
        }

        //描述读取文档内容
        String desc;
        if ("docx".equalsIgnoreCase(ext) || "doc".equalsIgnoreCase(ext)) {
            desc = readWord(rootPath + sourceFilePath).trim();
        } else {
            desc = HtmlUtil.readHtml(rootPath + sourceFilePath, 200).trim();
            targetFilePath = sourceFilePath;
        }

        //展示图 以tag为标记
        String imgPath = "images/articles/" + tag + ".jpg";

        Article article = new Article();
        article.setTitle(title);
        article.setCreateDate(DateUtil.stringToDate(createDate));
        article.setUpdateDate(DateUtil.stringToDate(updateDate));
        // article.setCategoryId((int)categoryId);
        article.setCategoryName(tag);
        article.setFileType(fileType);
        article.setTargetFilePath(targetFilePath);
        article.setSourceFilePath(sourceFilePath);
        article.setDescription(desc);
        article.setImagePath(imgPath);
        article.setUserId(33);
        article.setVisitors(0);
        //没有日期的文章不需要添加到数据库
        if (article.getCreateDate() != null) {
            articleList.add(article);
        }


    }

    private  boolean turnSwf(String pathDoc, String pathSwf) {

        File file = new File(pathSwf);
        if (file.exists() || file.isDirectory()) {
            return true;
        }
        log.info("源DOC文件:{}" , pathDoc);

        String pathPdf = pathSwf.substring(0, pathSwf.lastIndexOf(".")) + ".pdf";
        //转html使用
        //pathPdf = pathSwf.substring(0, pathSwf.lastIndexOf(".")) + ".html";
        file = new File(pathPdf);
        if (!file.exists() && !file.isDirectory()) {
            log.info("生成pdf文件:{}", pathPdf);
            //docx转pdf
            //Doc2Pdf.turn(pathDoc, pathPdf);
            Doc2Pdf.turn(pathDoc, pathPdf);
        }


        //pdf转swf
        log.info("转换swf中...");
        log.info("生成Swf文件:" + pathSwf);
        return Pdf2Swf.pdf2swf(pathPdf, pathSwf, swfToolsPath);
    }

    /**
     * 读取word文件内容
     *
     * @param path word文件路径
     * @return buffer
     */
    private String readWord(String path) {
        // TODO Auto-generated method stub
        StringBuilder content = new StringBuilder();
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(path);
            XWPFDocument doc = new XWPFDocument(inStream);
            //获取所有段落
            List<XWPFParagraph> paras = doc.getParagraphs();
            //遍历段落

            for (XWPFParagraph para : paras) {
                //当前段落的属性
                //CTPPr pr = para.getCTP().getPPr();
                //获取段落文本
                if (content.length() > 200) {
                    break;
                }
                content.append(para.getText().trim()).append(",");

            }
            return content.substring(0, 200);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            log.info("获取word文档内容错误:" + e.getMessage());
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }

    public  boolean turnSwfJar(String pathDoc, String pathSwf) {
        try {
            //jar内部资源存在SWF文件
            if (springBootJarUtil.sourceExists(pathSwf)) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        log.warn("内部资源文件不存在:" + pathSwf);
        String pathPdf = pathSwf.replace(".swf", ".pdf");
        ;
        pathPdf = pathPdf.replace("/swf/", "/pdf/");
        try {
            pathDoc = springBootJarUtil.getExtStaticSources() + pathDoc;
            pathPdf = springBootJarUtil.getExtStaticSources() + pathPdf;
            pathSwf = springBootJarUtil.getExtStaticSources() + pathSwf;
            boolean mkdirs = new File(pathPdf).getParentFile().mkdirs();
            boolean mkdirs1 = new File(pathSwf).getParentFile().mkdirs();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        //jar外部资源swf文件存在
        File file = new File(pathSwf);
        if (file.exists() && !file.isDirectory()) {
            return true;
        }
        log.warn("外部资源文件不存在:" + pathSwf);
        log.info("源DOC文件:" + pathDoc);
        file = new File(pathPdf);
        if (!file.exists() && !file.isDirectory()) {
            log.info("生成pdf文件:" + pathPdf);
            //docx转pdf
            int b = Doc2Pdf.turn(pathDoc, pathPdf);
            if (b != 0) {
                log.error("生成pdf失败");
                return false;
            }
        }

        //pdf转swf
        log.info("转换swf中..." + pathSwf);
        boolean b = Pdf2Swf.pdf2swf(pathPdf, pathSwf, swfToolsPath);
        if (!b) {
            log.error("转换swf失败:" + pathSwf);
        }
        return b;
    }
}
