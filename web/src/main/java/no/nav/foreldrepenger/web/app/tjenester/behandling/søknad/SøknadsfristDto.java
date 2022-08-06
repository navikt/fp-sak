package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;

public class SøknadsfristDto {

    private LocalDate mottattDato;
    private LocalDate utledetSøknadsfrist;
    private LocalDate søknadsperiodeStart;
    private LocalDate søknadsperiodeSlutt;
    private long dagerOversittetFrist = 0;

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public LocalDate getUtledetSøknadsfrist() {
        return utledetSøknadsfrist;
    }

    public void setUtledetSøknadsfrist(LocalDate utledetSøknadsfrist) {
        this.utledetSøknadsfrist = utledetSøknadsfrist;
    }

    public LocalDate getSøknadsperiodeStart() {
        return søknadsperiodeStart;
    }

    public void setSøknadsperiodeStart(LocalDate søknadsperiodeStart) {
        this.søknadsperiodeStart = søknadsperiodeStart;
    }

    public LocalDate getSøknadsperiodeSlutt() {
        return søknadsperiodeSlutt;
    }

    public void setSøknadsperiodeSlutt(LocalDate søknadsperiodeSlutt) {
        this.søknadsperiodeSlutt = søknadsperiodeSlutt;
    }

    public long getDagerOversittetFrist() {
        return dagerOversittetFrist;
    }

    public void setDagerOversittetFrist(long dagerOversittetFrist) {
        this.dagerOversittetFrist = dagerOversittetFrist;
    }
}
