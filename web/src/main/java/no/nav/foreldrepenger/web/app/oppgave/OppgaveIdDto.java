package no.nav.foreldrepenger.web.app.oppgave;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.vedtak.util.InputValideringRegex;

public class OppgaveIdDto {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @Size(min = 1, max = 50)
    private String verdi;

    public OppgaveIdDto(String verdi) {
        this.verdi = verdi;
    }

    public String getVerdi() {
        return verdi;
    }
}
