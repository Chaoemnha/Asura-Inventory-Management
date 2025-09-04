package com.inventory.service.impl;

import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.Response;
import com.inventory.dto.UserDTO;
import com.inventory.entity.User;
import com.inventory.entity.User;
import com.inventory.enums.UserRole;
import com.inventory.exceptions.InvalidCredentialsException;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtUtils;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    @Qualifier("modelMapper")
    private final ModelMapper modelMapper;
    private final JwtUtils jwtUtils;
    @Autowired
    @Qualifier("userMapper")
    private ModelMapper userMapper;


    @Override
    public Response registerUser(RegisterRequest registerRequest) {
        Optional<User> user1 = userRepository.findByName(registerRequest.getName());
        Optional<User> user2 = userRepository.findByPhoneNumber(registerRequest.getPhoneNumber());
        Optional<User> user3 = userRepository.findByEmail(registerRequest.getEmail());
        if (user1.isPresent()||user2.isPresent()||user3.isPresent()) {
            throw new InvalidCredentialsException("This user already exists");
        }

        UserRole role = UserRole.MANAGER;

        if (registerRequest.getRole() != null) {
            role=registerRequest.getRole();
        }

        User userToSave = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phoneNumber(registerRequest.getPhoneNumber())
                .role(role)
                .build();

        userRepository.save(userToSave);

        return Response.builder()
                .status(200)
                .message("user created successfully")
                .build();
    }

    @Override
    public Response loginUser(LoginRequest loginRequest) {
       User user = userRepository.findByEmail(loginRequest.getEmail())
               .orElseThrow(()-> new NotFoundException("Email not Found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("password does not match");
        }
        String token = jwtUtils.generateToken(user.getEmail());

        return Response.builder()
                .status(200)
                .message("user logged in successfully")
                .role(user.getRole())
                .token(token)
                .expirationTime("6 month")
                .build();
    }

    @Override
    public Response getAllUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<UserDTO> userDTOS = modelMapper.map(users, new TypeToken<List<UserDTO>>() {}.getType());
        userDTOS.forEach(userDTO -> userDTO.setTransactions(null));
        return Response.builder()
                .status(200)
                .message("success")
                .users(userDTOS)
                .build();
    }

    @Override
    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new NotFoundException("User Not Found"));

        user.setTransactions(null);

        return user;
    }

    @Override
    public Response updateUser(Long id, UserDTO userDTO) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("User Not Found"));

        if (userDTO.getEmail() != null) existingUser.setEmail(userDTO.getEmail());
        if (userDTO.getName() != null) existingUser.setName(userDTO.getName());
        if (userDTO.getPhoneNumber() != null) existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getRole() != null) existingUser.setRole(userDTO.getRole());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPhoneNumber(passwordEncoder.encode(userDTO.getPassword()));
        }

        userRepository.save(existingUser);

        return Response.builder()
                .status(200)
                .message("User Successfully updated")
                .build();
    }

    @Override
    public Response deleteUser(Long id) {

         userRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("User Not Found"));
         userRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("User Successfully Deleted")
                .build();
    }

    @Override
    public Response getUserTransactions(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("User Not Found"));

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        userDTO.getTransactions().forEach(transactionDTO -> {
            transactionDTO.setSupplier(null);
            transactionDTO.setUser(null);
        });
        return Response.builder()
                .status(200)
                .message("success")
                .user(userDTO)
                .build();
    }

    @Override
    public List<String> extractTextForEmbedding() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<UserDTO> userDTOS = userMapper.map(users, new TypeToken<List<UserDTO>>() {}.getType());
        return userDTOS.stream().map(UserDTO::getTextForEmbedding).collect(Collectors.toList());
    }


}
