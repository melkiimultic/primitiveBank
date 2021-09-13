package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.domain.UserInfo;
import com.julenka.api.primitiveBank.dto.CreateUserDTO;
import com.julenka.api.primitiveBank.dto.GetUserDTO;
import com.julenka.api.primitiveBank.dto.UserInfoDTO;
import com.julenka.api.primitiveBank.exceptions.EntityAlreadyExistsException;
import com.julenka.api.primitiveBank.mappers.UserInfoMapper;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final CurrentUserService currentUserService;
    private final UserInfoMapper userInfoMapper;

    @Transactional
    public Long createUser(CreateUserDTO createUserDTO) {
        userRepo.findOneByUsername(createUserDTO.getUsername()).
                ifPresent(e -> {
                    throw new EntityAlreadyExistsException("User with username " + createUserDTO.getUsername() +
                            " already exists");
                });
        User user = new User();
        user.setUsername(createUserDTO.getUsername());
        user.setPassword(encoder.encode(createUserDTO.getPassword()));
        user.setAuthorities(List.of(Roles.USER));
        UserInfoDTO userInfoDTO = createUserDTO.getUserinfo();
        UserInfo info = new UserInfo();
        info.setFirstName(userInfoDTO.getFirstName());
        info.setLastName(userInfoDTO.getLastName());
        user.setUserInfo(info);
        User saved = userRepo.save(user);
        return saved.getId();
    }

    @Transactional
    public void changeUserInfo(UserInfoDTO userInfoDTO) {
        User currentUser = currentUserService.getCurrentUser();
        currentUser.setUserInfo(userInfoMapper.toUserInfo(userInfoDTO));
        userRepo.save(currentUser);

    }

    @Transactional
    public void deleteCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();
        userRepo.delete(currentUser);
    }

    @Transactional
    public void deleteUserById(Long id) {
        userRepo.deleteById(id);
    }

    @Transactional
    public GetUserDTO getUserByUsername(String username) {
        GetUserDTO getUserDTO = new GetUserDTO();
        Optional<User> userOptional = userRepo.findOneByUsername(username);
        if (!userOptional.isPresent()) {
            throw new EntityNotFoundException();
        }
        User user = userOptional.get();
        getUserDTO.setUsername(user.getUsername());
        getUserDTO.setId(user.getId());
        getUserDTO.setUserinfo(userInfoMapper.fromUserInfo(user.getUserInfo()));
        getUserDTO.setAuthorities(user.getAuthorities());
        return getUserDTO;
    }


}
