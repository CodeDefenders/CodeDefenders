package org.codedefenders;

import org.codedefenders.util.MutantUtils;
import org.junit.Assert;
import org.junit.Test;

public class MutantUtilsTest {

    @Test
    public void testCleanUpMutatedCodeWithManySingleEmptyLinesInsertion(){
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = String.join("\n", 
                "public class Test{", 
                "public void test(){", 
                "String s = \"\"; //comment", 
                "int foo; // comment", 
                "int foo1;" , 
                "if(x > 0) \n\t return x; //x is positive" , 
                "}" , 
                "}");
        // Same as original but few emply blank lines
        String mutatedCode = String.join("\n",
                "public class Test{", 
                "public void test(){", 
                "String s = \"\"; //comment", 
                "", // Blank line
                "int foo; // comment",
                "", // Blank line
                "int foo1;" , 
                "if(x > 0) \n\t return x; //x is positive" , 
                "}" , 
                "}");
        
        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);
        
        Assert.assertEquals( originalCode, cleanedCode );
        
    }
    
    @Test
    public void testCleanUpMutatedCodeWithManySingleBlankLinesInsertion(){
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = String.join("\n", 
                "public class Test{", 
                "public void test(){", 
                "String s = \"\"; //comment", 
                "int foo; // comment", 
                "int foo1;" , 
                "if(x > 0) \n\t return x; //x is positive" , 
                "}" , 
                "}");
        // Same as original but few emply blank lines
        String mutatedCode = String.join("\n",
                "public class Test{", 
                "public void test(){", 
                "String s = \"\"; //comment", 
                "    ", // Blank line
                "int foo; // comment",
                "\t\t   ", // Blank line
                "int foo1;" , 
                "if(x > 0) \n\t return x; //x is positive" , 
                "}" , 
                "}");
        
        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);
        
        Assert.assertEquals( originalCode, cleanedCode );
        
    }
    
    @Test
    public void testCleanUpMutatedCodeWithManyMultiBlankLinesInsertion(){
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = String.join("\n", 
                "public class Test{", 
                "public void test(){", 
                "String s = \"\"; //comment", 
                "int foo; // comment", 
                "int foo1;" , 
                "if(x > 0) \n\t return x; //x is positive" , 
                "}" , 
                "}");
        // Same as original but few emply blank lines
        String mutatedCode = String.join("\n",
                "public class Test{", 
                "public void test(){", 
                "String s = \"\"; //comment", 
                "", // Blank line
                "", // Blank line
                "", // Blank line
                "int foo; // comment",
                "", // Blank line
                "int foo1;" , 
                "if(x > 0) \n\t return x; //x is positive" , 
                "}" , 
                "}");
        
        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);
        
        Assert.assertEquals( originalCode, cleanedCode );
        
    }
    
    @Test
    public void testCleanUpMutatedCodeWithBlankLinesInBetween(){
        MutantUtils mutantUtils = new MutantUtils();
        String originalCode = String.join("\n", 
                "public class Complex {",
                "",
                "public double real, imag;",
                "/**",
                "* Constructor that defines the <code>real</code> and",
                "* <code>imaginary</code> parts of the number.",
                "* ",
                "* @param real The real part of the number.",
                "* @param imag The imaginary part of the number.",
                "*/",
                "public Complex(double real, double imag) {",
                "  this.real = real;",
                " this.imag = imag;",
                "}");
        
        String mutatedCode = String.join("\n",
                "public class Complex {",
                "",
                "public double real, imag;",
                "/**",
                "* Constructor that defines the <code>real</code> and",
                "* <code>imaginary</code> parts of the number.",
                "* ",
                "* @param real The real part of the number.",
                "* @param imag The imaginary part of the number.",
                "*/",
                "public Complex(double real, double imag) {",
                "  this.real = ",
                "",
                "",
                "",
                "real;",
                " this.imag = imag;",
                "}");
        
        String expectedCode = String.join("\n",
                "public class Complex {",
                "",
                "public double real, imag;",
                "/**",
                "* Constructor that defines the <code>real</code> and",
                "* <code>imaginary</code> parts of the number.",
                "* ",
                "* @param real The real part of the number.",
                "* @param imag The imaginary part of the number.",
                "*/",
                "public Complex(double real, double imag) {",
                "  this.real = ",
                // The following three lines are removed
//                "",
//                "",
//                "",
                // But this line is yet into a new one
                "real;",
                " this.imag = imag;",
                "}");
        
        String cleanedCode = mutantUtils.cleanUpMutatedCode(originalCode, mutatedCode);
    
        // Now
        Assert.assertEquals( expectedCode, cleanedCode );
    }

}
