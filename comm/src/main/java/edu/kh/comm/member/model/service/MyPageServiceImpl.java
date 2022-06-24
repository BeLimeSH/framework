package edu.kh.comm.member.model.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.kh.comm.common.Util;
import edu.kh.comm.member.model.dao.MyPageDAO;
import edu.kh.comm.member.model.vo.Member;

@Service
public class MyPageServiceImpl implements MyPageService {
	
	@Autowired
	private MyPageDAO dao;
	
	// 암호화를 위한 bcrypt 객체 의존성 주입(DI)
	@Autowired
	private BCryptPasswordEncoder bcrypt;

	//회원 정보 수정 구현
	@Override
	public int updateInfo(Map<String, Object> paramMap) {
		return dao.updateInfo(paramMap);
	}

	//비밀번호 변경
	@Override
	public int updatePw(Map<String, Object> paramMap) {
		
		int memberNo = (int)paramMap.get("memberNo");
		int result = 0;
		
		// 1) DB에서 현재 회원의 비밀번호를 조회
		String memberPw = dao.selectPw(memberNo);
		
		// 2) 입력된 현재 비밀번호(평문)
		//    조회된 비밀번호(암호화)를 비교(bcrypt.matches() 이용)
		String currentPw = (String)paramMap.get("currentPw");
		
		// 3) 비교 결과가 일치하면
		//	  새 비밀번호를 암호화해서 update구문 수행
		if( bcrypt.matches( currentPw , memberPw ) ) {
			
			paramMap.put("newPw", bcrypt.encode((String)paramMap.get("newPw")));
			
			result = dao.updatePw(paramMap);
			
		}
		
		return result;
	}

	//회원 탈퇴 서비스 구현
	@Override
	public int secession(Member loginMember, String secessionPw) {
		
		int result = 0;
		String memberPw = dao.selectPw(loginMember.getMemberNo());
		
		if( bcrypt.matches( secessionPw , memberPw ) ) {
			
			result = dao.seccession(loginMember.getMemberNo());
			
		}
		return result;
	}

	//프로필 이미지 수정 서비스 구현
	@Override
	public int updateProfile(Map<String, Object> map) throws IOException {
		//webPath, folderPath, uploadImage, delete(String), memberNo
		
		MultipartFile uploadImage = (MultipartFile) map.get("uploadImage");
		String delete = (String) map.get("delete"); // "0" | "1" (삭제 됨)
		
		// 프로필 이미지 삭제 여부를 확인해서 
		// 삭제가 아닌 경우(== 새 이미지로 변경) -> 업로드된 파일명을 변경
		// 삭제된 경우 -> NULL 값을 준비 (DB에 update)
		
		String renameImage = null;	//변경된 파일명 저장
		
		if(delete.equals("0")) { //이미지가 변경된 경우
			// 파일명 변경
			renameImage = Util.fileRename( uploadImage.getOriginalFilename() );
			
//			map.put("profileImage", "변경된 파일명");
			map.put("profileImage", map.get("webPath") + renameImage );
								// /resources/images/memberProfile/202...15.jpg
			
		} else {
			map.put("profileImage", null); //null 값 준비
		}
		
		// DAO를 호출해서 프로필 이미지 수정
		int result = dao.updateProfile(map);
		
		// DB 수정 성공시 메모리에 임시 저장되어 있는 파일을 서버에 저장
		if(result > 0 && map.get("profileImage") != null) {
			uploadImage.transferTo( new File( map.get("folderPath") + renameImage ) );
			
			//transferTo() : 해당 파일을 지정된 경로 + 이름으로 저장
		}
		return result;
	}

}
