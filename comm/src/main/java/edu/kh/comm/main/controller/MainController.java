package edu.kh.comm.main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller //컨트롤러임을 명시 + bean 등록 (컴포넌트 스캔 시 bean으로 생성됨)
public class MainController {
	
	@RequestMapping("/main")
	public String mainForward() {
		
		// index.jsp의 forward를 처리하는 mainForward()에서
		// 다시 한 번 common/main.jsp로 forward
		return "common/main";
	}
}
