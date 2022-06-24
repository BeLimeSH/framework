package edu.kh.comm.member.model.service;

import java.io.IOException;
import java.util.Map;

import edu.kh.comm.member.model.vo.Member;

public interface MyPageService {

	/**
	 * 회원 정보 수정 Service
	 * @param paramMap
	 * @return result
	 */
	int updateInfo(Map<String, Object> paramMap);

	/**
	 * 비밀번호 변경  Service
	 * @param paramMap
	 * @return result
	 */
	int updatePw(Map<String, Object> paramMap);

	/**
	 * 회원 탈퇴 Service
	 * @param loginMember
	 * @return result
	 */
	int secession(Member loginMember, String secessionPw);

	/**
	 * 프로필 이미지 수정
	 * @param map
	 * @return result
	 */
	int updateProfile(Map<String, Object> map) throws IOException;

}
