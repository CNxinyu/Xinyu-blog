package com.xinyu.controller;

import com.xinyu.entity.Article;
import com.xinyu.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/article")
public class ArticleController {

    private final ArticleService articleService;

    @Autowired
    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @RequestMapping("/list")
    public Object getList(){
        List<Article> list = articleService.findAllArticle();
        return list;
    }

}
