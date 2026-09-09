package com.xinyu.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinyu.article.entity.TagEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;

public interface TagMapper extends BaseMapper<TagEntity> {
    @Select("SELECT id, name, slug, created_at, updated_at FROM tags "
            + "WHERE LOWER(slug) = LOWER(#{slug}) LIMIT 1")
    TagEntity selectBySlug(@Param("slug") String slug);

    @Select("SELECT id, name, slug, created_at, updated_at FROM tags "
            + "WHERE LOWER(name) = LOWER(#{name}) LIMIT 1")
    TagEntity selectByName(@Param("name") String name);

    @Select("SELECT id, name, slug, created_at, updated_at FROM tags ORDER BY name ASC, id ASC")
    List<TagEntity> selectAllOrderByName();

    @Update("""
            UPDATE tags
            SET name = #{name}, slug = #{slug}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateTag(@Param("id") Long id,
                  @Param("name") String name,
                  @Param("slug") String slug,
                  @Param("updatedAt") OffsetDateTime updatedAt);
}
