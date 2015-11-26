public class Book {
	
	public String bookName;
	private int noPages;
	private int currentPage;
	
	public Book(String name, int pages) {
		bookName = name;
		noPages = pages;
		currentPage = 0;
	}
	
	public boolean openBook() {
		if (currentPage == 0) {currentPage = 1; return true;}
		else {return false;}
	}
	
	public boolean closeBook() {
		if (currentPage != 0) {currentPage = 0; return true;}
		else {return false;}
	}
	
	public boolean turnPage() {
		if (currentPage < noPages) {currentPage++; return true;}
		else {return closeBook();}
	}
	
	public int getNoOfPages() {
		return noPages;
	}
	
	public boolean isOpen() {
		if (currentPage != 0) {return true;}
		else {return false;}
	}
}
