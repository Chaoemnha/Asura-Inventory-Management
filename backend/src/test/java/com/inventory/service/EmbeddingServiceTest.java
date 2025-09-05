package com.inventory.service;

import com.inventory.service.impl.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {
    @InjectMocks
    private EmbeddingService embeddingService;
    @Mock
    private EmbeddingModel embeddingModel;
    @BeforeEach
    void setUp() {

    }
//    Khong kiem thu duoc vi main connect toi Ollama Server that, dieu do khong co san khi test :(
//    @DisplayName("Testcase: kiem thu phuong thuc embedTexts(chuyen text thanh vector embed)")
//    @Test
//    void embedTextsTest(){
//        BDDMockito.given(embeddingModel.embedAll(List.of(TextSegment.from("abc")))).willReturn(Response.from(List.of(Embedding.from(new float[2]))));
//        List<Embedding> result = embeddingService.embedTexts(List.of("abc"));
//        assertThat(result.size()).isEqualTo(1);
//    }
}
