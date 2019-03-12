package yoo.board;

//DBConnectionMgr(DB접속,관리), BoardDTO(매개변수,반환형)
import java.sql.*;//DB사용
import java.util.*;//ArrayList,List을 사용하기 위해서

public class BoardDAO {  //MemberDAO
	
	   private DBConnectionMgr pool=null;//1.선언
	//추가
	//공통으로 접속할 경우 필요한 멤버변수
		private Connection con=null;
		private PreparedStatement pstmt=null;
		private ResultSet rs=null;
		private String sql="";//실행시킬 SQL구문 저장
	//2.생성자를 통해서 연결=>의존성
	public BoardDAO() {
		try {
			pool=DBConnectionMgr.getInstance();
			System.out.println("pool=>"+pool);
		}catch(Exception e) {
			System.out.println("DB접속오류=>"+e);
		}
	}//생성자

	//1.페이징 처리를 위해서 전체 레코드수를 구해와야한다.
	//select count(*) from board->select count(*) from member; ->getMemberCount()
	
	public int getArticleCount() { //->getMemberCount()
		int x=0;//레코드갯수
		
		try {
			con=pool.getConnection();
			System.out.println("con="+con);
			sql="select count(*) from board";  //select count(*) from member
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			if(rs.next()) { //보여주는 결과가 있다면
				x=rs.getInt(1);// 변수명=rs.get자료형(필드명 또는 인덱스번호)
				                       //필드명이 아니기때문에 select ~ from 사이에 나오는 순서
			}
		}catch(Exception e) {
			System.out.println("getArticleCount()메서드 에러유발"+e);
		}finally {
			pool.freeConnection(con,pstmt,rs);
		}
		return x;
	}
	//2.글목록보기에 대한 메서드 구현->레코드가 한개이상=>한페이지당 10개씩 끊어서 보여준다
	//1.레코드의 시작번호   2.불러올 레코드의 갯수
	public List getArticles(int start,int end) { //getMemberArticle(int start,int end)
		                                                          //id asc name desc
		List articleList=null;//ArrayList articleList=null;
		
		try {
			con=pool.getConnection();
			//그룹번호가 가장 최신의 글을 중심으로 정렬하되,만약에 level이 같은 경우에는
			//step값으로 오름차순을 통해서 몇번째레코드번호를 기준해서 정렬하라.
			sql="select * from board order by ref desc,re_step asc limit ?,?"; //1,10
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, start-1);//mysql은 레코드순번이 내부적으로 0부터 시작
			pstmt.setInt(2, end);
			rs=pstmt.executeQuery();
			//기존의 레코드외에 추가된 레코드를 첨부해서 다같이 보여주는 개념->누적개념(벽돌)
			if(rs.next()) {//레코드가 존재한다면(최소만족 1개)
				//articleList=new List();//X
				//형식)articleList=new 자식클래스명();
				articleList=new ArrayList(end);//10->end갯수만큼 데이터를 담을 공간생성하라
				do {
					BoardDTO article=makeArticleFromResult();
					/*
					BoardDTO article=new BoardDTO();//MemberDTO mem=new MemberDTO()
					article.setNum(rs.getInt("num"));
					article.setWriter(rs.getString("writer"));
					article.setEmail(rs.getString("email"));
					article.setSubject(rs.getString("subject"));
					article.setPasswd(rs.getString("passwd"));
					article.setReg_date(rs.getTimestamp("reg_date"));//오늘날짜->코딩 now()
					article.setReadcount(rs.getInt("readcount"));//default->0
					article.setRef(rs.getInt("ref"));//그룹번호->신규글과 답변글 묶어주는 번호
					article.setRe_step(rs.getInt("re_step"));//답변글의 순서
					article.setRe_level(rs.getInt("re_level"));//들여쓰기(답변의 깊이)
					article.setContent(rs.getString("content"));//글내용
					article.setIp(rs.getString("ip"));//글쓴이의 ip주소->request.getRemoteAddr()
					*/
					//추가
					articleList.add(article);
				}while(rs.next());
			}
		}catch(Exception e) {
			System.out.println("getArticles()메서드 에러유발"+e);
		}finally {
			pool.freeConnection(con, pstmt,rs);
		}
		return articleList;
	}
	//----------게시판의 글쓰기 및 글 답변달기---------------------------------------------------------------------
	//insert into board values(?,,,
	public void insertArticle(BoardDTO article) { //~(MemberDTO mem)
		
		//1.article->신규글인지 답변글인지 구분
		int num=article.getNum();//0(신규글인지) 0이 아닌경우(답변글)
		int ref=article.getRef();
		int re_step=article.getRe_step();
		int re_level=article.getRe_level();
		//테이블에 입력할 게시물 번호를 저장할 변수
		int number=0;
		System.out.println("insertArticle 메서드의 내부의 num=>"+num);
		System.out.println("ref="+ref+",re_step=>"+re_step+",re_level=>"+re_level);
		
		try {
			con=pool.getConnection();
			sql="select max(num) from board"; //최대값+1=실제 저장할 게시물번호
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			if(rs.next()) {//현재 테이블에서 데이터가 한개라도 존재한다면
				number=rs.getInt(1)+1;
			}else { //맨 처음에 레코드가 한개라도 없다면 무조건 number=1
				number=1;
			}
			//만약에 답변글인경우
			if(num!=0) {
				//같은 그룹번호를 가지고 있으면서 나보다 step값이 큰 놈을 찾아서 그 step증가
				sql="update board set re_step=re_step+1 where ref=? and re_step > ?";
				pstmt=con.prepareStatement(sql);
				pstmt.setInt(1, ref);
				pstmt.setInt(2, re_step);
				int update=pstmt.executeUpdate();
				System.out.println("댓글수정유무(update)=>"+update);//1 or 0
				//답변글
				re_step=re_step+1;
				re_level=re_level+1;
			}else {//num=0인 경우이기에 신규글임을 알 수가 있다.
				ref=number;
				re_step=0;
				re_level=0;
			}
			//12개->num,reg_date,reacount(생략)->default->sysdate,now()<-mysql (?대신에)
			sql="insert into board(writer,email,subject,passwd,reg_date,";
			sql+=" ref,re_step,re_level,content,ip)values(?,?,?,?,?,?,?,?,?,?)";
			pstmt=con.prepareStatement(sql);
			pstmt.setString(1, article.getWriter());//웹에서는 Setter Method를 메모리에 저장
			pstmt.setString(2, article.getEmail());
			pstmt.setString(3, article.getSubject());
			pstmt.setString(4, article.getPasswd());
			pstmt.setTimestamp(5, article.getReg_date());//웹에서 계산해서 저장
			//-------ref,re_step,re_level---------------------------------
			pstmt.setInt(6, ref);//pstmt.setInt(6,article,getRef());(X) 5
			pstmt.setInt(7, re_step);//0
			pstmt.setInt(8, re_level);//0
			//------------------------------------------
			pstmt.setString(9, article.getContent());//글내용
			pstmt.setString(10, article.getIp());//request.getRemoteAddr();//jsp
			int insert=pstmt.executeUpdate();
			System.out.println("게시판의 글쓰기 성공유무(insert)=>"+insert);
		}catch(Exception e) {
			System.out.println("insertArticle()메서드 에러유발=>"+e);
		}finally {
			pool.freeConnection(con, pstmt,rs);
		}//finally
	}
	
	//글상세보기->list.jsp
	// <a href="content.jsp?num=3&pageNum=1">게시판이란?</a>
	//형식) select * from board where num=3;
	//형식2) update board set readcount=readcount+1 where num=3
	public BoardDTO getArticle(int num) {
		BoardDTO article=null;
		
		try {
			con=pool.getConnection();
			sql="update board set readcount=readcount+1 where num=?";
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, num);
			int update=pstmt.executeUpdate();
			System.out.println("조회수 증가유무(update)=>"+update);
			
			sql="select * from board where num=?";
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, num);
			rs=pstmt.executeQuery();
			
			if(rs.next()) {//레코드가 존재한다면
				article=makeArticleFromResult();
				/*
			    article=new BoardDTO();//MemberDTO mem=new MemberDTO()
				article.setNum(rs.getInt("num"));
				article.setWriter(rs.getString("writer"));
				article.setEmail(rs.getString("email"));
				article.setSubject(rs.getString("subject"));
				article.setPasswd(rs.getString("passwd"));
				article.setReg_date(rs.getTimestamp("reg_date"));//오늘날짜->코딩 now()
				article.setReadcount(rs.getInt("readcount"));//default->0
				article.setRef(rs.getInt("ref"));//그룹번호->신규글과 답변글 묶어주는 번호
				article.setRe_step(rs.getInt("re_step"));//답변글의 순서
				article.setRe_level(rs.getInt("re_level"));//들여쓰기(답변의 깊이)
				article.setContent(rs.getString("content"));//글내용
				article.setIp(rs.getString("ip"));
				*/
			}
		}catch(Exception e) {
			System.out.println("getArticle()메서드 에러유발"+e);
		}finally {
			pool.freeConnection(con, pstmt,rs);
		}
		return article;
	}
	//--중복된 레코드 한개를 담을 수 있는 메서드를 따로 만들어서 처리-------
	private BoardDTO makeArticleFromResult() throws Exception{
		BoardDTO article=new BoardDTO();
		article.setNum(rs.getInt("num"));
		article.setWriter(rs.getString("writer"));
		article.setEmail(rs.getString("email"));
		article.setSubject(rs.getString("subject"));
		article.setPasswd(rs.getString("passwd"));
		article.setReg_date(rs.getTimestamp("reg_date"));//오늘날짜->코딩 now()
		article.setReadcount(rs.getInt("readcount"));//default->0
		article.setRef(rs.getInt("ref"));//그룹번호->신규글과 답변글 묶어주는 번호
		article.setRe_step(rs.getInt("re_step"));//답변글의 순서
		article.setRe_level(rs.getInt("re_level"));//들여쓰기(답변의 깊이)
		article.setContent(rs.getString("content"));//글내용
		article.setIp(rs.getString("ip"));
		return article;
	}
	
	//------------------------------------------------
	//게시판의 글수정하기
	//select * from board where num=? ->조회수를 증가X
	
	public BoardDTO updateGetArticle(int num) { //updateForm.jsp에서 출력
         BoardDTO article=null;
		try {
			con=pool.getConnection();
			sql="select * from board where num=?";
			pstmt=con.prepareStatement(sql);
			pstmt.setInt(1, num);
			rs=pstmt.executeQuery();
			
			if(rs.next()) {//레코드가 존재한다면
				article=makeArticleFromResult();
			}
		}catch(Exception e) {
			System.out.println("updateGetArticle()메서드 에러유발"+e);
		}finally {
			pool.freeConnection(con, pstmt,rs);
		}
		return article;
	}
	
	//글 수정시켜주는 메서드->insertArticle와 거의 동일=>암호를 물어본다.
	
	public int updateArticle(BoardDTO article) {
		 String dbpasswd=null;//db에서 찾은 암호를 저장
		 int x=-1;//게시물의 수정성공유무
		 
		 try {
			 con=pool.getConnection();
			 sql="select passwd from board where num=?";
			 pstmt=con.prepareStatement(sql);
			 pstmt.setInt(1, article.getNum());//매개변수를 다른 메서드의 실행결과를 받을 수있다
		     rs=pstmt.executeQuery();
		     if(rs.next()) {
		    	 dbpasswd=rs.getString("passwd");
		    	 System.out.println("dbpasswd=>"+dbpasswd);//확인된 뒤에는 삭제할것.
		    	 //db상의 암호=웹상에 입력한 암호가 맞다면
		    	 if(dbpasswd.equals(article.getPasswd())) {
		    		 sql="update board set writer=?,email=?,subject=?,passwd=?,";
		    		 sql+=" content=?  where num=?";
		    		 pstmt=con.prepareStatement(sql);
		    		 pstmt.setString(1, article.getWriter());//작성자
		    		 pstmt.setString(2, article.getEmail());
		    		 pstmt.setString(3, article.getSubject());
		    		 pstmt.setString(4, article.getPasswd());
		    		 pstmt.setString(5, article.getContent());
		    		 pstmt.setInt(6, article.getNum());
		    		 
		    		 int update=pstmt.executeUpdate();
		    		 System.out.println("게시판의 글수정 성공유무(update)="+update);//1성공
		    		 x=1;
		    	 }else {
		    		 x=0;//수정 실패
		    	 }
		     }//if(rs.next())->x=-1;
		 }catch(Exception e) {
			 System.out.println("updateArticle()메서드 에러유발=>"+e);
		 }finally {
			pool.freeConnection(con, pstmt, rs); //암호를 찾기 때문에
		 }
		 return x;
	}
	
	//글 삭제시켜주는 메서드->회원탈퇴(삭제)=>암호를 물어본다.=>deleteArticle
	public int deleteArticle(int num,String passwd) {
		
		 String dbpasswd=null;//db에서 찾은 암호를 저장
		 int x=-1;//게시물의 삭제성공유무
		 
		 try {
			 con=pool.getConnection();
			 sql="select passwd from board where num=?";
			 pstmt=con.prepareStatement(sql);
			 pstmt.setInt(1, num);
		     rs=pstmt.executeQuery();
		     
		     if(rs.next()) {
		    	 dbpasswd=rs.getString("passwd");
		    	 System.out.println("dbpasswd=>"+dbpasswd);//확인된 뒤에는 삭제할것.
		    	 //db상의 암호=웹상에 입력한 암호가 맞다면
		    	 if(dbpasswd.equals(passwd)) {
		    		 sql="delete from board where num=?";
		    		
		    		 pstmt=con.prepareStatement(sql);
		    		 pstmt.setInt(1, num);
		    		 int delete=pstmt.executeUpdate();
		    		 System.out.println("게시판의 글삭제 성공유무(delete)="+delete);//1성공
		    		 x=1;
		    	 }else {
		    		 x=0;//삭제 실패
		    	 }
		     }//if(rs.next())->x=-1;
		 }catch(Exception e) {
			 System.out.println("deleteArticle()메서드 에러유발=>"+e);
		 }finally {
			pool.freeConnection(con, pstmt, rs); //암호를 찾기 때문에
		 }
		 return x;
	}
}









