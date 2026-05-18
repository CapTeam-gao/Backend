package com.capteam.gaobackend.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "studentChatRoom")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(EntityListeners.class)
public class StudentChatRoom extends BaseTimeEntity{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chatRoomNameId")
    private ChatRoom chatRoomId;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User userId;
}
