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

	//검색 게시글 목록 조회 서비스 구현
	@Override
	public Map<String, Object> searchBoardList(Map<String, Object> paramMap) {
		
		//검색 조건에 맞는 게시글 목록의 전체 개수 조회
		int listCount = dao.searchListCount( paramMap );
		
		//페이지네이션 객체 생성
		Pagination pagination = new Pagination((int)paramMap.get("cp"), listCount);
		
		//검색 조건에 맞는 게시글 목록 조회(페이징 처리 적용)
		List<Board> boardList = dao.searchBoardList(paramMap, pagination);
		
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
		
		// * 게시글 번호를 먼저 따로 생성했던 이유..
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
			
			// 1. DB에 이미지 경로 저장하기 => webPath 사용
			
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
			
			// 2. 서버의 실제 폴더에 이미지 파일 저장하기! => folderPath 사용
			
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

	//게시글 수정
	//선언적 트랜잭션 처리
	@Transactional(rollbackFor = {Exception.class}) //모든 종류의 예외 발생 시 롤백
	@Override
	public int updateBoard(BoardDetail detail, List<MultipartFile> imageList, String webPath, String folderPath,
			String deleteList) throws IOException {
		
		// 1) XSS, 개행문자 처리
		detail.setBoardTitle( Util.XSSHandling( detail.getBoardTitle() ) );
		detail.setBoardContent( Util.XSSHandling( detail.getBoardContent() ) );
		detail.setBoardContent( Util.newLineHandling( detail.getBoardContent() ) );
		
		// 2) 게시글(제목, 내용, 마지막 수정일(sysdate) / boardNo 필요)만 수정하는 DAO 호출
		int result = dao.updateBoard(detail);
		
		if(result>0) {
			// 3) 업로드된 이미지만 분류하는 작업 수행
			
			List<BoardImage> boardImageList = new ArrayList<BoardImage>();
			List<String> reNameList = new ArrayList<String>();
			
			// 1. DB에 이미지 경로 저장하기 => webPath 사용
			
			// imageList에 담겨있는 파일 정보 중 실제 업로드된 파일만 분류하는 작업
			for(int i=0; i<imageList.size(); i++) {
				
				if(imageList.get(i).getSize() > 0) { //i번째 요소에 업로드된 이미지가 있을 경우
					
					//변경된 파일명 저장
					String reName = Util.fileRename( imageList.get(i).getOriginalFilename() );
					reNameList.add(reName);
					
					//BoardImage 객체를 생성하여 값 세팅 후 boardImageList에 추가
					BoardImage img = new BoardImage();
					img.setBoardNo( detail.getBoardNo() );
					img.setImageLevel(i);
					img.setImageOriginal(imageList.get(i).getOriginalFilename());
					img.setImageReName( webPath + reName ); //웹 접근 경로 + 변경된 파일명
					
					boardImageList.add(img);
				}
			} //for 종료
			
			// 4) deleteList를 이용해서 이미지 삭제
			if( !deleteList.equals("") ) {
				Map<String, Object> map = new HashMap<String, Object>();
				
				map.put("boardNo", detail.getBoardNo());
				map.put("deleteList", deleteList);
				
				result = dao.deleteBoardImage(map);
				
			}
			
			if(result >0) {
				// 5) boardImageList를 순차 접근하면서 하나씩 업데이트

				for(BoardImage img : boardImageList) {
					
					result = dao.updateBoardImage(img); // 변경명, 원본명, 게시글 번호, 레벨
					// 결과 1 -> 수정O -> 기존 이미지가 있었다.
					
					// 결과 0 -> 수정X -> 기존 이미지가 없었다.
					// -> insert 작업 수행
					
					// 6) update를 실패하면 insert
					if(result == 0) {
						result = dao.insertBoardImage(img);
						//-> 값을 하나씩 대입해서 삽입하는 경우 결과가 0이 나올 수 없다!
						// 단, 예외(제약조건 위배, sql 문법 오류 등)은 발생할 수 있다.
					}
				} //for 종료
				
				// 7) 업로드된 이미지가 있다면 서버에 저장
				if(!boardImageList.isEmpty() && result != 0) {
					for(int i=0; i<boardImageList.size(); i++) {
						
						int index = boardImageList.get(i).getImageLevel();

						// transferTo : 임시 메모리에 있는 파일을 해당 경로로 이동(서버 저장)
						imageList.get(index).transferTo( new File( folderPath + reNameList.get(i) ) );
						
					}
				}
			}
		}
		return result;
	}

	//게시글 삭제 구현
	@Override
	public int deleteBoard(int boardNo) {
		return dao.deleteBoard(boardNo);
	}
	
	
	
	
	
	
	
	
	
	
	
}
