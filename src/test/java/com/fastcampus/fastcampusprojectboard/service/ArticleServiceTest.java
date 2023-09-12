package com.fastcampus.fastcampusprojectboard.service;

import com.fastcampus.fastcampusprojectboard.domain.Article;
import com.fastcampus.fastcampusprojectboard.domain.UserAccount;
import com.fastcampus.fastcampusprojectboard.domain.constant.SearchType;
import com.fastcampus.fastcampusprojectboard.dto.ArticleDto;
import com.fastcampus.fastcampusprojectboard.dto.ArticleWithCommentsDto;
import com.fastcampus.fastcampusprojectboard.dto.UserAccountDto;
import com.fastcampus.fastcampusprojectboard.repository.ArticleRepository;
import com.fastcampus.fastcampusprojectboard.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.given;

@DisplayName("비즈니스 로직 - 게시글")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @InjectMocks private ArticleService sut;
    @Mock private ArticleRepository articleRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @DisplayName("검색어 없이 게시글 검색하면, 게시글 페이지 반환")
    @Test
    void givenNoSearchParam_whenSearchingArticles_thenReturnsArticlePage() {

        Pageable pageable = Pageable.ofSize(20);
        given(articleRepository.findAll(pageable)).willReturn(Page.empty());

        Page<ArticleDto> articles = sut.searchArticles(null, null, pageable);

        assertThat(articles).isEmpty();
        then(articleRepository).should().findAll(pageable);
    }

    @DisplayName("검색어와 함께 게시글 검색하면, 게시글 페이지 반환")
    @Test
    void givenSearchParam_whenSearchingArticles_thenReturnsArticlePage() {

        SearchType searchType = SearchType.TITLE;
        String searchKeyword = "what is title";
        Pageable pageable = Pageable.ofSize(20);
        given(articleRepository.findByTitleContaining(searchKeyword, pageable)).willReturn(Page.empty());

        Page<ArticleDto> articles = sut.searchArticles(searchType, searchKeyword, pageable);

        assertThat(articles).isEmpty();
        then(articleRepository).should().findByTitleContaining(searchKeyword, pageable);
    }

    @DisplayName("검색어 없이 게시글을 해시태그 검색하면, 빈 페이지 반환")
    @Test
    void givenNoSearchParam_whenSearchingArticlesViaHashtag_thenReturnsEmptyPage() {

        Pageable pageable = Pageable.ofSize(20);

        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(null, pageable);

        assertThat(articles).isEqualTo(Page.empty(pageable));
        then(articleRepository).shouldHaveNoInteractions();
    }

    @DisplayName("게시글을 해시태그 검색하면, 게시글 페이지 반환")
    @Test
    void givenHashtag_whenSearchingArticlesViaHashtag_thenReturnsArticlesPage() {

        String hashtag = "#java";
        Pageable pageable = Pageable.ofSize(20);
        given(articleRepository.findByHashtag(hashtag, pageable)).willReturn(Page.empty(pageable));

        Page<ArticleDto> articles = sut.searchArticlesViaHashtag(hashtag, pageable);

        assertThat(articles).isEqualTo(Page.empty(pageable));
        then(articleRepository).should().findByHashtag(hashtag, pageable);
    }

    @DisplayName("게시글 ID로 조회하면, 게시글+댓글 반환")
    @Test
    void givenArticleId_whenSearchingArticleWithComments_thenReturnsArticleWithComments() {

        Long articleId = 1L;
        Article article = createArticle();
        given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

        ArticleWithCommentsDto dto = sut.getArticleWithComments(articleId);

        assertThat(dto).hasFieldOrPropertyWithValue("title", article.getTitle());
        assertThat(dto).hasFieldOrPropertyWithValue("content", article.getContent());
        assertThat(dto).hasFieldOrPropertyWithValue("hashtag", article.getHashtags());
        then(articleRepository).should().findById(articleId);
    }

    @DisplayName("댓글+게시글 없으면, 예외 처리")
    @Test
    void givenNonexistentArticleId_whenSearchingArticleWithComments_thenThrowsException() {
        Long articleId = 0L;
        given(articleRepository.findById(articleId)).willReturn(Optional.empty());

        Throwable t = catchThrowable(() -> sut.getArticleWithComments(articleId));

        assertThat(t).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("게시글이 없습니다 - articleId:" + articleId);
        then(articleRepository).should().findById(articleId);
    }

    @DisplayName("게시글 조회하면, 게시글 반환")
    @Test
    void givenArticleId_whenSearchingArticle_thenReturnsArticle() {
        Long articleId = 1L;
        Article article = createArticle();
        given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

        ArticleDto dto = sut.getArticle(articleId);

        assertThat(dto)
                .hasFieldOrPropertyWithValue("title", article.getTitle())
                .hasFieldOrPropertyWithValue("content", article.getContent())
                .hasFieldOrPropertyWithValue("hashtag", article.getHashtags());
        then(articleRepository).should().findById(articleId);
    }

    @DisplayName("게시글 정보를 입력하면, 게시글을 생성")
    @Test
    void givenArticleInfo_whenSavingArticle_thenSavesArticle() {
        ArticleDto dto = createArticleDto();
        given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(createUserAccount());
        given(articleRepository.save(any(Article.class))).willReturn(createArticle());

        sut.saveArticle(dto);

        then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());
        then(articleRepository).should().save(any(Article.class)); //save 메소드가 호출되었는가를 확인하는 것
    }

    @DisplayName("게시글의 수정 정보를 입력하면, 게시글을 수정")
    @Test
    void givenModifiedArticleInfo_whenUpdatingArticle_thenUpdatesArticle() {
        Article article = createArticle();
        ArticleDto dto = createArticleDto("new title", "new content", "#springboot");
        given(articleRepository.getReferenceById(dto.id())).willReturn(article); //레퍼런스를 가져오는 것
        given(userAccountRepository.getReferenceById(dto.userAccountDto().userId())).willReturn(dto.userAccountDto().toEntity());

        sut.updateArticle(dto.id(), dto);

        assertThat(article)
                .hasFieldOrPropertyWithValue("title", dto.title())
                .hasFieldOrPropertyWithValue("content", dto.content())
                .hasFieldOrPropertyWithValue("hashtag", dto.hashtagDtos());
        then(articleRepository).should().getReferenceById(dto.id());
        then(userAccountRepository).should().getReferenceById(dto.userAccountDto().userId());

    }

    @DisplayName("없는 게시글의 수정 정보를 입력하면, 경고 로그를 찍고 아무것도 하지 않는다.")
    @Test
    void givenNonexistentArticleInfo_whenUpdatingArticle_thenLogsWarningAndDoesNothing() {
        ArticleDto dto = createArticleDto("new title", "new content", "#springboot");
        given(articleRepository.getReferenceById(dto.id())).willThrow(EntityNotFoundException.class);

        sut.updateArticle(dto.id(), dto);

        then(articleRepository).should().getReferenceById(dto.id());
    }

    @DisplayName("게시글의 ID를 입력하면, 게시글을 삭제")
    @Test
    void givenArticleId_whenDeletingArticle_thenDeletesArticle() {
        Long articleId = 1L;
        String userId = "haco";
        willDoNothing().given(articleRepository).deleteByIdAndUserAccount_UserId(articleId, userId);

        sut.deleteArticle(1L, userId);

        then(articleRepository).should().deleteById(articleId); //delete 메소드가 호출되었는가를 확인하는 것
    }

    @DisplayName("해시태그를 조회하면, 유니크 해시태그 리스트 반환")
    @Test
    void givenNothing_whenCalling_thenReturnsHashtags() {

        List<String> expectedHashtags = List.of("#java", "#spring", "#boot");
        given(articleRepository.findAllDistinctHashtags()).willReturn(expectedHashtags);

        List<String> actualHashtags = sut.getHashtags();

        assertThat(actualHashtags).isEqualTo(expectedHashtags);
        then(articleRepository).should().findAllDistinctHashtags();
    }

    private UserAccount createUserAccount() {
        return UserAccount.of(
                "uno",
                "password",
                "uno@email.com",
                "Uno",
                null
        );
    }

    private Article createArticle() {
        Article article = Article.of(
                createUserAccount(),
                "title",
                "content"
        );

        ReflectionTestUtils.setField(article, "id", 1L);
        return article;
    }

    private ArticleDto createArticleDto() {
        return createArticleDto("title", "content", "#java");
    }

    private ArticleDto createArticleDto(String title, String content, String hashtag) {
        return ArticleDto.of(1L,
                createUserAccountDto(),
                title,
                content,
                null,
                LocalDateTime.now(),
                "Uno",
                LocalDateTime.now(),
                "Uno");
    }

    private UserAccountDto createUserAccountDto() {
        return UserAccountDto.of(
                "uno",
                "password",
                "uno@mail.com",
                "Uno",
                "This is memo",
                LocalDateTime.now(),
                "uno",
                LocalDateTime.now(),
                "uno"
        );
    }


}