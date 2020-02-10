package no.nav.foreldrepenger.domene.opptjening.dto;

public class OpptjeningPeriodeDto {

    private int måneder;
    private int dager;

    OpptjeningPeriodeDto() {
        // trengs for deserialisering av JSON
        this.måneder = 0;
        this.dager = 0;
    }

    OpptjeningPeriodeDto(int måneder, int dager) {
        this.måneder = måneder;
        this.dager = dager;
    }

    public int getMåneder() {
        return måneder;
    }

    public void setMåneder(int måneder) {
        this.måneder = måneder;
    }

    public int getDager() {
        return dager;
    }

    public void setDager(int dager) {
        this.dager = dager;
    }
}
