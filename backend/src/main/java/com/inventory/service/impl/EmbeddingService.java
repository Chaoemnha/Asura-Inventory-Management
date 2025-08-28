package com.inventory.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;
@Service
public class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    public static final String OLLAMA_BASE_URL = System.getenv("OLLAMA_BASE_URL");
    public static final String ALL_MINILM_MODEL = "all-minilm:latest";
    public static String ollamaBaseUrl(){
        if(isNullOrEmpty(OLLAMA_BASE_URL)){
            return "http://localhost:11434";
        }
        else{
            return OLLAMA_BASE_URL;
        }
    }

    public EmbeddingService(){
        this.embeddingModel = OllamaEmbeddingModel.builder().baseUrl(ollamaBaseUrl()).modelName(ALL_MINILM_MODEL).build();
        TestConnection();
    }

    private void TestConnection(){
        try {
            System.out.println("Testing embedding model connection...");
            var embedding = embeddingModel.embed("Hello world").content();
            System.out.println("Embedding model connection ok"+embedding.dimension());
        }
        catch (Exception e){
            throw new RuntimeException("Failed to connect to embedding model: "+e.getMessage(), e);
        }
    }

    public List<Embedding> embedTexts(List<String> texts){
        List<TextSegment> textSegments = texts.stream().map(TextSegment::from).collect(Collectors.toList());

        Response<List<Embedding>> response = embeddingModel.embedAll(textSegments);
        return response.content();
    }

    public EmbeddingModel getEmbeddingModel(){
        return embeddingModel;
    }
}
