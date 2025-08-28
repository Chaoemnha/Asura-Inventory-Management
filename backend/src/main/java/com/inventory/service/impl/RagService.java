package com.inventory.service.impl;

import com.inventory.service.ProductService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.randomUUID;

@Service
@Slf4j
public class RagService {
    private final RagDataExtractionService ragDataExtractionService;
    private final EmbeddingService  embeddingService;
    private final PineconeEmbeddingStore embeddingStore;
    private final Assistant assistant;
    Dotenv dotenv = Dotenv.configure().load();

    public RagService(RagDataExtractionService  ragDataExtractionService, EmbeddingService embeddingService) {
        this.ragDataExtractionService = ragDataExtractionService;
        this.embeddingService = embeddingService;

        //Su dung InMemEmbed de thu nghiem
        this.embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(dotenv.get("PINECONE_API_KEY"))
                .index("test")
                .nameSpace(randomUUID())
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")
                        .region("us-east-1")
                        .dimension(embeddingService.getEmbeddingModel().dimension())
                        .build())
                .build();

        //Tich hop llaMa qua Ollama
        OllamaChatModel chatModel = OllamaChatModel.builder().baseUrl("http://localhost:11434").modelName("llama3.1:8b").temperature(0.0).build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingService.getEmbeddingModel())
                        .maxResults(5)
                        .minScore(0.7)
                        .build())
                .contentInjector(DefaultContentInjector.builder()
                        .metadataKeysToInclude(Arrays.asList("source", "title"))
                        .build())
                .build();

        //Build AiServices voi retrievalAugmentor
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }
    //Khoi tao du lieu RAG (chay khi ung dung khoi dong)
    @PostConstruct
    public void initializeRAG() {
        List<String> texts = ragDataExtractionService.extractAllDataForEmbedding();
        List<Embedding> embeddings = embeddingService.embedTexts(texts);
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
        embeddingStore.addAll(embeddings, segments);
    }
    //Truy van RAG
    public String query(String question) {
        return assistant.answer(question);
    }
    //Interface cho langchain4j
    interface Assistant{
        String answer(String query);
    }
}
