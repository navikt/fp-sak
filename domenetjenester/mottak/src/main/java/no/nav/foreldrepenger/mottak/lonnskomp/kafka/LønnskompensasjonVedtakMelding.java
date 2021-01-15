package no.nav.foreldrepenger.mottak.lonnskomp.kafka;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LønnskompensasjonVedtakMelding {
    private String id;
    private String fnr;
    private BigDecimal totalKompensasjon;
    private String bedriftNr;
    private LocalDate fom;
    private LocalDate tom;
    private String sakId;

    public String getId() {
        return id;
    }

    public String getFnr() {
        return fnr;
    }

    public BigDecimal getTotalKompensasjon() {
        return totalKompensasjon;
    }

    public String getBedriftNr() {
        return bedriftNr;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getSakId() {
        return sakId;
    }

}
