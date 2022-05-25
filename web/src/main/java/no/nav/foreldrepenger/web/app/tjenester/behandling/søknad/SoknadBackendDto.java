package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.familiehendelse.rest.SøknadType;

public class SoknadBackendDto {

    private SøknadType soknadType;
    private LocalDate mottattDato;
    private LocalDate soknadsdato;
    private OppgittRettighetDto oppgittRettighet;
    private boolean oppgittAleneomsorg;
    private Språkkode spraakkode;

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

    public LocalDate getSoknadsdato() {
        return soknadsdato;
    }

    public void setSoknadsdato(LocalDate soknadsdato) {
        this.soknadsdato = soknadsdato;
    }

    public OppgittRettighetDto getOppgittRettighet() {
        return oppgittRettighet;
    }

    public void setOppgittRettighet(OppgittRettighetDto oppgittRettighet) {
        this.oppgittRettighet = oppgittRettighet;
    }

    public Språkkode getSpraakkode() {
        return spraakkode;
    }

    public void setSpraakkode(Språkkode spraakkode) {
        this.spraakkode = spraakkode;
    }

    public boolean isOppgittAleneomsorg() {
        return oppgittAleneomsorg;
    }

    public void setOppgittAleneomsorg(boolean oppgittAleneomsorg) {
        this.oppgittAleneomsorg = oppgittAleneomsorg;
    }
}
