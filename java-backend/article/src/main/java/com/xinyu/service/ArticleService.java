package com.xinyu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xinyu.entity.Article;

import java.util.List;


public interface ArticleService extends IService<Article> {

    List<Article> findAllArticle();
}
