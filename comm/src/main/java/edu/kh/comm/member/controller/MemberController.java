package edu.kh.comm.member.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import edu.kh.comm.member.model.service.MemberService;
import edu.kh.comm.member.model.service.MemberServiceImpl;
import edu.kh.comm.member.model.vo.Member;

// POJO 기반 프레임워크: 외부 라이브러리 상속 X

// class : 객체를 만들기 위한 설계도
// -> 객체로 생성 되어야지 기능 수행이 가능하다
// --> IOC(제어의 역전, 객체 생명 주기를 스프링이 관리)를 이용하여 객체 생성
// 		** 이 때, spring이 생성한 객체를 [bean]이라고 한다 **

// bean 등록 == 스프링이 객체로 만들어서 가지고 있어라
//@Component //해당 클래스를 bean으로 등록하라는 컴파일 주석(Annotation)

@Controller //생성된 bean이 Controller임을 명시 + bean으로 등록
@RequestMapping("/member") //localhost:8080/comm/member 이하의 요청을 처리하는 컨트롤러
@SessionAttributes({"loginMember"}) //Model에 추가된 값의 key와 어노테이션에 작성된 값이 같으면
									//해당 값을 Session scope로 이동시키는 역할
public class MemberController {
	
	private Logger logger = LoggerFactory.getLogger(MemberController.class);
	
	@Autowired //bean으로 등록된 객체 중 타입이 같거나, 상속 관계인 bean을 Spring이 주입해주는 역할
	private MemberService service; //-> 의존성 주입(DI, Dependency Injection)

	
	//Controller : 요청/응답을 제어하는 역할을 하는 클래스
	
	/* @RequestMapping : 클라이언트 요청(url)에 맞는 클래스 or 메서드를 연결시켜주는 어노테이션
	
		[위치에 따른 해석]
		- 클래스 레벨 : 공통 주소(프론트 컨트롤러 패턴 지정)
		- 메서드 레벨 : 공통 주소 외 나머지 주소
		
		단, 클래스 레벨에 @RequestMapping이 존재하지 않는다면
		- 메서드 레벨 : 단독 요청 처리 주소
		
		
		[작성법에 따른 해석]

		1) @RequestMapping("url")
		   -> 요청 방식(GET/POST) 관계 없이 url이 일치하는 요청 처리
		
		2) @RequestMapping(value = "url", method = RequestMethod.GET | POST)
		   -> 요청 방식에 따라 요청 처리
		   
		** 메서드 레벨에서 GET/POST 방식을 구분하여 매핑할 경우 **
		
		-> @GetMapping("url") / @PostMapping("url") 사용하는 것이 일반적 (메서드 레벨에서만 사용 가능)
		
	*/
	
	// Argument Resolver 라는 매개변수를 유연하게 처리해주는 해결사 -> 스프링에 내장
	// https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-arguments
	
	// 요청 시 파라미터를 얻어오는 방법 1
	// -> HttpServletRequest 이용
	
	//로그인
//	@RequestMapping("/login")
//	public String login(HttpServletRequest req) {
//		
//		logger.info("로그인 요청 됨");
//		
//		String inputEmail = req.getParameter("inputEmail");
//		String inputPw = req.getParameter("inputPw");
//		
//		logger.debug("inputEmail : " + inputEmail);
//		logger.debug("inputPw : " + inputPw);
//		
//		return "redirect:/";
//	}
	
	// 요청 시 파라미터를 얻어오는 방법 2
	// -> @RequstParam 어노테이션 사용
	
	// @RequestParam("name 속성값") 자료형 변수명
	// - 클라이언트 요청 시 같이 전달된 파라미터를 변수에 저장
	// --> 어떤 파라미터를 변수에 지정할지는 "name속성값"을 이용해서 지정
	
	// 매개변수 지정 시 데이터 타입 파싱을 자유롭게 진행할 수 있음 ex) String -> int로 변환
	
	// [속성]
	// value : input 태그의 name 속성값 (default)
	
	//@RequestParam("inputEmail") == @RequestParam( value = "inputEmail")
	
	// required : 입력된 name 속성값이 필수적으로 파라미터에 포함되어야 하는지를 지정
	// 			  required = true / required = false (기본값 true)
	
	// 400 - 잘못된 요청(Bad Request) : 파라미터가 존재하지 않아 요청이 잘못 됨.
	
	// required = false일 때 파라미터가 없으면 null
	
	// defaultValue : required가 false인 상태에서 파라미터가 존재하지 않은 경우의 값을 지정
	
	// ** @RequestParam을 생략하지만 파라미터를 얻어오는 방법 **
	// -> name 속성 값과 파라미터를 저장할 변수 이름을 동일하게 작성!
	
//	@RequestMapping("/login")	
//	public String login(/* @RequestParam("inputEmail") */ int inputEmail,
//			/* @RequestParam("inputPw") */ String inputPw,
//			@RequestParam(value="inputName", required=false, defaultValue="홍길동") String name) {
//		
//		logger.debug("email : " + inputEmail);
//		logger.debug("pw : " + inputPw);		
//		logger.debug("name : " + name);		
//		
//		//email로 숫자만 입력 받는다고 가정
//		logger.debug( inputEmail + 100 + "");
//		
//		return "redirect:/";
//	}
	
	
	// 요청 시 파라미터를 얻어오는 방법 3
	// -> @ModelAttribute 어노테이션 사용
	
	// [@ModelAttribute를 매개변수에 작성하는 경우]
	
	// @ModelAttribute VO타입 변수명
	// -> 파라미터 중 name 속성값이 VO의 필드와 일치하면
	//	  해당 VO 객체의 필드에 값 세팅
	
	// *** @ModelAttribute를 이용해서 객체에 값을 직접 담는 경우에 대한 주의사항 ***
	
	// 반드시 필요한 내용!
	// - VO 기본 생성자
	// - VO 필드에 대한 Setter
	
	// Getter는 JSP - EL 사용 시 반드시 필요!
	
	//@RequestMapping(value="/login", method=RequestMethod.POST)
	@PostMapping("/login")
	public String login( /* @ModelAttribute */ Member inputMember,
						Model model	) {
		
		//@ModelAttribute 생략 가능
		
		logger.info("로그인 기능 수행됨");
		
		// 아이디, 비밀번호가 일치하는 회원 정보를 조회하는 Service 호출 후 결과 반환 받기
		Member loginMember = service.login(inputMember);
		
		/* Model : 데이터를 맵 형식(K:V) 형태로 담아 전달하는 용도의 객체
		 * -> request, session을 대체하는 객체
		 * 
		 * - 기본 scope : request
		 * - session scope로 변환하고 싶은 경우
		 *   클래스 레벨로 @SessionAttributes를 작성한다
		 * */

		// @SessionAttrubutes 미작성 -> request scope
		model.addAttribute("loginMember", loginMember); //req.setAttribute("loginMember", loginMember);
		
		//session.setAttribute("loginMember", loginMember);
		
		
		return "redirect:/";
	}

	//로그아웃
	@GetMapping("/logout")
	public String logout( /*HttpSession session,*/ SessionStatus status) {
		
		//로그아웃 == 세션을 없애는 것
		// * @SessionAttributes을 이용해서 session scope에 배치된 데이터는
		//	 SessionStatus라는 별도 객체를 이용해야만 없앨 수 있다.
		logger.info("로그아웃 수행됨");
		
		//session.invalidate(); 기존 세션 무효화 방식으로는 안 된다!
		
		status.setComplete(); //세션이 할 일이 완료됨 -> 없앰
		
		return "redirect:/"; //메인페이지 리다이렉트
	}

	
	//회원가입 화면 전환
	@GetMapping("/signUp") //Get 방식 : /comm/member/signUp 요청
	public String singUp() {
		
		return "member/signUp";
	}
	
	
	
	
}
