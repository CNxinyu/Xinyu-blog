package com.xinyu.common.mybatis.typehandler;

import com.xinyu.auth.mapper.RefreshTokenMapper;
import com.xinyu.article.mapper.ArticleMapper;
import com.xinyu.article.mapper.ArticleTagMapper;
import com.xinyu.article.mapper.CategoryMapper;
import com.xinyu.article.mapper.TagMapper;
import com.xinyu.comment.mapper.CommentMapper;
import com.xinyu.user.mapper.UserMapper;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgreSqlUuidTypeHandlerTest {

    private final PostgreSqlUuidTypeHandler handler = new PostgreSqlUuidTypeHandler();

    @Test
    void writesUuidAsPostgreSqlOther() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        UUID value = UUID.randomUUID();

        handler.setNonNullParameter(statement, 1, value, JdbcType.OTHER);

        verify(statement).setObject(1, value, Types.OTHER);
    }

    @Test
    void readsUuidWithoutStringConversion() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID value = UUID.randomUUID();
        when(resultSet.getObject("family_id", UUID.class)).thenReturn(value);

        assertThat(handler.getNullableResult(resultSet, "family_id")).isEqualTo(value);
    }

    @Test
    void refreshTokenMapperMetadataCanBeParsed() {
        Configuration configuration = new Configuration();
        configuration.getTypeHandlerRegistry().register(PostgreSqlUuidTypeHandler.class);

        assertThatCode(() -> configuration.addMapper(RefreshTokenMapper.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> configuration.addMapper(UserMapper.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> configuration.addMapper(ArticleMapper.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> configuration.addMapper(ArticleTagMapper.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> configuration.addMapper(CategoryMapper.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> configuration.addMapper(TagMapper.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> configuration.addMapper(CommentMapper.class))
                .doesNotThrowAnyException();
    }
}
