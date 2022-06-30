package edu.kh.comm.board.model.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import edu.kh.comm.board.model.vo.BoardDetail;
import edu.kh.comm.board.model.vo.BoardType;

public interface BoardService {

	/**
	 * 게시판 코드 / 이름 조회
	 * @return boardTypeList
	 */
	List<BoardType> selectBoardType();

	/**
	 * 게시글 목록 조회 Service
	 * @param cp
	 * @param boardCode
	 * @return map
	 */
	Map<String, Object> selectBoardList(int cp, int boardCode);

	/**
	 * 게시글 상세조회
	 * @param boardNo
	 * @return
	 */
	BoardDetail selectboardDetail(int boardNo);

	/**
	 * 조회수 증가
	 * @param boardNo
	 * @return result
	 */
	int updateReadCount(int boardNo);
	
	/**
	 * 게시글 삽입 + 이미지 삽입 서비스
	 * @param detail
	 * @param imageList
	 * @param webPath
	 * @param folderPath
	 * @return boardNo
	 */
	int insertBoard(BoardDetail detail, List<MultipartFile> imageList, String webPath, String folderPath) throws IOException;

}
