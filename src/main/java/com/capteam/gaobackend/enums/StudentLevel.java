package com.capteam.gaobackend.enums;

public enum StudentLevel {
    UPPER, //상
    // AI 분석 결과가 상/중/하보다 세분화되어 내려오는 경우를 저장하기 위한 중상 단계입니다.
    UPPER_MIDDLE, //중상
    MIDDLE,//중
    // AI 분석 결과가 상/중/하보다 세분화되어 내려오는 경우를 저장하기 위한 중하 단계입니다.
    LOWER_MIDDLE, //중하
    LOWER//하
  }