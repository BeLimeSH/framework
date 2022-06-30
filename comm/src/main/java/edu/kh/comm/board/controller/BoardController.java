package edu.kh.comm.board.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.kh.comm.board.model.service.BoardService;
import edu.kh.comm.board.model.vo.BoardDetail;
import edu.kh.comm.member.model.vo.Member;

@Controller
@RequestMapping("/board")
@SessionAttributes({"loginMember"})
public class BoardController {

	@Autowired
	private BoardService service;
	
	//게시글 목록 조회
	
	//@PathVariable("value") : URL 경로에 포함되어있는 값을 변수로 사용할 수 있게하는 역할
	//-> 자동으로 request scope에 등록됨 -> jsp에서 ${value} EL 작성 가능
	
	//PathVariable : 요청 자원을 식별하는 경우 
	
	//QueryString : 정렬, 검색 등의 필터링 옵션
	
	@GetMapping("/list/{boardCode}")
	public String boardList(@PathVariable("boardCode") int boardCode,
							@RequestParam(value="cp", required = false, defaultValue = "1") int cp,
							Model model	) {
		
		//게시글 목록 조회 서비스 호출
		//1) 게시판 이름 조회 -> 인터셉터로 application에 올려둔 boardTypeList?
		//2) 페이지네이션 객체 생성(listCount)
		//3) 게시글 목록 조회
		
		Map<String, Object> map = service.selectBoardList(cp, boardCode);
		
		model.addAttribute("map", map);
		
		return "board/boardList";
	}
	
	
	//게시글 상세조회
	@GetMapping("/detail/{boardCode}/{boardNo}")
	public String boardDetail( @PathVariable("boardCode") int boardCode,
							   @PathVariable("boardNo") int boardNo,
							   @RequestParam(value="cp", required=false, defaultValue="1") int cp,
							   Model model,
							   HttpSession session,
							   HttpServletRequest req, HttpServletResponse resp) {
		
		//게시글 상세 조회 서비스 호출
		BoardDetail detail = service.selectboardDetail(boardNo);
		
		//쿠키를 이용한 조회수 증복 증가 방지 코드 + 작성자 본인이라면 조회수 증가X
//		int memberNo = loginMember.getMemberNo();
		
		//@ModelAttribute("loginMember") Member loginMember (사용불가)
		//이유? @ModelAttribute는 별도의 required 속성이 없어서 무조건 필수!
		//   -> 세션에 loginMember가 없으면 예외 발생
		
		// 해결방법 : HttpSession을 이용
		
		if(detail != null) {
			Member loginMember = (Member)session.getAttribute("loginMember");
			
			int memberNo = 0;
			
			if(loginMember != null) {
				memberNo = loginMember.getMemberNo();
			}
			
			//글쓴이와 현재 클라이언트가 같지 않은 경우 -> 조회수 증가
			if(detail.getMemberNo() != memberNo) {
				
				Cookie cookie = null; //기존에 존재하던 쿠키를 저장하는 변수
				Cookie[] cArr = req.getCookies(); //쿠키 얻어오기
				
				if(cArr != null && cArr.length > 0) { //얻어온 쿠키가 있을 경우
					
					for(Cookie c : cArr) { //얻어온 쿠키 중 이름(key)이 "readBoardNo"가 있으면 얻어오기
						
						if(c.getName().equals("readBoardNo")) {
							cookie = c;
						}
					}
				}
				
				int result = 0;
				
				if(cookie == null) { //기존에 "readBoardNo" 이름의 쿠키가 없던 경우
					cookie = new Cookie("readBoardNo", boardNo + "");
					
					//조회수 증가 서비스 호출
					result = service.updateReadCount(boardNo);
					
				} else {
					//기존에 "readBoardNo" 이름의 쿠키가 있을 경우
					//-> 쿠키에 저장된 값 뒤쪽에 현재 조회된 게시글 번호를 추가
					//	 단, 기존 쿠키값에 중복되는 번호가 없어야 함.
					
					String[] temp = cookie.getValue().split("/"); //기존 value
					
					List<String> list = Arrays.asList(temp); //배열 -> List 변환
					
					// List.indexOf(Object)
					// - List에서 Object와 일치하는 부분의 시작 인덱스를 반환
					// - 일치하는 부분이 없으면 -1 반환
					if(list.indexOf(boardNo+"") == -1) { //기존 값에 같은 글번호가 없다면 추가
						
						cookie.setValue( cookie.getValue() + "/" + boardNo );
						
						//조회수 증가 서비스 호출
						result = service.updateReadCount(boardNo); 
					}
				}
				
				if(result > 0) {
					detail.setReadCount(detail.getReadCount() +1); //이미 조회된 데이터 DB와 동기화
					
					// 조회 증가 성공 시 기존 쿠키에 게시글 번호를 누적
//					cookie = new Cookie("boardNo-" + boardNo, boardNo + "");
					
					cookie.setPath(req.getContextPath());
					cookie.setMaxAge(60*60*1); //1시간
					resp.addCookie(cookie);
				}
			}
		}
		
		model.addAttribute("detail", detail);
		return "board/boardDetail";
	}
	
	
	//게시글 작성 화면 전환
	@GetMapping("/write/{boardCode}")
	public String boardWriteForm(@PathVariable("boardCode") int boardCode,
								 String mode) {
		
		if(mode.equals("update")) {
			//게시글 상세조회 서비스 호출
		}
		
		return "board/boardWriteForm";
	}
	
	
	//게시글 작성(삽입/수정)
	@PostMapping("/write/{boardCode}")
	public String boardWrite( BoardDetail detail, //boardTitle, boardContent
							  @RequestParam(value="images", required=false) List<MultipartFile> imageList, //업로드 파일(이미지) 리스트
							  @PathVariable("boardCode") int boardCode,
							  String mode,
							  @ModelAttribute("loginMember") Member loginMember,
							  HttpServletRequest req,
							  RedirectAttributes ra ) throws IOException {
		
		// 1) 로그인한 회원 번호 얻어와서 detial에 세팅
		detail.setMemberNo( loginMember.getMemberNo() );
		
		// 2) 이미지 저장 경로 얻어오기(webPath, folderPath)
		String webPath = "/resources/images/board/";
		String folderPath = req.getSession().getServletContext().getRealPath(webPath);
		
		if(mode.equals("insert")) { //삽입
			
			// 게시글 부분 삽입(제목, 내용, 회원번호, 게시판코드)
			// -> 삽입된 게시글의 번호(boardNo) 반환 (왜? 삽입 끝나면 상세조회로 리다이렉트)
			
			// 게시글에 포함된 이미지 정보 삽입(0~5개 게시글 번호 필요)
			// -> 실제 파일로 변환해서 서버에 저장(transFer())
			
			// 두 번의 insert 중 한 번이라도 실패하면 전체 rollback (트랜잭션 처리)
			
			int boardNo = service.insertBoard(detail, imageList, webPath, folderPath);
			
			String path = null;
			String message = null;

			if(boardNo > 0) {
				path = "../detail/" + boardCode + "/" + boardNo;
				message = "게시글이 등록되었습니다.";
			} else {
				path = req.getHeader("referer");
				message = "게시글 삽입 실패...";
			}
			
			ra.addFlashAttribute("message", message);
			
			return "redirect:" + path;
			
		} else { //수정
			
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
