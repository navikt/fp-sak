package no.nav.foreldrepenger.web.app.oppgave;

import javax.validation.constraints.Digits;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class FagsakIdDto implements AbacDto {

    @Digits(integer = 18, fraction = 0)
    private String verdi;

    public FagsakIdDto(String verdi) {
        this.verdi = verdi;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.FAGSAK_ID, getVerdi());
    }

    public Long getVerdi() {
        return Long.parseLong(verdi);
    }
}
