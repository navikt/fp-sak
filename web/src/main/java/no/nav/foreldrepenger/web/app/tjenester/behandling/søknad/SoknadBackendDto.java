package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;

public class SoknadBackendDto {

    private SøknadType soknadType;
    private LocalDate mottattDato;
    private boolean oppgittAleneomsorg;

    protected SoknadBackendDto() {
    }

    public SøknadType getSoknadType() {
        return soknadType;
    }

    public boolean erSoknadsType(SøknadType søknadType) {
        return søknadType.equals(this.soknadType);
    }

    public void setSoknadType(SøknadType soknadType) {
        this.soknadType = soknadType;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public boolean isOppgittAleneomsorg() {
        return oppgittAleneomsorg;
    }

    public void setOppgittAleneomsorg(boolean oppgittAleneomsorg) {
        this.oppgittAleneomsorg = oppgittAleneomsorg;
    }
}
