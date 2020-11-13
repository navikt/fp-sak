package no.nav.foreldrepenger.domene.uttak.testutilities.fagsak;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class FagsakRelasjonBuilder {

    private FagsakYtelseType ytelseType;

    private Dekningsgrad dekningsgrad;

    private LocalDate fødselsdato;
    private LocalDate termindato;

    public FagsakRelasjonBuilder(FagsakYtelseType type) {
        ytelseType = type;
    }

    public static FagsakRelasjonBuilder engangsstønad() {
        return new FagsakRelasjonBuilder(FagsakYtelseType.ENGANGSTØNAD);
    }

    public static FagsakRelasjonBuilder foreldrepenger() {
        return new FagsakRelasjonBuilder(FagsakYtelseType.FORELDREPENGER);
    }

    public static FagsakRelasjonBuilder svangerskapspenger() {
        return new FagsakRelasjonBuilder(FagsakYtelseType.SVANGERSKAPSPENGER);
    }

    public FagsakRelasjonBuilder medTermindato(LocalDate dato) {
        this.termindato = dato;
        return this;
    }

    public LocalDate getTermindato() {
        return termindato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Dekningsgrad getDekningsgrad() {
        return dekningsgrad;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    void setDefaults() {
        fødselsdato = LocalDate.now();
        termindato = fødselsdato;
        if (dekningsgrad == null) {
            dekningsgrad = Dekningsgrad._100;
        }
    }

}
