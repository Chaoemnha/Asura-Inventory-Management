package com.inventory.service.impl;

import com.inventory.enums.UserRole;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@Slf4j
public class RagService {
    private final DataSource dataSource;
    private Assistant adminAssistant;
    private Assistant staffAssistant;
    private Assistant customerAssistant;
    private Assistant supplierAssistant;

    Dotenv dotenv = Dotenv.configure().load();

    @Autowired
    public RagService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeRAG() {
        log.info("Initializing RagService with role-based SQL Database Content Retrievers");

        // Tich hop OpenAI GPT model
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // Tao cac ContentRetriever khac nhau cho tung role
        this.adminAssistant = createAssistantForRole(chatModel, UserRole.ADMIN);
        this.staffAssistant = createAssistantForRole(chatModel, UserRole.STOCKSTAFF);
        this.customerAssistant = createAssistantForRole(chatModel, UserRole.CUSTOMER);
        this.supplierAssistant = createAssistantForRole(chatModel, UserRole.SUPPLIER);
    }

    private Assistant createAssistantForRole(ChatModel chatModel, UserRole role) {
        // Tao system message khac nhau cho tung role
        String systemMessage = createSystemMessageForRole(role);

        ContentRetriever contentRetriever = SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatModel(chatModel)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .systemMessageProvider(chatMemoryId -> systemMessage)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(3))
                .build();
    }

    private String createSystemMessageForRole(UserRole role) {
        String baseMessage = "Ban la mot AI assistant cho he thong quan ly kho hang. ";

        switch (role) {
            case ADMIN:
                return baseMessage + "Ban co quyen truy cap tat ca du lieu trong database bao gom: users, products, categories, suppliers, transactions. " +
                        "Ban co the thuc hien tat ca cac truy van SELECT tren cac bang nay.";

            case STOCKSTAFF:
                return baseMessage + "Ban chi co quyen truy cap du lieu: products, categories, transactions lien quan den nhap/xuat kho/tra hang ve supplier. " +
                        "Ban KHONG duoc truy cap thong tin users hoac suppliers. " +
                        "Chi duoc truy van SELECT tren cac bang: products, categories, transactions";

            case SUPPLIER:
                return baseMessage + "Ban chi co quyen truy cap du lieu: products, categories, transactions lien quan den xuat kho/tra hang ve supplier. " +
                        "Ban KHONG duoc truy cap thong tin users hoac suppliers. " +
                        "Chi duoc truy van SELECT tren cac bang: products, categories, transaction";

            case CUSTOMER:
                return baseMessage + "Ban chi co quyen xem thong tin co ban ve san pham va danh muc. " +
                        "Ban chi duoc SELECT cac truong: products.id, products.name, products.description, products.price, categories.name tu cac bang products va categories. " +
                        "Ban KHONG duoc truy cap bat ky thong tin nao khac.";

            default:
                return baseMessage + "Ban chi co quyen xem thong tin co ban ve san pham va danh muc. " +
                        "Ban chi duoc SELECT cac truong: products.id, products.name, products.description, products.price, categories.name tu cac bang products va categories. " +
                        "Ban KHONG duoc truy cap bat ky thong tin nao khac.";
        }
    }

    // Truy van RAG voi phan quyen
    public String query(String question) {
        UserRole currentUserRole = getCurrentUserRole();
        log.info("User with role {} is making query: {}", currentUserRole, question);

        Assistant assistant = getAssistantForRole(currentUserRole);
        return assistant.answer(question);
    }

    private UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found, defaulting to CUSTOMER role");
            return UserRole.CUSTOMER;
        }

        // Lay role tu authentication (gia su role duoc luu trong authorities)
        return authentication.getAuthorities().stream()
                .map(authority -> {
                    try {
                        return UserRole.valueOf(authority.getAuthority().replace("ROLE_", ""));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid role found: {}", authority.getAuthority());
                        return UserRole.CUSTOMER;
                    }
                })
                .findFirst()
                .orElse(UserRole.CUSTOMER);
    }

    private Assistant getAssistantForRole(UserRole role) {
        switch (role) {
            case ADMIN:
                return adminAssistant;
            case SUPPLIER:
                return supplierAssistant;
            case STOCKSTAFF:
                return staffAssistant;
            case CUSTOMER:
            default:
                return customerAssistant;
        }
    }

    // Interface cho langchain4j
    interface Assistant {
        String answer(String query);
    }
}
