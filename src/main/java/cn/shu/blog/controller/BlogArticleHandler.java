package cn.shu.blog.controller;

import cn.shu.blog.ArticleUtil;
import cn.shu.blog.beans.Article;
import cn.shu.blog.beans.Category;
import cn.shu.blog.beans.Comment;
import cn.shu.blog.beans.SearchArticle;
import cn.shu.blog.dao.CategoryMapper;
import cn.shu.blog.service.ArticleServiceInter;
import cn.shu.blog.service.CommentService;
import cn.shu.blog.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 文章 处理器
 * @作者 舒新胜
 * @项目 MyBlog
 * @创建时间 2020-5-7 19:10
 */
@Controller
@RequestMapping("/article")
public class BlogArticleHandler {
    /**
     * Article service
     */
    @Resource private ArticleServiceInter articleServiceInter = null;

    /**
     * comment of article service
     */
    @Resource private CommentService commentService = null;

    /**
     * category of article service
     */
    @Resource private CategoryMapper categoryMapper = null;

    /**
     * Office转pdf工具类
     */
    @Resource ArticleUtil articleUtil =null;

    /**
     * 获取热门文章列表
     * @param request 请求体
     * @return 请求转发对象
     */
    @RequestMapping("/hot.action")
    public String getHotArticle(HttpServletRequest request) {
        List<Article> hotArticles = articleServiceInter.getHotArticles();
        request.setAttribute("articles", hotArticles);

        return "forward:/WEB-INF/jsp/hotArticle.jsp";


    }

    /**
     * 查看文章详情
     * @param request 请求体
     * @param articleId 文章id
     * @return 请求转发对象
     */
    @RequestMapping("/detail/{articleId}.action")
    public String showArticleDetail(HttpServletRequest request, @PathVariable String articleId) {
        ServletContext servletContext = request.getServletContext();
        //获取每页显示的页数
        String pageCommNumStr = servletContext.getInitParameter("pageCommNum");
        int pageCommNum = 5;
        if (pageCommNumStr != null) {
            pageCommNum = Integer.parseInt(pageCommNumStr.trim());
        }

        //增加访问次数
        articleServiceInter.addVisitRecord(articleId);
        //获取文章信息
        Article article = articleServiceInter.getSingleArticle(articleId);
        if (article != null) {

            request.setAttribute("recommendArticles", new ArrayList<>());
            request.setAttribute("pageCommNum", pageCommNum);
            request.setAttribute("articleInfo", article);
            if (".pdf".equalsIgnoreCase(article.getFileType())) {
                //转换为swf方便显示 转换失败
                if (!articleUtil.turnPdf(article.getSourceFilePath(), article.getTargetFilePath())) {
                    return "forward:/WEB-INF/jsp/404.jsp";
                }
                return "forward:/WEB-INF/jsp/article.jsp";
                //html文件
            } else if (".html".equalsIgnoreCase(article.getFileType()) || ".htm".equalsIgnoreCase(article.getFileType())) {
                request.setAttribute("html",servletContext.getContextPath()+"/" +article.getTargetFilePath());
                return "forward:/WEB-INF/jsp/articleHtml.jsp";
            }

        }

        return "forward:/WEB-INF/jsp/404.jsp";
    }

    /**
     * 推荐与当前文章类似的文章
     * @param request 请求体
     * @param articleId 当前文章id
     * @param title 当前文章标题
     * @return 请求转发对象
     */
    @RequestMapping("/recommend.action")
    public String getRecommendArticle(HttpServletRequest request, String articleId, String title) {
        //获取推荐文章
        List<Article> recommendArticles = articleServiceInter.getRecommendArticles(articleId, title);
        request.setAttribute("recommendArticles", recommendArticles);
        return "forward:/WEB-INF/jsp/recommendArticle.jsp";
    }

    @RequestMapping("/comment.action")
    public String getComment(HttpServletRequest request, String currPage) {

        //获取每页加载的页数
        ServletContext servletContext = request.getServletContext();
        int pageCommNum = 5;
        if (servletContext.getInitParameter("pageCommNum") != null) {
            pageCommNum = Integer.parseInt(servletContext.getInitParameter("pageCommNum").trim());
        }


        String articleId = request.getParameter("articleId");
        //获取文章的评论
        List<Comment> articleComments = commentService.getArticleComments(articleId, currPage, pageCommNum);
        if (articleComments != null && articleComments.size() > 0) {
            request.setAttribute("pageCommNum", pageCommNum);
            request.setAttribute("articleComments", articleComments);
        }
        return "forward:/WEB-INF/jsp/comments.jsp";

    }

    /**
     * 前端提交articleId=" + articleId
     * 封装参数到Comment的 articleId参数
     */
    @ResponseBody
    @RequestMapping(value = "/addComment.action")
    public String addComment(HttpServletRequest request, Comment comment) {
        Object user = request.getSession().getAttribute("user");
        String s = commentService.articleComment(comment, user);
        return s;
    }

    @ResponseBody
    @RequestMapping(value = "/search.action", produces = "application/json")
    public SearchArticle searchArticle(String searchStr, String currPage, String pageNum) {
        return articleServiceInter.searchArticle(searchStr, Integer.parseInt(currPage), Integer.parseInt(pageNum));
    }

    @ResponseBody
    @RequestMapping(value = "/searchIas.action")
    public String searchArticleIAS(String searchStr, String currPage, String pageNum) {
        List<Article> articles = articleServiceInter.searchArticle(searchStr, Integer.parseInt(currPage), Integer.parseInt(pageNum)).getArticles();
        StringBuilder stringBuilder = new StringBuilder();
        for (Article article : articles) {
            //获取文章分类
            Category category = categoryMapper.selectByPrimaryKey(article.getCategoryId());
            //获取文章评论
            Comment comment = new Comment();
            comment.setArticleId(article.getId());
            List<Comment> comments = commentService.selectByAll(comment);
            stringBuilder.append(
                    "<article class='excerpt excerpt-1'>" + "<a class='focus' href='article.jsp' title=''> "
                            + "<img class='thumb' data-original='/images/logo.png' src=/")
                    .append(article.getImagePath()).append(" /> ").append("</a>").append("<header>")
                    .append("<a class='cat' href='program'>")
                    .append(category.getCategoryName()).append("<i></i></a>").append("<h2><a href='article.jsp' title=''>").append(article.getTitle())
                    .append("</a></h2>").append("</header>").append("<p class='meta'>")
                    .append("<time class='time'><i class='glyphicon glyphicon-time'></i>")
                    .append(DateUtil.formatDate(article.getUpdateDate(),"yyyy-MM-dd")).append("</time>")
                    .append("<span class='views'><i class='glyphicon glyphicon-eye-open'></i> ")
                    .append(article.getVisitors()).append(" 人围观</span>").append("<a class='comment' href='article.jsp#comment'><i class='glyphicon glyphicon-comment'></i> ")
                    .append(comments.size()).append(" 评论</a>")
                    .append("</p>").append("<p class='note'>")
                    .append(article.getDescription()).append("</p>").append("</article>");
        }
        return stringBuilder.toString();
    }
}
