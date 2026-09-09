package com.xinyu.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinyu.article.entity.CategoryEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

public interface CategoryMapper extends BaseMapper<CategoryEntity> {
    @Select("SELECT id, name, slug, description, created_at, updated_at FROM categories "
            + "WHERE LOWER(slug) = LOWER(#{slug}) LIMIT 1")
    CategoryEntity selectBySlug(@Param("slug") String slug);

    @Select("SELECT id, name, slug, description, created_at, updated_at FROM categories "
            + "WHERE LOWER(name) = LOWER(#{name}) LIMIT 1")
    CategoryEntity selectByName(@Param("name") String name);

    @Select("SELECT id, name, slug, description, created_at, updated_at "
            + "FROM categories ORDER BY name ASC, id ASC")
    List<CategoryEntity> selectAllOrderByName();

    @Update("""
            UPDATE categories
            SET name = #{name}, slug = #{slug}, description = #{description,jdbcType=VARCHAR},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateCategory(@Param("id") Long id,
                       @Param("name") String name,
                       @Param("slug") String slug,
                       @Param("description") String description,
                       @Param("updatedAt") OffsetDateTime updatedAt);
}
