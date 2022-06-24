package edu.kh.comm.member.model.dao;

import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MyPageDAO {
	
	@Autowired
	private SqlSessionTemplate sqlSession;

	// 파라미터(단순 자료형, VO, Map)
	
	// 파라미터가 Map인 경우 -> Mapper에서 사용시 #{key}를 작성하면
	// key에 대응되는 value가 출력됨
	// ex) #{updateTel} -> '01099999999'
	
	/**
	 * 회원 정보 수정 DAO
	 * @param paramMap
	 * @return result
	 */
	public int updateInfo(Map<String, Object> paramMap) {
		
		return sqlSession.update("myPageMapper.updateInfo", paramMap);
	}

	/**
	 * 비밀번호 불러오기 DAO
	 * @param memberNo
	 * @return memberPw
	 */
	public String selectPw(int memberNo) {
		return sqlSession.selectOne("myPageMapper.selectPw", memberNo);
	}

	/**
	 * 비밀번호 변경 DAO
	 * @param paramMap
	 * @return result
	 */
	public int updatePw(Map<String, Object> paramMap) {
		return sqlSession.update("myPageMapper.updatePw", paramMap);
	}

	/**
	 * 회원 탈퇴 DAO
	 * @param memberNo
	 * @return result
	 */
	public int seccession(int memberNo) {
		return sqlSession.update("myPageMapper.secession", memberNo);
	}

	/**
	 * 프로필 이미지 수정 DAO
	 * @param map
	 * @return result
	 */
	public int updateProfile(Map<String, Object> map) {
		return sqlSession.update("myPageMapper.updateProfile", map);
	}

}
