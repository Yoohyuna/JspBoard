<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="yoo.board.*" %>
<%
  String pageNum=request.getParameter("pageNum");//BoardDTO의 멤버변수가 X
  String passwd=request.getParameter("passwd");
  int num=Integer.parseInt(request.getParameter("num"));
  System.out.println("num=>"+num+",passwd=>"+passwd+",pageNum=>"+pageNum);
  
  BoardDAO dbPro=new BoardDAO();
  int check=dbPro.deleteArticle(num,passwd);//암호찾고->웹상의 암호체크
  if(check==1){ //글삭제이 성공했다면
 
%>
<meta http-equiv="Refresh"  content="0;url=list.jsp?pageNum=<%=pageNum%>">
<% }else { %>
<script>
          alert("비밀번호가 맞지않습니다.\n 다시 비밀번호를 확인요망!")
          history.go(-1)
</script>
<% } %>







