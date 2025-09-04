package com.inventory.service;

import com.inventory.dto.UserDTO;
import com.inventory.entity.User;
import com.inventory.enums.UserRole;
import com.inventory.repository.UserRepository;
import com.inventory.service.impl.PasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PasswordServiceTest {
    @InjectMocks
    private PasswordServiceImpl passwordService;

    @Mock
    private UserRepository userRepository;

    private User user;

    @Qualifier("modelMapper")
    private ModelMapper modelMapper;
    @BeforeEach
    public void setUp() {
        modelMapper = new ModelMapper();
        user = modelMapper.map(UserDTO.builder().id(1L).name("Luuanz").email("luuanz@yahoo.com").password("039584728").phoneNumber("0498582").role(UserRole.MANAGER).build(), User.class);
        user.setRsTokenCrDate(new Date(2025-1900, 8,4,21,01));
    }

    @DisplayName("Testcase: kiem thu phuong thuc validateResetPasswordToken (Xac thuc rspw token)")
    @Test
    public void TestValidateResetPasswordToken(){
        BDDMockito.given(userRepository.findByResetPasswordToken("mockToken")).willReturn(user);
        Boolean result = passwordService.validateResetPasswordToken("mockToken");
        assertThat(result).isTrue();
    }

}
