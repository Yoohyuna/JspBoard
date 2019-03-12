<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="yoo.board.*" %>
<%
     //한글처리
     request.setCharacterEncoding("utf-8");
     //BoardDTO->Setter Method(5)+hidden (1)
     //BoardDAO 객체 필요
%>
<jsp:useBean id="article"  class="yoo.board.BoardDTO" />
<jsp:setProperty name="article" property="*" />
<%
  String pageNum=request.getParameter("pageNum");//BoardDTO의 멤버변수가 X
  BoardDAO dbPro=new BoardDAO();
  int check=dbPro.updateArticle(article);//암호찾고->웹상의 암호체크
  if(check==1){ //글수정이 성공했다면
  //response.sendRedirect("list.jsp");//입력한후 다시 DB접속->화면에 출력
  //http-equiv="Refresh" =>새로고침 옵션
  //content="초단위(몇초동안 멈춘후);url=이동할 페이지명
%>
<meta http-equiv="Refresh"  content="0;url=list.jsp?pageNum=<%=pageNum%>">
<% }else { %>
<script>
          alert("비밀번호가 맞지않습니다.\n 다시 비밀번호를 확인요망!")
          history.go(-1)
</script>
<% } %>







