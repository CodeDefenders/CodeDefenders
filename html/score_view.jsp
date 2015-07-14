<html>

<head>
	<title>Score Window</title>
</head>

<body>
	<%
	switch((Integer)request.getAttribute("result")) {
		case 0 :
			out.println("Attacker has won");
			break;
		case 1 :
			out.println("Defender has won");
			break;
		case 2 :
			out.println("It was a draw");
			break;
	}%>
</body>
</html>