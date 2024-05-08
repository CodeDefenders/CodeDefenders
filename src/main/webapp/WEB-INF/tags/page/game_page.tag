<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ tag import="org.codedefenders.game.AbstractGame" %>
<%@ tag import="org.codedefenders.game.Role" %>
<%@ tag import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ tag import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="pageInfo" type="org.codedefenders.beans.page.PageInfoBean"--%>
<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>


<%@ attribute name="additionalImports" required="false" fragment="true" %>


<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    AbstractGame game = (AbstractGame) request.getAttribute("game");

    Role role = null;
    if (game instanceof MeleeGame) {
        role = ((MeleeGame) game).getRole(login.getUserId());
    } else if (game instanceof MultiplayerGame) {
        role = ((MultiplayerGame) game).getRole(login.getUserId());
    }

    String roleStr = role.getFormattedString();
    if (roleStr == null) {
        roleStr = "no role";
    }

    request.setAttribute("title", "Game " + game.getId() + " (" + roleStr + ")");
%>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
        <jsp:invoke fragment="additionalImports"/>
    </jsp:attribute>

    <jsp:body>
        <div id="game-container" class="container-fluid">
            <t:game_header/>
            <jsp:doBody/>
        </div>
        <t:game_js_init/> <%-- TODO: move this before the container? --%>
    </jsp:body>
</p:main_page>
