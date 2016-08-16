<hr>

<div id="research" class="paper-list">
    <br><br><br>
    <h1>Research</h1>
    <p></p>
    <div class="row-fluid">
        <ul class="papercite_bibliography">
            <li>
                <a href="papers/Mutation16_CodeDefenders.pdf" title='Download PDF'>
                    <img src='images/pdf.png' alt="[PDF]"/>
                </a>
                Jos&eacute; Miguel Rojas and Gordon Fraser.
                <span class="paper_title">&#8220;Code Defenders: A Mutation Testing Game,&#8221;</span>
                In <span class="paper_venue">Proc. of The 11th International Workshop on Mutation Analysis</span>,  2016.
                <a href="javascript:void(0)" onclick="javascript:toggleBibtex(document.getElementById('mutation16_bibtex'));">[Bibtex]</a>
                <div id="mutation16_bibtex" class="bibtex_hide" >
<pre class="tex"><code>@inproceedings{Mutation16_CodeDefenders,
	author = {Jos{\'e} Miguel Rojas and Gordon Fraser},
	title = {Code Defenders: A Mutation Testing Game},
	booktitle = {Proc. of The 11th International Workshop on Mutation Analysis},
	year = {2016},
	publisher = {IEEE},
	note = {To appear}
}</code></pre>
                </div>
            </li>
            <li>
                <a href="papers/ECSEE16_MutationEducation.pdf" title='Download PDF'>
                    <img src='images/pdf.png' alt="[PDF]"/>
                </a>
                Jos&eacute; Miguel Rojas and Gordon Fraser.
                <span class="paper_title">&#8220;Teaching Mutation Testing using Gamification,&#8221;</span>
                In <span class="paper_venue">Proc. of The European Conference of Software Engineering Education</span>,  2016.
                <a href="javascript:void(0)" onclick="javascript:toggleBibtex(document.getElementById('ecsee16_bibtex'));">[Bibtex]</a>
                <div id="ecsee16_bibtex" class="bibtex_hide" >
<pre class="tex"><code>@inproceedings{ECSEE16_MutationEducation,
	author = {Jos{\'e} Miguel Rojas and Gordon Fraser},
	title = {Teaching Mutation Testing using Gamification},
	booktitle = {Proc. of The European Conference of Software Engineering Education (ECSEE)},
	year = {2016},
	publisher = {Shaker Publishing},
	note = {To appear}
}</code></pre>
                </div>
            </li>
            <li>


                <a href="papers/PPIG16_TeachingTesting.pdf" title='Download PDF'>
                    <img src='images/pdf.png' alt="[PDF]"/>
                </a>
                Jos&eacute; Miguel Rojas and Gordon Fraser.
                <span class="paper_title">&#8220;Teaching Software Testing with a Mutation Testing Game,&#8221;</span>
                In <span class="paper_venue">Proc. of the Annual Workshop of the Psychology of Programming Interest Group</span>,  2016.
                <a href="javascript:void(0)" onclick="javascript:toggleBibtex(document.getElementById('ppig16_bibtex'));">[Bibtex]</a>
                <div id="ppig16_bibtex" class="bibtex_hide" >
<pre class="tex"><code>@inproceedings{PPIG16_TeachingTesting,
	author = {Jos{\'e} Miguel Rojas and Gordon Fraser},
	title = {Teaching Software Testing with a Mutation Testing Game},
	booktitle = {Psychology of Programming Interest Group 2016 (PPIG)},
	year = {2016}
}</code></pre>
                </div>
            </li>
        </ul>
    </div>
</div>

<hr>

<div id="contact" class="container">
    <form  action="sendEmail" method="post" class="form-signin">
        <input type="hidden" name="formType" value="login">
        <h1 class="form-signin-heading">Contact Us</h1>
        <label for="inputName" class="sr-only">Name</label>
        <input type="text" id="inputName" name="name" class="form-control" placeholder="Name" required>
        <label for="inputEmail" class="sr-only">Email</label>
        <input type="email" id="inputEmail" name="email" class="form-control" placeholder="Email" required>
        <label for="inputSubject" class="sr-only">Subject</label>
        <input type="text" id="inputSubject" name="subject" class="form-control" placeholder="Subject" required>
        <label for="inputMessage" class="sr-only">Message</label>
        <textarea id="inputMessage" name="message" class="form-control" placeholder="Message" rows="8" required></textarea>
        <button class="btn btn-lg btn-primary btn-block" type="submit">Send</button>
    </form>
    <%
        String result = (String)request.getSession().getAttribute("emailSent");
        request.getSession().removeAttribute("emailSent");
        if (result != null) {
    %>
    <div class="alert alert-info" id="messages-div">
        <p><%=result%></p>
    </div>
    <%
        }
    %>
</div>
</body>
<script>
    function toggleBibtex(div) {
        var className = div.getAttribute("class");
        if(className=="bibtex_hide") {
            div.className = "bibtex_show";
        }
        else{
            div.className = "bibtex_hide";
        }
    }
</script>
</html>
