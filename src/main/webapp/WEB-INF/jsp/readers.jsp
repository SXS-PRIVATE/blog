<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="renderer" content="webkit">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>舒新胜博客读者墙</title>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.min.css">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/nprogress.css">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/font-awesome.min.css">
<link rel="apple-touch-icon-precomposed" href="${pageContext.request.contextPath}/images/icon/icon.png">
<link rel="shortcut icon" href="${pageContext.request.contextPath}/images/icon/head.ico">
<script src="${pageContext.request.contextPath}/js/jquery/jquery-2.1.4.min.js"></script>
<script src="${pageContext.request.contextPath}/js/nprogress.js"></script>
<script src="${pageContext.request.contextPath}/js/jquery/jquery.lazyload.min.js"></script>
<!--[if gte IE 9]>
  <script src="${pageContext.request.contextPath}/js/jquery/jquery-1.11.1.min.js" type="text/javascript"></script>
  <script src="${pageContext.request.contextPath}/js/html5shiv.min.js" type="text/javascript"></script>
  <script src="${pageContext.request.contextPath}/js/respond.min.js" type="text/javascript"></script>
  <script src="${pageContext.request.contextPath}/js/selectivizr-min.js" type="text/javascript"></script>
<![endif]-->
<!--[if lt IE 9]>
  <script>window.location.href='upgrade-browser.jsp';</script>
<![endif]-->
</head>

<body class="user-select">
<%@ include file="_header.jsp"%>
<section class="container container-page">
  <div class="pageside">
    <div class="pagemenus">
      <ul class="pagemenu">
        <li><a href="${pageContext.request.contextPath}/header/tags.action">标签云</a></li>
        <li><a href="${pageContext.request.contextPath}/header/readers.action">读者墙</a></li>
        <li><a href="${pageContext.request.contextPath}/header/links.action">友情链接</a></li>
      </ul>
    </div>
  </div>
  <div class="content">
    <header class="article-header">
      <h1 class="article-title">读者墙</h1>
    </header>
    <div class="readers"> <a class="item-readers item-readers-1" target="_top" href="" rel="nofollow">
      <h4>【金牌读者】<small>评论：123</small></h4>
      <img class="avatar" height="36" width="36" src="${pageContext.request.contextPath}/images/icon/icon.png" alt=""><strong>舒新胜</strong>http://www.shuxinsheng.com/</a> <a class="item-readers item-readers-2" target="_top" href="" rel="nofollow">
      <h4>【银牌读者】<small>评论：12</small></h4>
      <img class="avatar" height="36" width="36" src="${pageContext.request.contextPath}/images/icon/icon.png" alt=""><strong>舒新胜</strong>http://www.shuxinsheng.com/</a> <a class="item-readers item-readers-3" target="_top" href="" rel="nofollow">
      <h4>【铜牌读者】<small>评论：8</small></h4>
      <img class="avatar" height="36" width="36" src="${pageContext.request.contextPath}/images/icon/icon.png" alt=""><strong>舒新胜</strong>http://www.shuxinsheng.com/</a> <a class="item-readers item-readers-4" target="_top" href="" rel="nofollow">
      <h4>【普通读者】<small>评论：1</small></h4>
      <img class="avatar" height="36" width="36" src="${pageContext.request.contextPath}/images/icon/icon.png" alt=""><strong>舒新胜</strong>http://www.shuxinsheng.com/</a> </div>
  </div>
</section>
<%@ include file="_footer.jsp"%>

<script src="${pageContext.request.contextPath}/js/bootstrap.min.js"></script>
<script src="${pageContext.request.contextPath}/js/jquery/jquery.ias.js"></script>
<script src="${pageContext.request.contextPath}/js/scripts.js"></script>
</body>
</html>