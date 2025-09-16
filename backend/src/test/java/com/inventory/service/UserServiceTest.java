package com.inventory.service;

import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.Response;
import com.inventory.dto.UserDTO;
import com.inventory.entity.User;
import com.inventory.enums.UserRole;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtUtils;
import com.inventory.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Qualifier("modelMapper")
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils  jwtUtils;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;
    private UserDTO userDTO;
    private User user;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        modelMapper = new ModelMapper();
        userDTO = UserDTO.builder().id(1L).name("Luuanz").email("luuanz@yahoo.com").password("039584728").phoneNumber("0498582").role(UserRole.MANAGER).build();
        user = modelMapper.map(userDTO, User.class);
        // Tiem modelMapper vao userService (co phuong thuc ser dung mapper nen p set ko thi null excep)
        Field field = UserServiceImpl.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(userService, modelMapper);
    }

    @DisplayName("Testcase: kiem thu phuong thuc registerUser")
    @Test
    public void testRegisterUser() {
        RegisterRequest registerRequest = new RegisterRequest("luuanz", "luuanz@yahoo.com", "123", "0498582", UserRole.MANAGER);
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(userRepository.findByName(registerRequest.getName())).willReturn(Optional.empty());
        BDDMockito.given(userRepository.findByPhoneNumber(registerRequest.getPhoneNumber())).willReturn(Optional.empty());
        BDDMockito.given(userRepository.findByEmail(registerRequest.getEmail())).willReturn(Optional.empty());
        BDDMockito.given(passwordEncoder.encode(registerRequest.getPassword())).willReturn(registerRequest.getPassword());
        //Sau là test tổng quan - return
        Response response = userService.registerUser(registerRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc loginUser")
    @Test
    public void testLoginUser() {
        LoginRequest loginRequest = new LoginRequest("luuanz@yahoo.com", "123");
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(user));
        BDDMockito.given(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).willReturn(true);
        BDDMockito.given(jwtUtils.generateToken(user.getEmail())).willReturn("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsdXVhbnpAeWFob28uY29tIiwiaWF0IjoxNzU0ODk4NTExLCJleHAiOjE3NTY0NTM3MTF9.i7gH1Nt-Cg3j6KyjUhAfz76RtCReBadw-y5bEnsM_UM");
        //Sau là test tổng quan - return
        Response response = userService.loginUser(loginRequest);
        assertThat(response.getToken().isEmpty()).isFalse();
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAll")
    @Test
    public void testGetAllUsers() {
        BDDMockito.given(userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(user));
        //Test return kem userDTOS
        Response response = userService.getAllUsers();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getUsers().getFirst().getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getCurrentLoggedInUser")
    @Test
    public void getCurrentLoggedInUser() {
        BDDMockito.given(authentication.getName()).willReturn("luuanz@yahoo.com");
        BDDMockito.given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        BDDMockito.given(userRepository.findByEmail("luuanz@yahoo.com")).willReturn(Optional.of(user));
        UserDTO user = userService.getCurrentLoggedInUser();
        assertThat(user.getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc update")
    @Test
    public void testUpdateUser() {
        userDTO.setName("Luan");
        BDDMockito.given(userRepository.findById(1L)).willReturn(Optional.of(user));
        BDDMockito.given(userRepository.save(user)).willReturn(user);
        Response response = userService.updateUser(1L, userDTO);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc delete")
    @Test
    public void testDeleteUser() {
        BDDMockito.given(userRepository.findById(1L)).willReturn(Optional.of(user));
        Response response = userService.deleteUser(1L);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}