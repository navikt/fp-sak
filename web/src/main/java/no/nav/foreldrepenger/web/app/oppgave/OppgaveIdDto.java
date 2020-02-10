package no.nav.foreldrepenger.web.app.oppgave;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class OppgaveIdDto implements AbacDto {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @Size(min = 1, max = 50)
    private String verdi;

    public OppgaveIdDto(String verdi) {
        this.verdi = verdi;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.OPPGAVE_ID, verdi);
    }

    public String getVerdi() {
        return verdi;
    }
}
