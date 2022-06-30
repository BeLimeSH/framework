package edu.kh.comm.board.model.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.kh.comm.board.model.dao.BoardDAO;
import edu.kh.comm.board.model.exception.InsertFailException;
import edu.kh.comm.board.model.vo.Board;
import edu.kh.comm.board.model.vo.BoardDetail;
import edu.kh.comm.board.model.vo.BoardImage;
import edu.kh.comm.board.model.vo.BoardType;
import edu.kh.comm.board.model.vo.Pagination;
import edu.kh.comm.common.Util;

@Service	//비즈니스 로직 처리하는 클래스 명시 + bean 등록
public class BoardServiceImpl implements BoardService {

	@Autowired
	private BoardDAO dao;

	// 게시판 코드, 이름 조회
	@Override
	public List<BoardType> selectBoardType() {
		return dao.selectBoardType();
	}

	//게시판 목록 조회 서비스 구현
	@Override
	public Map<String, Object> selectBoardList(int cp, int boardCode) {
		
		//2) 페이지네이션 객체 생성(listCount)
		int listCount = dao.getListCount(boardCode);
		Pagination pagination = new Pagination(cp, listCount);
		
		//3) 게시글 목록 조회
		List<Board> boardList = dao.selectBoardList(pagination, boardCode);
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		// map에 담기
		map.put("pagination", pagination);
		map.put("boardList", boardList);
		
		return map;
	}

	// 게시글상세조회
	@Override
	public BoardDetail selectboardDetail(int boardNo) {
		return dao.selectBoardDetail(boardNo);
	}

	//조회 수 증가
	@Override
	public int updateReadCount(int boardNo) {
		return dao.updateReadCount(boardNo);
	}

	//게시글 삽입 + 이미지 삽입 서비스 구현
	//Spring에서 트랜잭션 처리하는 방법
	// * AOP(관점 지향 프로그래밍)을 이용해서 DAO -> Service 또는 Service 코드 수행 시점에
	//	예외가 발생하면 rollback을 수행
	
	//방법 1) <tx:advice> XML을 이용한 방법 -> 패턴을 지정하여 일치하는 메서드 호출 시 자동으로 트랜잭션 제어
	//방법 2) @Transactional 선언적 트랜잭션 처리 방법
	//		  -> RuntimeException (Unchecked Exception) 처리를 기본값으로 갖는다.
	
	// checked Exception : 예외처리가 필수 ( trnasferTo() ) -> SQL 관련 예외, 파일 업로드 관련 예외
	// Unchecked Exception : 예외처리가 선택 ( int a = 10/0; )
	
	//rollbackFor : rollback을 수행하기 위한 예외의 종류 작성
	
	@Transactional(rollbackFor = {Exception.class})
	@Override
	public int insertBoard(BoardDetail detail, List<MultipartFile> imageList, String webPath, String folderPath) throws IOException {
		
		// 1. 게시글 삽입
		
		// 1) XSS 방지 처리 + 개행문자 처리
		detail.setBoardTitle( Util.XSSHandling( detail.getBoardTitle() ) );
		detail.setBoardContent( Util.XSSHandling( detail.getBoardContent() ) );
		
		detail.setBoardContent( Util.newLineClear( detail.getBoardContent() ) );
		
		// 2) 게시글 삽입 DAO 호출 후 게시글 번호 반환 받기
		
		// * 게시글 번호를 먼저 따로 생성했던 이유
		// 1. 서비스 결과 반환 후 컨트롤러에서 상세조회로 리다이렉트하기 위해
		// 2. 동일한 시간에 삽입이 2회 이상 진행된 경우 시퀀스 번호가 의도와 달리 여러 번 증가해서
		//    이후에 작성된 이미지 삽입 코드에 영햔을 미치는 것을 방지하기 위해서
		
		int boardNo = dao.insertBoard(detail);
		
		if(boardNo > 0) {
			//이미지 삽입
			
			//imageList : 실제 파일이 담겨있는 리스트
			//boardImageList : DB에 삽입할 이미지 정보만 담겨있는 리스트
			//reNameList : 변경된 파일명이 담겨있는 리스트
			
			List<BoardImage> boardImageList = new ArrayList<BoardImage>();
			List<String> reNameList = new ArrayList<String>();
			
			// imageList에 담겨있는 파일 정보 중 실제 업로드된 파일만 분류하는 작업
			for(int i=0; i<imageList.size(); i++) {
				
				if(imageList.get(i).getSize() > 0) { //i번째 요소에 업로드된 이미지가 있을 경우
					
					//변경된 파일명 저장
					String reName = Util.fileRename( imageList.get(i).getOriginalFilename() );
					reNameList.add(reName);
					
					//BoardImage 객체를 생성하여 값 세팅 후 boardImageList에 추가
					BoardImage img = new BoardImage();
					img.setBoardNo(boardNo);
					img.setImageLevel(i);
					img.setImageOriginal(imageList.get(i).getOriginalFilename());
					img.setImageReName( webPath + reName ); //웹 접근 경로 + 변경된 파일명
					
					boardImageList.add(img);
				}
			} //for 종료
			
			// 분류 작업 종료 후 boardImageList가 비어있지 않은 경우 == 파일이 업로드가 된 경우
			if( !boardImageList.isEmpty() ) {
				int result = dao.insertBoardImageList(boardImageList);
				
				// result == 삽입 성공한 행의 개수
				if(result == boardImageList.size()) { // 삽입된 행의 개수와 업로드 이미지 수가 같을 경우
					
					// 서버에 이미지 저장
					for(int i=0; i<boardImageList.size(); i++) {
						
						int index = boardImageList.get(i).getImageLevel();
						
						imageList.get(index).transferTo(new File(folderPath + reNameList.get(i)));
						
					}
				} else { //이미지 삽입 실패 시
					
					// 강제로 예외를 발생시켜 rollback을 수행하게 함
					// -> 사용자 정의 예외
					
					throw new InsertFailException();
				}
			}
		}
		return boardNo;
	}
	
	
	
	
	
	
	
	
}
