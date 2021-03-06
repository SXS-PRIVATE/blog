package cn.shu.blog.utils.lucene;

import cn.shu.blog.beans.Article;
import cn.shu.blog.beans.SearchArticle;
import cn.shu.blog.utils.DateUtil;
import cn.shu.blog.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @作者 舒新胜
 * @项目 blog
 * @创建时间 2020/6/6 18:24
 */
@Slf4j
public class LuceneIndexUntil {

    private static HashMap<String, String> getTerm(Analyzer a, String msg, String title, String description) throws IOException {
        if (msg != null) {


            //调用a这个分词器的api将需要分词的数据计算成词项
            //分词不能独立存在,需要依托document数据，这里"document"模拟为文档对象
            //实际 “document”就是定义的域属性
            TokenStream tokenStream = a.tokenStream("document", msg);

            //上面分词计算后 指针指向这里需要重置回到开头
            tokenStream.reset();

            //获取分词后词项的文本属性及偏移量属性
            CharTermAttribute charAttr = tokenStream.getAttribute(CharTermAttribute.class);
            OffsetAttribute offAttr = tokenStream.getAttribute(OffsetAttribute.class);

            //遍历词项
            while (tokenStream.incrementToken()) {
            /*log.info("偏移量起始位置:"+offAttr.startOffset());
             log.info("偏移量结束位置:"+offAttr.endOffset());*/

                // System.out.print(s + ",");
                String s = charAttr.toString();
                title = title.replaceAll("(?i)" + s, "<font color='#e33e33'>" + s + "</font>");
                description = description.replaceAll("(?i)" + s, "<font color='#e33e33'>" + s + "</font>");
            }
        }
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("title", title);
        stringStringHashMap.put("description", description);
        return stringStringHashMap;
    }


    /**
     * 给文章创建lucene索引
     * 索引存在则更新，不存在则添加
     *
     * @param articles 文章List
     * @param pathStr  索引，目录
     * @param update   是更新还是新增
     */
    public static void createIndex(List<Article> articles, String pathStr, boolean update) {
        IndexWriter indexWriter = null;
        FSDirectory dir = null;
        try {
            //1、创建一个路径对象
            Path path = Paths.get(pathStr);
            //2、将路径对象交给lucene管理
            dir = FSDirectory.open(path);

            //3、创建一个输出流配置对象，需要指定一个分词器，这里使用IK分词器
            IndexWriterConfig writerConfig = new IndexWriterConfig(new IKAnalyzer6x());
            //配置每次创建索引的模式 每次创建后追加
            writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            //4、创建索引输出流
            indexWriter = new IndexWriter(dir, writerConfig);
            //5、封装数据到document对象
            for (Article article : articles) {
                Document doc = new Document();
                //获取文章评论
                //  List<Comment> comments = commentService.selectByAll(Comment.builder().articleId(article.getId()).build());
                doc.add(new TextField("title", article.getTitle(), Field.Store.YES));
                // doc.add(new StringField("content", "lucene是一种全文检索技术的工具包...", Field.Store.YES));
                doc.add(new StringField("id", article.getId() + "", Field.Store.YES));
                doc.add(new TextField("description", article.getDescription() + "", Field.Store.YES));
                doc.add(new IntPoint("visitors", article.getVisitors()));
                doc.add(new StringField("visitors", article.getVisitors() + "", Field.Store.YES));
                doc.add(new StringField("updateDate", DateUtil.formatDate(article.getUpdateDate(), null), Field.Store.YES));
                // doc.add(new StringField("commNum", article.getCommNum() + "", Field.Store.YES));
                //doc.add(new StringField("categoryName", article.getCategoryName(), Field.Store.YES));
                doc.add(new StringField("imagePath", article.getImagePath(), Field.Store.YES));

                if (update) {
                    //执行步骤如下：根据ID查询数据--》查询到替换或未查询到-添加doc数据
                    indexWriter.updateDocument(new Term("id", article.getId() + ""), doc);
                } else {
                    indexWriter.addDocument(doc);
                }
            }

            //6、输出到索引文件
            indexWriter.commit();

        } catch (IOException e) {
            e.printStackTrace();
            log.warn("创建lucene索引失败：" + articles);
        } finally {
            close(null,dir,indexWriter);
        }

    }

    /**
     * 词项查询条件
     */
    public static Query newTermQuery(String field, String text) throws IOException {

        //创建查询条件
        Term term = new Term(field, text);
        return new TermQuery(term);
    }

    /**
     * 多域查询条件
     */
    public static Query newMultiQuery(String[] fields, String queryStr) throws ParseException {

        //创建多域查询条件

        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, new IKAnalyzer6x());
        return queryParser.parse(queryStr);
        //底层实现逻辑
        //ik分词器对"中国美国"-->"中国""美国""国美"
        //和string[]中的域名做 排列组合 title:中国,title:美国,title:国美
        //content:中国,content:美国,content:国美 将每个结果按词项查询单独搜索计算得到一个document结果集

    }

    /**
     * 布尔查询条件
     */
    public static Query newBooleanQuery(BooleanClause... booleanClauses) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (BooleanClause booleanClause : booleanClauses) {
            builder.add(booleanClause);
        }
        return builder.build();
/*        //布尔查询子条件
        TermQuery termQuery = new TermQuery(new Term("title", "美国"));
        TermQuery termQuery1 = new TermQuery(new Term("content", "中国"));
        //封装子条件
        BooleanClause booleanClause = new BooleanClause(termQuery, BooleanClause.Occur.MUST);
        BooleanClause booleanClause1 = new BooleanClause(termQuery1, BooleanClause.Occur.MUST_NOT);
        return add(booleanClause).add(booleanClause1).build();
        //MUST 布尔查询结果必须是子条件的子集
        //MUST_NOT 布尔查询结果必须不是子条件的子集*/
    }

    /**
     * 范围查询条件
     */
    public static Query newRangeQuery(String field, int lowerValue, int upperValue) {
        //范围查询条件
        return IntPoint.newRangeQuery(field, lowerValue, upperValue);

    }

    /**
     *
     *
     * @param query
     */
    /**
     * 搜索文档
     *
     * @param searchStr 搜索字符串
     * @param query     查询条件
     * @param currPage  当前页面
     * @param pageNum   每页显示数量
     * @param savePath  索引目录
     * @param minScore  匹配最小分数
     * @return 搜索的文档对象
     */
    public static SearchArticle searchDocument(String searchStr, Query query, Integer currPage, Integer pageNum, String savePath, double minScore) {
        DirectoryReader reader = null;
        FSDirectory fsDirectory = null;
        try {
            //获取目录
            Path path = Paths.get(savePath);
            fsDirectory = FSDirectory.open(path);

            reader = DirectoryReader.open(fsDirectory);


            //创建查询对象
            IndexSearcher searcher = new IndexSearcher(reader);

            //指定查询条件并搜索
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            //查询到的总数量
            int totalHits = topDocs.totalHits;
            //计算当前需要返回的记录条数及页面
            int beginPos = (currPage - 1) * pageNum;
            //获取查询的文档数组
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            ArrayList<Article> articles = new ArrayList<>();
            for (int i = 0; i < scoreDocs.length; i++) {
                if (i < beginPos) {
                    continue;
                } else if (i >= beginPos + pageNum) {
                    break;
                }
                ScoreDoc scoreDoc = scoreDocs[i];
                if (scoreDoc.score < minScore) {
                    continue;
                }
                Article article = new Article();
                //获取文档对象
                int doc = scoreDoc.doc;
                Document doc1 = searcher.doc(doc);
                HashMap<String, String> term = getTerm(new IKAnalyzer6x(), searchStr, doc1.get("title"), doc1.get("description"));
                article.setTitle(term.get("title"));
                article.setId(Integer.parseInt(doc1.get("id")));
                article.setCategoryName(doc1.get("categoryName"));
                article.setCommNum(doc1.get("commNum") == null ? 0 : Long.parseLong(doc1.get("commNum")));
                article.setUpdateDate(DateUtil.stringToDate(doc1.get("updateDate")));
                article.setDescription(term.get("description"));
                article.setImagePath(doc1.get("imagePath"));
                article.setVisitors(StringUtil.isEmpty(doc1.get("visitors"))?0:Integer.parseInt(doc1.get("visitors")));


                articles.add(article);
                log.info("匹配分数：" + scoreDoc.score + ":" + doc1.get("title"));
            }
            SearchArticle searchArticle = new SearchArticle();
            searchArticle.setCurrPage(currPage);
            searchArticle.setTotalNum(totalHits);
            searchArticle.setArticles(articles);
            return searchArticle;
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("搜索文档失败：" + query);
        } finally {
            close(reader,fsDirectory,null);

        }
        return null;
    }

    /**
     * 关闭相关对象
     * @param reader
     * @param fsDirectory
     * @param indexWriter
     */
    private static void close(DirectoryReader reader,FSDirectory fsDirectory,IndexWriter indexWriter){

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException | AlreadyClosedException e) {
                //e.printStackTrace();
            }
        }
        if (fsDirectory != null) {
            try {
                fsDirectory.close();
            } catch (IOException | AlreadyClosedException e) {
                //e.printStackTrace();
            }
        }
        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException | AlreadyClosedException e) {
                //e.printStackTrace();
            }
        }
    }
}
