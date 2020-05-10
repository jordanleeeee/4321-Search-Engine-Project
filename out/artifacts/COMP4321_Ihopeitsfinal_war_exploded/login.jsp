<%--
  Created by IntelliJ IDEA.
  User: tsuiw
  Date: 5/5/2020
  Time: 10:13 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Title</title>
  </head>
  <body>
  <%String keyword =(String)request.getAttribute("keyword");
    out.print("your name"+ keyword);%>
  </body>
</html>
