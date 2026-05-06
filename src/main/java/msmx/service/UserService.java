package msmx.service;

import msmx.entity.User;
import msmx.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password) {
        logger.info("尝试登录: username={}", username);
        User user = userRepository.findByUsername(username);
        if (user != null && password != null && password.equals(user.getPassword())) {
            logger.info("登录成功: username={}", username);
            return user;
        }
        logger.warn("登录失败: username={}", username);
        throw new RuntimeException("用户名或密码错误");
    }

    public User register(User user) {
        logger.info("尝试注册: username={}", user.getUsername());
        if (userRepository.findByUsername(user.getUsername()) != null) {
            logger.warn("注册失败，用户名已存在: username={}", user.getUsername());
            throw new RuntimeException("用户名已存在");
        }
        User saved = userRepository.save(user);
        logger.info("注册成功: id={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }
}