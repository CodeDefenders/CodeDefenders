package org.codedefenders.dto.api;

public class DuelsCount {
    private Integer won;
    private Integer lost;
    private Integer ongoing;

    public DuelsCount(Integer won, Integer lost, Integer ongoing) {
        this.won = won;
        this.lost = lost;
        this.ongoing = ongoing;
    }
}
