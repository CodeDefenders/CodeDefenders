package gammut;

// Loading required libraries
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
 
public class DatabaseAccess {

   static final String JDBC_DRIVER="com.mysql.jdbc.Driver";  
   static final String DB_URL="jdbc:mysql://localhost/gammut";

   //  Database credentials
   static final String USER = "root";
   static final String PASS = "donotstandatmygraveandweep";
   
   public static String addSlashes(String s) {
   		return s.replaceAll("\\\\", "\\\\\\\\");
   }
} 