package org.codedefenders.validation.code;

/**
 * This enumeration represents states and their
 * message during code validation.
 * <p>
 * Use {@link #get()} to retrieve the message as a {@link String}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
public enum ValidationMessage {
	// Generic error message.
	MUTANT_VALIDATION_FAILED("Invalid mutant. Your mutant does not comply with our rules."),

    MUTANT_VALIDATION_SUCCESS("Your mutant complies with our rules."),
    MUTANT_VALIDATION_LINES("Invalid mutant, sorry! Removing or adding lines is not allowed."),
    MUTANT_VALIDATION_MODIFIER("Invalid mutant, sorry! Changing modifiers such as 'static' or 'public' is not allowed."),
    MUTANT_VALIDATION_COMMENT("Invalid mutant, sorry! Adding or modifying comments is not allowed."),
    MUTANT_VALIDATION_LOGIC("Invalid mutant, sorry! Your mutant contains new logical operations"),
    MUTANT_VALIDATION_OPERATORS("Invalid mutant, sorry! Your mutant contains prohibited operations such as bitshifts, ternary operators, added comments or multiple statments per line."),
    MUTANT_VALIDATION_CALLS("Your mutant contains calls to System.*, Random.* or new control structures.\n\nShame on you!"),
    MUTANT_VALIDATION_IDENTICAL("Invalid mutant, sorry! Your mutant is identical to the CUT"),
    MUTANT_VALIDATION_METHOD_SIGNATURE("Invalid mutant, sorry! Your mutant changes one or more method signatures or field names or import statements"),

    // Probably a better label required
    MUTANT_MISSING_INTENTION("Invalid mutant. You must declare your intention."),
    
    MUTATION_CLASS_DECLARATION("Invalid mutation contains class declaration."),
    MUTATION_METHOD_DECLARATION("Invalid mutation contains method declaration."),
    MUTATION_SYSTEM_USE("Invalid mutation contains System uses"),
    MUTATION_FOR_EACH_STATEMENT("Invalid mutation contains a ForeachStmt statement"),
    MUTATION_IF_STATEMENT("Invalid mutation contains an IfStmt statement"),
    MUTATION_FOR_STATEMENT("Invalid mutation contains a ForStmt statement"),
    MUTATION_WHILE_STATEMENT("Invalid mutation contains a WhileStmt statement"),
    MUTATION_DO_STATEMENT("Invalid mutation contains a DoStmt statement"),
    MUTATION_SWITCH_STATEMENT("Invalid mutation contains a SwitchStmt statement"),
    MUTATION_SYSTEM_CALL("Invalid mutation contains a call to System.*"),
    MUTATION_SYSTEM_DECLARATION("Invalid mutation contains variable declaration using System.*");

	
    private String message;

    ValidationMessage(String message) {
        this.message = message;
    }

    public String get() {
        return message;
    }

    @Override
    public String toString() {
        return get();
    }
}
