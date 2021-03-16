package cn.shu.blog.service;

import cn.shu.blog.beans.*;
import cn.shu.blog.dao.ArticleMapper;
import cn.shu.blog.dao.CategoryMapper;
import cn.shu.blog.dao.CommentMapper;
import cn.shu.blog.dao.UserMapper;
import cn.shu.blog.utils.*;
import cn.shu.blog.utils.lucene.LuceneIndexUntil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

@Service("articleServiceInter") //Spring管理
@Scope(value = "singleton") //单例
@Lazy //懒加载
@Slf4j
public class ArticleService implements ArticleServiceInter {
    //自动注入，首先寻找 ArticleDaoInter类型的(包括实现了的类)，找到唯一注入，
    //没找到或找到多个则按id(articleDaoInter)查找,查找到则注入

    @Resource
    private SpringBootJarUtil springBootJarUtil = null;

    @Autowired(required = false)

    private ArticleMapper articleMapper = null;

    @Autowired(required = false)
    private UserMapper userMapper = null;

    @Autowired(required = false)
    private CategoryMapper categoryMapper = null;

    @Autowired(required = false)
    private CommentMapper commentMapper = null;
    //索引保存位置
    private String savePath = "/article_index";

    @PostConstruct  //初始化方法
    public void init() {

    }

    @PreDestroy  //销毁方法
    public void destroy() {

    }

    /**
     * 增加访问次数
     *
     * @param articleId 文章ID
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void addVisitRecord(String articleId) {
        if (StringUtils.isBlank(articleId)) {
            return;
        }
        articleMapper.addVisitRecord(Integer.parseInt(articleId));
    }

    /**
     * 获取单个文章信息
     *
     * @param articleId 文章ID
     * @return 文章对象列表
     */
    @Override
    public Article getSingleArticle(String articleId) {
        if (StringUtil.isEmpty(articleId)) {
            return null;
        }
        Article article = articleMapper.selectByPrimaryKey(Integer.parseInt(articleId));
        User user = userMapper.selectByPrimaryKey(article.getUserId());
        Category category = categoryMapper.selectByPrimaryKey(article.getCategoryId());
        Comment comment = new Comment();
        comment.setArticleId(article.getId());
        List<Comment> comments = commentMapper.selectByAll(comment);
        article.setNickname(user.getNickname());
        article.setCommNum((long)comments.size());
        article.setCategoryName(category.getCategoryName());
        article.setCommentList(comments);
        return article;
    }

    /**
     * 获取热门文章
     * @return 热门文章
     */
    @Override
    public List<Article> getHotArticles() {
        PageHelper.startPage(1,5);
        PageInfo<Article> articlePageInfo = new PageInfo<>(articleMapper.getHotArticles());
        return articlePageInfo.getList();
    }

    /**
     * 获取文章数量
     * @return 文章数量
     */
    @Override
    public int getArticlesCount(String categoryId) {
        if (StringUtil.isEmpty(categoryId)) {
            return articleMapper.getArticlesCount(-1);
        } else {
            return articleMapper.getArticlesCount(Integer.parseInt(categoryId));

        }
    }

    /**
     * 获取所有文章信息
     * @return 查询
     */
    @Override
    public List<Article> getArticles(int currPage, String categoryId) {
        if (currPage < 1) {
            return null;
        }
        //开始记录
        int recordFirst = ((currPage - 1) * 5);
        List<Article> articles = null;
        //显示全部分类
        if (StringUtil.isEmpty(categoryId) || "-1".equals(categoryId)) {
            //-1为所有
            articles = articleMapper.getArticles(recordFirst, 5, null);
        } else {
            articles = articleMapper.getArticles(recordFirst, 5, Integer.parseInt(categoryId));
        }

        if (articles != null && !articles.isEmpty()) {
            //下载文章封面图片
            downloadImg(articles);
        }
        return articles;
    }

    /**
     * 下载封面图片
     *
     * @param articles 文章列表
     */
    private void downloadImg(List<Article> articles) {
        new Thread(() -> {
            //获取不重复的所有图片
            HashSet<String> set = new HashSet<>();
            for (Article article : articles) {
                set.add(article.getImagePath());
            }

            for (String imgRelativePath : set) {
                //判断封面图片是否存在
                //图片绝对路径
                String imageAbsolutePath = null;
                String imgStr = null;
                String fileName = null;
                String fileNameAndExt;
                try {
                    //文章展示图片文件完整路径
                    imgStr = springBootJarUtil.getExtStaticSources() + imgRelativePath;
                    File imageFile = new File(imgStr);
                    //存放文件名，包括后缀
                    fileNameAndExt = imageFile.getName();
                    //文件名,没有后缀
                    fileName = fileNameAndExt.substring(0, fileNameAndExt.lastIndexOf("."));

                    //图片存放路径，不包括文件名
                    imageAbsolutePath = imageFile.getParent();
                    File imagePathFile = new File(imageAbsolutePath);
                    //保存图片的目录不存在
                    if (!imagePathFile.exists()) {
                        boolean mkdirs = imagePathFile.mkdirs();
                        //创建失败
                        if (!mkdirs) {
                            log.warn("图片保存目录创建失败：" + imageAbsolutePath);
                            return;
                        }
                    }
                    //内部和外部资源都不存在
                    if (!springBootJarUtil.sourceExists(File.separator + imgRelativePath) && !imageFile.exists()) {
                        //1代表下载一页，一页一般有30张图片
                        File imgFile = GetNetImage.getPictures(fileName, 20, imageAbsolutePath, fileNameAndExt);
                        log.info("图片下载成功：" + imgFile.getAbsolutePath());
                    }
                } catch (FileNotFoundException e) {
                    log.warn(e.getMessage());
                    e.printStackTrace();
                }

            }
        }).start();

    }

    /**
     * 获取与当前文章相关的推荐文章
     * 按当前浏览文章的标题，分词
     * 分词后在数据库查找包含 这几个词的标题对应的文章
     * 按出现的词语数量 排序
     *
     * @return 匹配度最高的5篇文章
     */
    @Override
    public List<Article> getRecommendArticles(String currArticleId, String currArticleTitle) {
        //Article currArticle = getSingleArticle(currArticleId);
        if (StringUtil.isEmpty(currArticleTitle) || StringUtil.isEmpty(currArticleId)) {
            return null;
        }
        String[] fields = {"title"};
        try {
            //布尔查询子条件
            Query multiQuery = LuceneIndexUntil.newMultiQuery(fields, currArticleTitle);
            TermQuery termQuery = new TermQuery(new Term("id", currArticleId));

            //封装子条件
            BooleanClause booleanClause = new BooleanClause(multiQuery, BooleanClause.Occur.MUST);
            //推荐文章不包括当前文章
            BooleanClause booleanClause1 = new BooleanClause(termQuery, BooleanClause.Occur.MUST_NOT);

            Query query = LuceneIndexUntil.newBooleanQuery(booleanClause, booleanClause1);
            return LuceneIndexUntil.searchDocument(null, query, 1, 10, springBootJarUtil.getExtStaticSources() + savePath, 1).getArticles();
        } catch (ParseException | FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }





    /**
     * @param searchStr 查询关键字
     * @param currPage  当前第几页
     * @param pageNum   一页几条数据
     * @return
     */
    @Override
    public SearchArticle searchArticle(String searchStr, Integer currPage, Integer pageNum) {
        //根据title、content搜索
        String[] fields = new String[]{"title", "description"};
        try {
            //查询条件
            Query query = LuceneIndexUntil.newMultiQuery(fields, searchStr);
            //查询10条
            SearchArticle searchArticle = LuceneIndexUntil
                    .searchDocument(searchStr, query, currPage, pageNum, springBootJarUtil.getExtStaticSources() + savePath, 0);

            log.info("搜索结果:" + searchArticle.getArticles().size() + " " + searchArticle);

            return searchArticle;
        } catch (ParseException | FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
    @Override
    public int deleteByPrimaryKey(Integer id) {
        return articleMapper.deleteByPrimaryKey(id);
    }
    @Override
    public int insert(Article record) {
        return articleMapper.insert(record);
    }
    @Override
    public int insertOrUpdate(Article record) {
        return articleMapper.insertOrUpdate(record);
    }
    @Override
    public int insertOrUpdateSelective(Article record) {
        return articleMapper.insertOrUpdateSelective(record);
    }
    @Override
    public int insertSelective(Article record) {
        return articleMapper.insertSelective(record);
    }
    @Override
    public Article selectByPrimaryKey(Integer id) {
        return articleMapper.selectByPrimaryKey(id);
    }
    @Override
    public int updateByPrimaryKeySelective(Article record) {
        return articleMapper.updateByPrimaryKeySelective(record);
    }
    @Override
    public int updateByPrimaryKey(Article record) {
        return articleMapper.updateByPrimaryKey(record);
    }
    @Override
    public int updateBatch(List<Article> list) {
        return articleMapper.updateBatch(list);
    }
    @Override
    public int updateBatchSelective(List<Article> list) {
        return articleMapper.updateBatchSelective(list);
    }
    @Override
    public int batchInsert(List<Article> list) {
        return articleMapper.batchInsert(list);
    }
}

