package org.codedefenders.dto.api;

public class MutantsCount {
    private Integer alive;
    private Integer killed;
    private Integer equivalent;
    private Integer total;

    public MutantsCount(Integer alive, Integer killed, Integer equivalent) {
        this.alive = alive;
        this.killed = killed;
        this.equivalent = equivalent;
        total = alive + killed + equivalent;
    }
}
