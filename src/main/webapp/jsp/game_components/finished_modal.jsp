<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%--
    Shows a modal, which indicates that a game is finished.
--%>

<jsp:useBean id="finishedModal" class="org.codedefenders.beans.game.FinishedModalBean" scope="request"/>

<t:modal title="Game Over" id="finishedModal">
    <jsp:attribute name="content">${finishedModal.message}</jsp:attribute>
</t:modal>

<script type="module">
    import {Modal} from '${url.forPath("/js/bootstrap.mjs")}';

    new Modal('#finishedModal').show();
</script>
