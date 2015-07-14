<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

    <!-- Bootstrap -->
    <link href="${pageContext.request.contextPath}/html/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
	<div id="game_window">

		<h1> Book </h1>

		<div class="method">
			<h3>Book(String name, int pages)</h3>
			<p>2 * Literal Assignments</p>
			<p>2 * Constructor Values</p>
		</div>

		<div class="method">
			<h3>openBook()</h3>
			<p>1 * Conditional</p>
			<p>1 * Literal Assignment</p>
			<p>Incoming Negate Conditionals Mutator</p>
		</div>

		<div class="method">
			<h3>closeBook()</h3>
			<p>1 * Conditional</p>
			<p>1 * Literal Assignment</p>
			<p>Incoming Remove Conditionals Mutator</p>
		</div>

		<div class="method">
			<h3>turnPage()</h3>
			<p>1 * Conditional</p>
		</div>
	</div>

	<div id="player_input">
		<form action="/../../runmutationtests" method="post">
			<input type="hidden" name="user" value="1">
			<textarea name="test" rows="30" cols="100">
@Test
public void test() {

}
			</textarea>

			<br>

			Test is being applied to:

			<select name="method_3">
				<option value="na">~ Select a Method to Defend ~</option>
				<option value="method_a">Book(String name, int pages)</option>
				<option value="method_b">openBook()</option>
				<option value="method_c">closeBook()</option>
			</select>

			<input type="submit" value="Defend!">

		</form>
	</div>

	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="js/bootstrap.min.js"></script>
</body>
</html>