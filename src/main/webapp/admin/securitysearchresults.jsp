<%--

    Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
    This software is published under the GPL GNU General Public License.
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    This software was written for the
    Department of Family Medicine
    McMaster University
    Hamilton
    Ontario, Canada

--%>

<%@ taglib uri="/WEB-INF/security.tld" prefix="security"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ page import="java.sql.*, java.util.*, oscar.*" buffer="none" errorPage="errorpage.jsp"%>
	
<%@ page import="java.util.*" %>
<%@ page import="org.oscarehr.util.SpringUtils" %>
<%@ page import="org.oscarehr.common.model.Security" %>
<%@ page import="org.oscarehr.common.dao.SecurityDao" %>


<%
	SecurityDao securityDao = SpringUtils.getBean(SecurityDao.class);
%>
	
	
	
<jsp:useBean id="apptMainBean" class="oscar.AppointmentMainBean" scope="session" />

<html:html locale="true">
<head>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/global.js"></script>
<title><bean:message key="admin.securitysearchresults.title" /></title>
<c:set var="ctx" value="${pageContext.request.contextPath}"
	scope="request" />
<link rel="stylesheet" href="../web.css" />
<script LANGUAGE="JavaScript">
    <!--
		function setfocus() {
		  document.searchprovider.keyword.focus();
		  document.searchprovider.keyword.select();
		}

    function onsub() {
      if(document.searchprovider.keyword.value=="") {
        alert('<bean:message key="global.msgInputKeyword"/>');
        return false;
      } else return true;
      // check input data in the future 
    }

    function encryptPIN(){
	   window.open("<c:out value="${ctx}"/>/admin/encryptPIN.jsp","_blank","width=100,height=100");
    }
    //-->
    </script>
</head>

<%
	if(session.getAttribute("userrole") == null )  response.sendRedirect("../logout.jsp");
    String roleName$ = (String)session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    
    boolean isSiteAccessPrivacy=false;
%>

<security:oscarSec objectName="_site_access_privacy" roleName="<%=roleName$%>" rights="r" reverse="false">
	<%
		isSiteAccessPrivacy=true;
	%>
</security:oscarSec>

<body background="../images/gray_bg.jpg" bgproperties="fixed"
	onLoad="setfocus()" topmargin="0" leftmargin="0" rightmargin="0">
<center>
<table border="0" cellspacing="0" cellpadding="0" width="100%">
	<tr bgcolor="#486ebd">
		<th align="CENTER"><font face="Helvetica" color="#FFFFFF"><bean:message
			key="admin.securitysearchresults.description" /></font></th>
	</tr>
</table>

<%--@ include file="zprovidertitlesearch.htm" --%>
<table cellspacing="0" cellpadding="0" width="100%" border="0"
	BGCOLOR="#C4D9E7">

	<form method="post" action="admincontrol.jsp" name="searchprovider">
	<tr valign="top">
		<td rowspan="2" align="right" valign="middle"><font
			face="Verdana" color="#0000FF"><b><i><bean:message
			key="admin.search.formSearchCriteria" /></i></b></font></td>
		<td nowrap><font size="1" face="Verdana" color="#0000FF">
		<input type="radio" name="search_mode" value="search_username"><bean:message
			key="admin.securityrecord.formUserName" /></font></td>
		<td nowrap><font size="1" face="Verdana" color="#0000FF">
		<input type="radio" checked name="search_mode"
			value="search_providerno"><bean:message
			key="admin.securityrecord.formProviderNo" /></font></td>
		<td valign="middle" rowspan="2" ALIGN="left"><input type="text"
			NAME="keyword" SIZE="17" MAXLENGTH="100"> <INPUT
			TYPE="hidden" NAME="orderby" VALUE="user_name"> 
			<%
 				if (isSiteAccessPrivacy)  {
 			%>	 
				<INPUT	TYPE="hidden" NAME="dboperation" VALUE="site_security_search_titlename">
			<%
				}
					  else	  {
			%>
				<INPUT	TYPE="hidden" NAME="dboperation" VALUE="security_search_titlename">
			 <%
			 	}
			 %>				
		<INPUT TYPE="hidden" NAME="limit1" VALUE="0"> <INPUT
			TYPE="hidden" NAME="limit2" VALUE="10"> <INPUT TYPE="hidden"
			NAME="displaymode" VALUE="Security_Search"> <INPUT
			TYPE="SUBMIT" NAME="button"
			VALUE='<bean:message key="admin.search.btnSubmit"/>' SIZE="17"></td>
	</tr>
	<tr>
		<td nowrap><font size="1" face="Verdana" color="#0000FF"><bean:message
			key="admin.securitysearchresults.reserved" /></font></td>
		<td nowrap><font size="1" face="Verdana" color="#0000FF">
		</font></td>
	</tr>
	</form>
</table>

<table width="100%" border="0">
	<tr>
		<td align="left"><i><bean:message key="admin.search.keywords" /></i>:
		<%=request.getParameter("keyword")%> &nbsp; <%
 	if(apptMainBean.isPINEncrypted()==false){
 %> <input type="button" name="encryptPIN" value="Encrypt PIN"
			onclick="encryptPIN()"> <%
 	}
 %>
		</td>
	</tr>
</table>
<CENTER>
<table width="100%" cellspacing="0" cellpadding="2" border="1"
	bgcolor="#ffffff">
	<tr bgcolor="#339999">
		<TH align="center" width="20%"><b><bean:message
			key="admin.securityrecord.formUserName" /></b></TH>
		<TH align="center" width="40%"><b><bean:message
			key="admin.securityrecord.formPassword" /></b></TH>
		<TH align="center" width="20%"><b><bean:message
			key="admin.securityrecord.formProviderNo" /></b></TH>
		<TH align="center" width="20%"><b><bean:message
			key="admin.securityrecord.formPIN" /></b></TH>
	</tr>

<%
	List<org.oscarehr.common.model.Security> securityList = securityDao.findAllOrderBy("user_name");
	
	//if action is good, then give me the result
	String dboperation = request.getParameter("dboperation");
	String searchMode = request.getParameter("search_mode");
	String keyword=request.getParameter("keyword").trim()+"%";
	
	// if search mode is provider_no 
	if(searchMode.equals("search_providerno"))
		securityList = securityDao.findByLikeProviderNo(keyword);
	
	// if search mode is user_name
	if(searchMode.equals("search_username"))
		securityList = securityDao.findByLikeUserName(keyword);
	
	boolean toggleLine = false;

	for(Security securityRecord : securityList) {
		
		toggleLine = !toggleLine;
%>

	<tr bgcolor="<%=toggleLine?"ivory":"white"%>">

		<td><a
			href='admincontrol.jsp?keyword=<%=securityRecord.getId()%>&displaymode=Security_Update&dboperation=Security_search_detail'><%= securityRecord.getUserName() %></a></td>
		<td nowrap>*********</td>
		<td align="center"><%= securityRecord.getProviderNo() %></td>
		<td align="center">****</td>
	</tr>
	<%
    }
%>

</table>
<br>
<%
  int nLastPage=0,nNextPage=0;
  String strLimit1=request.getParameter("limit1");
  String strLimit2=request.getParameter("limit2");
  
  nNextPage=Integer.parseInt(strLimit2)+Integer.parseInt(strLimit1);
  nLastPage=Integer.parseInt(strLimit1)-Integer.parseInt(strLimit2);
  if(nLastPage>=0) {
%> <a
	href="admincontrol.jsp?keyword=<%=request.getParameter("keyword")%>&search_mode=<%=request.getParameter("search_mode")%>&displaymode=<%=request.getParameter("displaymode")%>&dboperation=<%=request.getParameter("dboperation")%>&orderby=<%=request.getParameter("orderby")%>&limit1=<%=nLastPage%>&limit2=<%=strLimit2%>"><bean:message
	key="admin.securitysearchresults.btnLastPage" /></a> | <%
  }
  if(true) { //nItems==Integer.parseInt(strLimit2)) {
%> <a
	href="admincontrol.jsp?keyword=<%=request.getParameter("keyword")%>&search_mode=<%=request.getParameter("search_mode")%>&displaymode=<%=request.getParameter("displaymode")%>&dboperation=<%=request.getParameter("dboperation")%>&orderby=<%=request.getParameter("orderby")%>&limit1=<%=nNextPage%>&limit2=<%=strLimit2%>"><bean:message
	key="admin.securitysearchresults.btnNextPage" /></a> <%
}
%>
<p><bean:message key="admin.securitysearchresults.msgClickForDetail" /></p>
</center>
<%@ include file="footerhtm.jsp"%></center>
</body>
</html:html>
