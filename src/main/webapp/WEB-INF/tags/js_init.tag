<%@ tag pageEncoding="UTF-8" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<!-- Context path of server, so plain JS code (without JSP templating) can construct a correct url. -->
<script>
    const contextPath = "${url.forPath("/")}";
    const applicationURL = "${url.getAbsoluteURLForPath("/")}";
</script>

<!-- JS Init -->
<script type="module">
    import '${url.forPath("/js/codedefenders_init.mjs")}';
</script>
