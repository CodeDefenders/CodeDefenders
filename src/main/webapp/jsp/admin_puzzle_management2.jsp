<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
    int[] arr = {0, 1, 2, 3};
    int[][] arrr = {{0,1}, {0}, {0}, {0}};
    pageContext.setAttribute("arr", arr);
    pageContext.setAttribute("arrr", arrr);
%>

<p:main_page title="Puzzle Management">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/puzzle_management.css")}" rel="stylesheet">
    </jsp:attribute>
    <jsp:body>
        <div class="container">
            <t:admin_navigation activePage="adminPuzzles"/>
            <div id="puzzle-management" class="d-flex flex-column gap-5">
                <c:forEach items="${arr}" var="x">
                    <c:if test="${x==0}">
                        <div class="chapter chapter-unassigned">
                    </c:if>
                    <c:if test="${x==1}">
                        <div id="chapters" class="chapters d-flex flex-column gap-4">
                            <div class="chapter chapter-regular">
                    </c:if>
                    <c:if test="${x==2}">
                        <div class="chapter chapter-regular">
                    </c:if>
                    <c:if test="${x==3}">
                        <div class="chapter chapter-archived">
                    </c:if>
                        <div class="chapter__header d-flex align-items-center justify-content-between mb-3">
                            <div class="chapter__info d-flex gap-3 flex-shrink-1 align-items-baseline">
                            <c:if test="${x==0}">
                                <span class="chapter__title badge bg-secondary fs-6">Unassigned Puzzles</span>
                            </c:if>
                            <c:if test="${x==1}">
                                    <span class="chapter__title badge bg-secondary fs-6">Beginner</span>
                                    <div class="chapter__description">
                                        Puzzles of easy difficulty.
                                        Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.
                                    </div>
                            </c:if>
                            <c:if test="${x==2}">
                                    <span class="chapter__title badge bg-secondary fs-6">Advanced</span>
                                    <div class="chapter__description">
                                        Puzzles of medium difficulty.
                                    </div>
                            </c:if>
                            <c:if test="${x==3}">
                                <span class="chapter__title badge bg-danger fs-6">Archived Puzzles</span>
                            </c:if>

                            </div>

                            <c:if test="${x>0 && x<3}">
                                <div class="chapter__controls d-flex gap-1 align-items-center">
                                    <div class="chapter__handle me-3"></div>
                                    <button class="btn btn-xs btn-primary"
                                            data-bs-toggle="tooltip"
                                            title="Edit">
                                        <i class="fa fa-edit"></i>
                                    </button>
                                    <%--
                                    <button class="btn btn-xs btn-primary ms-1 move-up"
                                            data-bs-toggle="tooltip"
                                            title="Move Up">
                                        <i class="fa fa-arrow-up"></i>
                                    </button>
                                    <button class="btn btn-xs btn-primary me-1 move-down"
                                            data-bs-toggle="tooltip"
                                            title="Move Down">
                                        <i class="fa fa-arrow-down"></i>
                                    </button>
                                    --%>
                                    <button class="btn btn-xs btn-danger"
                                            data-bs-toggle="tooltip"
                                            title="Delete">
                                        <i class="fa fa-trash"></i>
                                    </button>
                                </div>
                            </c:if>
                        </div>

                        <div class="puzzles">

                            <c:forEach items="${arrr[x]}" var="y">

                                <a class="puzzle puzzle-defender">
                                    <img class="puzzle__watermark" alt="DEFENDER" src="/codedefenders/images/achievements/codedefenders_achievements_2_lvl_0.png">
                                    <div class="puzzle__container">
                                        <div class="puzzle__info">
                                            <div class="puzzle__title">Puzzle 1</div>
                                            <div class="puzzle__description">
                                                Write a test that detects the mutant
                                                Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.
                                            </div>
                                        </div>
                                        <div class="puzzle__tags">
                                            <span class="badge puzzle__tag">#170</span>
                                            <span class="badge puzzle__tag">302 games</span>
                                        </div>
                                    </div>
                                    <div class="puzzle__controls">
                                        <button class="btn btn-xs btn-primary">
                                            <i class="fa fa-edit"></i>
                                        </button>
                                        <button class="btn btn-xs btn-danger">
                                            <i class="fa fa-archive"></i>
                                        </button>
                                        <button class="btn btn-xs btn-danger">
                                            <i class="fa fa-trash"></i>
                                        </button>
                                    </div>
                                </a>



                                <a class="puzzle puzzle-defender">
                                    <img class="puzzle__watermark" alt="DEFENDER" src="/codedefenders/images/achievements/codedefenders_achievements_2_lvl_0.png">
                                    <div class="puzzle__container">
                                        <div class="puzzle__info">
                                            <div class="puzzle__title">Puzzle 2</div>
                                            <div class="puzzle__description">Write a test that detects the mutant</div>
                                        </div>
                                        <div class="puzzle__tags">
                                            <span class="badge puzzle__tag">#210</span>
                                            <span class="badge puzzle__tag">156 games</span>
                                        </div>
                                    </div>
                                    <div class="puzzle__controls">
                                        <button class="btn btn-xs btn-primary">
                                            <i class="fa fa-edit"></i>
                                        </button>
                                        <button class="btn btn-xs btn-danger">
                                            <i class="fa fa-archive"></i>
                                        </button>
                                        <button class="btn btn-xs btn-danger">
                                            <i class="fa fa-trash"></i>
                                        </button>
                                    </div>
                                </a>



                                <a class="puzzle puzzle-attacker">
                                    <img class="puzzle__watermark" alt="ATTACKER" src="/codedefenders/images/achievements/codedefenders_achievements_1_lvl_0.png">
                                    <div class="puzzle__container">
                                        <div class="puzzle__info">
                                            <div class="puzzle__title">Puzzle 3</div>
                                            <div class="puzzle__description">Write a mutant which evades all the tests.</div>
                                        </div>
                                        <div class="puzzle__tags">
                                            <span class="badge puzzle__tag">#312</span>
                                            <span class="badge puzzle__tag">87 games</span>
                                        </div>
                                    </div>
                                    <div class="puzzle__controls">
                                        <button class="btn btn-xs btn-primary">
                                            <i class="fa fa-edit"></i>
                                        </button>
                                        <button class="btn btn-xs btn-danger">
                                            <i class="fa fa-archive"></i>
                                        </button>
                                        <button class="btn btn-xs btn-danger">
                                            <i class="fa fa-trash"></i>
                                        </button>
                                    </div>
                                </a>

                            </c:forEach>

                        </div>
                    </div>
                    <c:if test="${x==2}">
                        </div>
                    </c:if>
                </c:forEach>
            </div>
            </div>
        </div>

        <script type="module">
            import {Sortable} from '${url.forPath('/js/sortablejs.mjs')}'
            import {PuzzleAPI, DeferredPromise} from '${url.forPath("/js/codedefenders_main.mjs")}';

            for (const list of document.getElementsByClassName('puzzles')) {
                Sortable.create(list, {
                    animation: 200,
                    group: 'puzzles'
                })
            }

            for (const list of document.getElementsByClassName('chapters')) {
                Sortable.create(list, {
                    animation: 200,
                    group: 'chapters',
                    handle: '.chapter__handle'
                })
            }


            const puzzleData = await PuzzleAPI.fetchPuzzleData();

            const chapters = puzzleData.puzzleChapters;
            const puzzles = puzzleData.puzzleChapters;
        </script>
    </jsp:body>
</p:main_page>


