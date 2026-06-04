package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAccessService {

    // 채팅 접근 권한 검사에 필요한 사용자 정보를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 학생이 어느 팀에 속해 있는지 확인하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 팀별 채팅방을 조회하는 Repository 필드입니다.
    private final ChatRoomRepository chatRoomRepository;

    // 채팅 채널 정보를 조회하는 Repository 필드입니다.
    private final ChatChannelRepository chatChannelRepository;

    // userId로 채팅 기능에서 사용할 사용자 엔티티를 조회하는 기능입니다.
    public User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    // 학생 본인이 속한 팀의 채팅방을 조회하는 기능입니다.
    public ChatRoom getMyChatRoom(String userId) {
        TeamUser teamUser = teamUserRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("아직 배정된 팀이 없습니다."));

        // 현재 기획에서는 팀 확정 후 팀마다 채팅방 1개가 생성되는 구조입니다.
        // 그래서 학생의 팀 id로 자신의 채팅방을 찾습니다.
        return chatRoomRepository.findByTeamId(teamUser.getTeam().getId())
                .orElseThrow(() -> new IllegalArgumentException("팀 채팅방이 없습니다."));
    }

    // 특정 채팅방을 조회하고 해당 사용자가 접근 가능한 팀인지 검사하는 기능입니다.
    public ChatRoom getAccessibleRoom(Long roomId, String userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        checkTeamAccess(room.getTeam().getId(), userId);
        return room;
    }

    // 특정 채널을 조회하고 해당 사용자가 채널의 팀에 접근 가능한지 검사하는 기능입니다.
    public ChatChannel getAccessibleChannel(Long channelId, String userId) {
        ChatChannel channel = chatChannelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅 채널입니다."));

        checkTeamAccess(channel.getChatRoom().getTeam().getId(), userId);
        return channel;
    }

    // 관리자 기능에서 권한 검사 없이 채널 존재 여부만 확인하고 조회하는 기능입니다.
    public ChatChannel getAdminChannel(Long channelId) {
        return chatChannelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅 채널입니다."));
    }

    // 관리자면 전체 허용하고 학생이면 자기 팀 채팅만 허용하는 기능입니다.
    private void checkTeamAccess(Long teamId, String userId) {
        User user = getUser(userId);

        // 선생님 계정은 팀 채팅 관리 화면에서 모든 팀 채팅을 볼 수 있게 열어둡니다.
        // 학생 계정은 자기 팀에 속한 채팅방/채널만 접근할 수 있습니다.
        if (user.getAccountRole() == AccountRole.ADMIN) {
            return;
        }

        boolean isTeamMember = teamUserRepository.existsByTeamIdAndUserUserId(teamId, userId);
        if (!isTeamMember) {
            throw new IllegalArgumentException("이 팀 채팅에 접근할 권한이 없습니다.");
        }
    }
}
