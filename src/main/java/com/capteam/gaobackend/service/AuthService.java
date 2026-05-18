package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.auth.request.ChangePasswordRequestDto;
import com.capteam.gaobackend.dto.auth.request.LoginRequestDto;
import com.capteam.gaobackend.dto.auth.response.SessionUserDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    public SessionUserDto doLogin(LoginRequestDto dto) {
        //유저 있는지 없는지
        User user = userRepository.findByStudentId(dto.getStudentId()).orElseThrow(UserNotFoundException::new);

        if(!user.isPasswordEncoded()) {
            if(!user.getPassword().equals(dto.getPassword()))
                throw new RuntimeException("비밀번호 틀림");


            String encoded = passwordEncoder.encode(dto.getPassword()); // 비번 해쉬하기

            user.setPassword(encoded);      //해쉬된 비번 저장
            user.setPasswordEncoded(true);  //비번 해쉬된거 true

            userRepository.save(user);  //저장

        } else {
            if(!passwordEncoder.matches(dto.getPassword(),user.getPassword())) {   //비번 암호화된 사용자 비번 검증
                throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
            }
        }


        return SessionUserDto.from(user);
    }





    public void changePassword(ChangePasswordRequestDto dto){
        var user = getAuthenticatedUser();
        if(!passwordEncoder.matches(dto.getPassword(),user.getPassword())){
            throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }

        if(!dto.getNewPassword().equals(dto.getCheckPassword())) {
            throw new BadCredentialsException("새 비밀번호가 일치하지 않습니다.");
        }

        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));

        userRepository.save(user);
    }


    private User getAuthenticatedUser() {
        var userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());

        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
