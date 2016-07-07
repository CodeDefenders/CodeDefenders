<hr>

<div id="research" class="container">
    <br><br><br>
    <h1>Research</h1>
    <p></p>
    <div class="row-fluid">
        <ul class="papercite_bibliography">
            <li>
                <a href="papers/Mutation16_CodeDefenders.pdf" title='Download PDF'>
                    <img src='images/pdf.png' alt="[PDF]"/>
                </a>
                Jos&eacute; Miguel Rojas and G. Fraser
                &#8220;Code Defenders: A Mutation Testing Game,&#8221;
                In <span style="font-style: italic">Proc. of The 11th International Workshop on Mutation Analysis</span>,  2016.<br/>
                <a href="javascript:void(0)" onclick="javascript:var x=document.getElementById('mutation16_bibtex');x.style.display=x.style.display=='none'?'':'none'">[Bibtex]</a>
                <div style="display: none" id="mutation16_bibtex">
<pre style="text-align: left"><code class="tex bibtex">@inproceedings{Mutation16_CodeDefenders,
	author = {Jos{\'e} Miguel Rojas and Gordon Fraser},
	title = {Code Defenders: A Mutation Testing Game},
	booktitle = {Proc. of The 11th International Workshop on Mutation Analysis},
	year = {2016},
	publisher = {IEEE},
	note = {To appear}
}</code></pre>
                </div>
            </li>
        </ul>
    </div>
    <div class="row-fluid">
        <ul class="papercite_bibliography">
            <li>
                <a href="papers/ECSEE16_MutationEducation.pdf" title='Download PDF'>
                    <img src='images/pdf.png' alt="[PDF]"/>
                </a>
                Jos&eacute; Miguel Rojas and G. Fraser
                &#8220;Teaching Mutation Testing using Gamification,&#8221;
                In <span style="font-style: italic">Proc. of The European Conference of Software Engineering Education</span>,  2016.<br/>
                <a href="javascript:void(0)" onclick="javascript:var x=document.getElementById('ecsee16_bibtex');x.style.display=x.style.display=='none'?'':'none'">[Bibtex]</a>
                <div style="display: none" id="ecsee16_bibtex">
<pre style="text-align: left"><code class="tex bibtex">@inproceedings{ECSEE16_MutationEducation,
	author = {Jos{\'e} Miguel Rojas and Gordon Fraser},
	title = {Teaching Mutation Testing using Gamification},
	booktitle = {Proc. of The European Conference of Software Engineering Education (ECSEE)},
	year = {2016},
	publisher = {Shaker Publishing},
	note = {To appear}
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
</html>