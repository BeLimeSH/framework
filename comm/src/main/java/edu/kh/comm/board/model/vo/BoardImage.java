package edu.kh.comm.board.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardImage {

	private int imageNo;
	private String imageReName;
	private String imageOriginal;
	private int imageLevel;
	private int boardNo;
	
}
