<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="java.sql.Timestamp,yoo.board.*" %>
<%
     //한글처리
     request.setCharacterEncoding("utf-8");
     //BoardDTO->Setter Method(5)+hidden (4)
     //BoardDAO 객체 필요
%>
<jsp:useBean id="article"  class="yoo.board.BoardDTO" />
<jsp:setProperty name="article" property="*" />
<%
  //BoardDAO객체->insertArticle(article)=>9개-->10개->12개(readcount(0),num)
  Timestamp temp=new Timestamp(System.currentTimeMillis());//컴퓨터의 날짜,시간
  article.setReg_date(temp);
  article.setIp(request.getRemoteAddr());//원격ip주소 저장
  BoardDAO dbPro=new BoardDAO();
  dbPro.insertArticle(article);
  response.sendRedirect("list.jsp");//입력한후 다시 DB접속->화면에 출력
%>





