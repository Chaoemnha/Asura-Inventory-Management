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
    private Assistant guestAssistant;

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
        this.guestAssistant = createAssistantForRole(chatModel, UserRole.GUEST);
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
                .chatMemory(MessageWindowChatMemory.withMaxMessages(2))
                .build();
    }

    private String createSystemMessageForRole(UserRole role) {
        String baseMessage = "Bạn là AI trợ lý quản lý kho hàng. Trả lời bằng tiếng Việt có dấu. Thêm <br> mỗi khi câu trả lời của bạn có xuống dòng" +
                "Nếu không tìm thấy dữ liệu thì nói 'Tôi không tìm thấy thông tin này'. Nếu dữ liệu tìm được có thông tin ảnh, loại bỏ thông tin ảnh ra khỏi dữ liệu" +
                "Để điều hướng hoặc dẫn người dùng: trả về JSON {\"action\": \"navigate\", \"url\": \"/path\", \"message\": \"Đang chuyển trang...\"}. " +
                "Nếu không có URL phù hợp để dẫn hoặc điều hướng thì nói 'Không tìm thấy trang này trong hệ thống'.";

        switch (role) {
            case ADMIN:
                return baseMessage + " Quyền ADMIN: Truy cập tất cả bảng (users, products, categories, suppliers, transactions). " +
                        "URL được phép: /dashboard, /product, /transaction, /category, /supplier, /users, /purchase, /sell, /profile. " +
                        "Đến sản phẩm: /product/{id}, Giao dịch: /transaction/{id}.";
            case STOCKSTAFF:
                return baseMessage + " Quyền NHÂN VIÊN: Chỉ truy cập products, categories, transactions. " +
                        "KHÔNG được truy cập users, suppliers. " +
                        "URL được phép: /dashboard, /product, /transaction, /purchase, /sell, /profile."+
            "Đến sản phẩm: /product/{id}, Giao dịch: /transaction/{id}.";

            case SUPPLIER:
                return baseMessage + " Quyền NHÀ CUNG CẤP: Chỉ truy cập products, categories, transactions với transactions.transaction_type!='SALE'." +
                        "KHÔNG được truy cập users, suppliers. " +
                        "URL được phép: /dashboard, /product, /transaction, /profile."+
            "Đến sản phẩm: /product/{id}, Giao dịch: /transaction/{id}.";
            case CUSTOMER:
                return baseMessage + " Quyền KHÁCH HÀNG: Chỉ truy cập products, categories, transactions với transactions.transaction_type!='PURCHASE'." +
                        "KHÔNG được truy cập thông tin khác. " +
                        "URL được phép: /dashboard, /product, /transaction, /sell, /profile."+
            "Đến sản phẩm: /product/{id}, Giao dịch: /transaction/{id}.";
            case GUEST:
            default:
                return baseMessage + " Quyền KHÁCH: Chỉ xem products.id, products.name, products.description, products.price, categories" +
                        "KHÔNG được truy cập thông tin khác. " +
                        "URL được phép: /login, /product/, /register. Khuyến khích đăng ký, đăng nhập để sử dụng đầy đủ tính năng."+
            "Đến sản phẩm: /product/{id}.";
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
            log.warn("No authenticated user found, defaulting to GUEST");
            return UserRole.GUEST;
        }

        // Lay role tu authentication (gia su role duoc luu trong authorities)
        return authentication.getAuthorities().stream()
                .map(authority -> {
                    try {
                        return UserRole.valueOf(authority.getAuthority().replace("ROLE_", ""));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid role found: {}", authority.getAuthority());
                        return UserRole.GUEST;
                    }
                })
                .findFirst()
                .orElse(UserRole.GUEST);
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
                return customerAssistant;
            case GUEST:
            default:
                return guestAssistant;
        }
    }

    // Interface cho langchain4j
    interface Assistant {
        String answer(String query);
    }
}
