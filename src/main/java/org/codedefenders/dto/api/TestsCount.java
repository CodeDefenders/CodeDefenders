package org.codedefenders.dto.api;

public class TestsCount {
    private Integer killing;
    private Integer nonkilling;
    private Integer total;

    public TestsCount(Integer killing, Integer nonkilling) {
        this.killing = killing;
        this.nonkilling = nonkilling;
        total = killing + nonkilling;
    }
}
