/**
 * RAG Assistant Chat Widget JavaScript
 * Dependencies: jQuery, Bootstrap 4
 */
var RagChatWidget = (function($) {
    'use strict';
    
    // Configuration
    var config = {
        apiBaseUrl: '/api/rag',
        selectors: {
            chatMessages: '#chatMessages',
            messageInput: '#messageInput', 
            sendBtn: '#sendBtn',
			typingIndicator: '#typingIndicator',
            chatToggle: '#check'
        }
    };
    
    // DOM Elements
    var elements = {};
        
    /**
     * Initialize the chat widget
     */
    function init() {
        // Cache DOM elements
        cacheElements();
        
        // Bind events
        bindEvents();
        
        // Focus input when chat opens
        focusInputOnOpen();
        
        console.log('RAG Chat Widget initialized');
    }
    
    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements.chatMessages = $(config.selectors.chatMessages);
        elements.messageInput = $(config.selectors.messageInput);
        elements.sendBtn = $(config.selectors.sendBtn);
		elements.typingIndicator = $(config.selectors.typingIndicator);
        elements.chatToggle = $(config.selectors.chatToggle);
    }
    
    /**
     * Bind event handlers
     */
    function bindEvents() {
        // Send button click
        elements.sendBtn.on('click', handleSendMessage);
        
        // Enter key press in input
        elements.messageInput.on('keypress', function(e) {
            if (e.which === 13 && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
            }
        });
    }
    
    /**
     * Handle send message
     */
    function handleSendMessage() {
        var message = elements.messageInput.val().trim();
        if (!message) return;
        
        // Disable input
        setInputState(false);
        
        // Add user message to chat
        addMessage(message, 'user');
        
        // Clear input
        elements.messageInput.val('');
        
        // Show typing indicator
		showTyping();
        
        // Send to backend
        sendMessageToServer(message);
    }
    
    /**
     * Send message to server
     */
    function sendMessageToServer(message) {
    // Lấy token từ sessionStorage và decrypt
    var encryptedToken = sessionStorage.getItem('token');
    var token = '';
    if (encryptedToken) {
        try {
            token = CryptoJS.AES.decrypt(encryptedToken, 'luannguyen').toString(CryptoJS.enc.Utf8);
        } catch (e) {
            token = '';
        }
    }

    $.ajax({
        url: 'http://localhost:8080/api/rag' + '/chat',
        method: 'POST',
        contentType: 'application/json',
        headers: {
            'Authorization': 'Bearer ' + token
        },
        data: JSON.stringify({
            question: message,
        }),
        success: function(response) {
            handleServerResponse(response);
        },
        error: function(xhr, status, error) {
            handleServerError(xhr, status, error);
        },
        complete: function() {
            // Re-enable input
            setInputState(true);
            elements.messageInput.focus();
        }
    });
}
    
    /**
     * Handle successful server response
     */
    function handleServerResponse(response) {
		hideTyping();
        
        if (response.success && response.assistantMessage) {
            addMessage(response.assistantMessage.message, 'assistant');
        } else {
            var errorMsg = response.error || 'Xin lỗi, tôi không thể trả lời câu hỏi này. Vui lòng thử lại.';
            addMessage(errorMsg, 'assistant');
        }
    }
    
    /**
     * Handle server error
     */
    function handleServerError(xhr, status, error) {
		hideTyping();
        console.error('Chat API Error:', error);
        
        var errorMessage = 'Có lỗi xảy ra khi kết nối với server. Vui lòng thử lại sau.';
        
        // Handle specific error codes
        if (xhr.status === 400) {
            errorMessage = 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại.';
        } else if (xhr.status === 500) {
            errorMessage = 'Lỗi server nội bộ. Vui lòng thử lại sau.';
        } else if (xhr.status === 0) {
            errorMessage = 'Không thể kết nối với server. Vui lòng kiểm tra kết nối mạng.';
        }
        
        addMessage(errorMessage, 'assistant');
    }
    
    /**
     * Add message to chat
     */
    function addMessage(text, type) {
        var time = new Date().toLocaleTimeString('vi-VN', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
        
        try {
        var parsed = JSON.parse(text);
        text = text.replace(/\n/g, "<br>");
        if (parsed.message) {
            text = parsed.message;
        }
        if (parsed && parsed.action === 'navigate') {
          // Use parsed message for navigation
          const navigateEvent = new CustomEvent('ai-navigate', {
                detail: { url: parsed.url }
            });
            window.dispatchEvent(navigateEvent);
        }
    } catch (e) {
        // Không làm gì nếu không phải JSON
    }

    var messageHtml = buildMessageHTML(text, type, time);
    elements.chatMessages.append(messageHtml);
    scrollToBottom();
    }
    
    /**
     * Build message HTML
     */
    function buildMessageHTML(text, type, time) {
        return [
            '<div class="message ' + type + '">',
                '<div class="message-bubble">',
                    text,
                    '<div class="message-time">' + time + '</div>',
                '</div>',
            '</div>'
        ].join('');
    }
    
    /**
     * Show typing indicator
     */    
	function showTyping() {
	    elements.typingIndicator.show();
	    scrollToBottom();
	}

	/**
	 * Hide typing indicator
	 */
	function hideTyping() {
	    elements.typingIndicator.hide();
	}
    /**
     * Set input state (enabled/disabled)
     */
    function setInputState(enabled) {
        elements.messageInput.prop('disabled', !enabled);
        elements.sendBtn.prop('disabled', !enabled);
    }
    
    /**
     * Scroll to bottom of chat
     */
    function scrollToBottom() {
        if (elements.chatMessages.length > 0) {
            elements.chatMessages.scrollTop(elements.chatMessages[0].scrollHeight);
        }
    }
    
    /**
     * Focus input when chat opens
     */
    function focusInputOnOpen() {
        if (elements.messageInput.length > 0) {
            elements.messageInput.focus();
        }
    }
    
    /**
     * Escape HTML characters
     */
    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text || '';
        return div.innerHTML;
    }
    
    /**
     * Generate unique session ID
     */
    
    /**
     * Set API base URL
     */
    function setApiUrl(url) {
        config.apiBaseUrl = url;
    }
    
    /**
     * Reset chat (clear messages)
     */
    function resetChat() {
        if (elements.chatMessages) {
            elements.chatMessages.empty();
            // Add welcome message back
            var welcomeHtml = [
                '<div class="welcome-message">',
                    '<i class="fa fa-robot" style="font-size: 24px; margin-bottom: 10px;"></i><br>',
                    'Xin chào! Tôi là AI Assistant. Hãy hỏi tôi bất cứ điều gì bạn muốn biết.',
                '</div>'
            ].join('');
            elements.chatMessages.append(welcomeHtml);
        }
        
    }
    
    // Public API
    return {
        init: init,
        setApiUrl: setApiUrl,
        resetChat: resetChat,
        addMessage: addMessage
    };
    
})(jQuery);

// Auto-initialize when DOM is ready
$(document).ready(function() {
    // Check if chat widget exists on page
    if ($('#chatMessages').length > 0) {
        RagChatWidget.init();
    }
});

// Make it globally available
window.RagChatWidget = RagChatWidget;