package com.inventory.repository;

import com.inventory.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Luuanz");
    }

    @Test
    @DisplayName("Luu va tim nguoi dung theo ID")
    void testSaveAndFindById(){
        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem người dùng đã được lưu và tìm lại bằng ID có tồn tại không
        assertThat(found).isPresent();
        // Testcase: Kiểm tra tên của người dùng tìm được có khớp với giá trị ban đầu không
        assertThat(found.get().getName()).isEqualTo("Luuanz");
    }

    @Test
    @DisplayName("Luu va lay tat ca nguoi dung")
    void testSaveAndFindAll(){
        User saved = userRepository.save(user);

        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        // Testcase: Kiểm tra danh sách người dùng sau khi lưu có không rỗng không
        assertThat(users).isNotEmpty();
        // Testcase: Kiểm tra người dùng đầu tiên trong danh sách có tên khớp với giá trị ban đầu không
        assertThat(users.get(0).getName()).isEqualTo("Luuanz");
    }

    @Test
    @DisplayName("Luu va xoa nguoi dung theo ID")
    void testSaveAndDeleteById(){
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());
        Optional<User> found = userRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem người dùng đã bị xóa và không còn tồn tại không
        assertThat(found).isNotPresent();
    }

    @Test
    @DisplayName("Tim nguoi dung theo reset password token")
    void testFindByResetPasswordToken() {
        User resetUser = new User();
        resetUser.setName("Reset User");
        resetUser.setResetPasswordToken("token123");
        userRepository.save(resetUser);

        User found = userRepository.findByResetPasswordToken("token123");
        // Testcase: Kiểm tra xem người dùng được tìm thấy bằng token có tồn tại không
        assertThat(found).isNotNull();
        // Testcase: Kiểm tra tên của người dùng tìm được bằng token có khớp với giá trị ban đầu không
        assertThat(found.getName()).isEqualTo("Reset User");
    }

    @Test
    @DisplayName("Tim nguoi dung theo email")
    void testFindByEmail() {
        User emailUser = new User();
        emailUser.setName("Test User");
        emailUser.setEmail("test@example.com");
        userRepository.save(emailUser);
        //Optional<Dtg> chap nhan null va ko co ngoai le null
        Optional<User> found = userRepository.findByEmail("test@example.com");
        //Testcase1: Kiểm tra xem Optional có chứa giá trị không
        assertThat(found).isPresent();
        //Testcase2: Xác thực đtg xem có phải là cái mình save không?
        assertThat(found.get().getName()).isEqualTo("Test User");
    }
}
