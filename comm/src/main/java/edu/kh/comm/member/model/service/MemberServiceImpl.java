package edu.kh.comm.member.model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.kh.comm.member.model.dao.MemberDAO;

@Service //비즈니스 로직(데이터 가공, DB 연결 등)을 처리하는 클래스임을 명시 + bean으로 등록
public class MemberServiceImpl implements MemberService{

	@Autowired //bean으로 등록된 객체 중 같은 타입이 있으면 의존성 주입(DI)
	private MemberDAO dao;
	
}
