<html>
<body>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="indexer.PageProperty" %>
<%@ page import="retriever.Retrieval" %>
<%@ page import="java.util.List" %>
<%

if(request.getParameter("txtname")!=null)
{
	out.println("The results are:<hr/>");
	String query = request.getParameter("txtname");


    Retrieval retrieval = new Retrieval(query);
	List<Integer> result = retrieval.getResult();
	if(result.size() > 0){
		out.println("<table>");
		for(int i = 0; i < result.size(); i++){
			Integer id = result.get(i);
			String url = PageProperty.getInstance().getUrl(id);
			String title = PageProperty.getInstance().getTitle(id);
		//	String size = PageProperty.getInstance().getSize(id);
			String mod_time = PageProperty.getInstance().getLastModificationTime(id);
			out.println(url + "<br>" + "<a href='" + url + "'>" + title + "</a><br>" + mod_time + "<p>");
		}
		out.println("</table>");
	}else{
		out.println("No match result");
	}
}
else
{
	out.println("You input nothing");
}

%>
</body>
</html>