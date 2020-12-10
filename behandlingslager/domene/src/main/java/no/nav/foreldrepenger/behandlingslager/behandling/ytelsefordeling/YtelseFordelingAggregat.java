package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;

public class YtelseFordelingAggregat {

    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private OppgittFordelingEntitet overstyrtFordeling;
    private OppgittDekningsgradEntitet oppgittDekningsgrad;
    private OppgittRettighetEntitet oppgittRettighet;
    private PerioderUtenOmsorgEntitet perioderUtenOmsorg;
    private PerioderAleneOmsorgEntitet perioderAleneOmsorg;
    private PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon;
    private AvklarteUttakDatoerEntitet avklarteDatoer;
    private PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRett;
    private AktivitetskravPerioderEntitet opprinneligeAktivitetskravPerioder;
    private AktivitetskravPerioderEntitet saksbehandledeAktivitetskravPerioder;

    protected YtelseFordelingAggregat() {
    }

    public OppgittFordelingEntitet getOppgittFordeling() {
        return oppgittFordeling;
    }

    /**
     * Skal ikke brukes.
     * Bruk {@link no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository}
     */
    @Deprecated
    public OppgittDekningsgradEntitet getOppgittDekningsgrad() {
        return oppgittDekningsgrad;
    }

    public OppgittRettighetEntitet getOppgittRettighet() {
        return oppgittRettighet;
    }

    public Optional<PerioderUtenOmsorgEntitet> getPerioderUtenOmsorg() {
        return Optional.ofNullable(perioderUtenOmsorg);
    }

    public Optional<PerioderAleneOmsorgEntitet> getPerioderAleneOmsorg() {
        return Optional.ofNullable(perioderAleneOmsorg);
    }

    public Optional<PerioderAnnenforelderHarRettEntitet> getPerioderAnnenforelderHarRett() {
        return Optional.ofNullable(perioderAnnenforelderHarRett);
    }

    public Optional<AktivitetskravPerioderEntitet> getOpprinneligeAktivitetskravPerioder() {
        return Optional.ofNullable(opprinneligeAktivitetskravPerioder);
    }

    public Optional<AktivitetskravPerioderEntitet> getSaksbehandledeAktivitetskravPerioder() {
        return Optional.ofNullable(saksbehandledeAktivitetskravPerioder);
    }

    public Optional<AktivitetskravPerioderEntitet> getGjeldendeAktivitetskravPerioder() {
        return getSaksbehandledeAktivitetskravPerioder().isPresent() ? getSaksbehandledeAktivitetskravPerioder()
            : getOpprinneligeAktivitetskravPerioder();
    }

    public Optional<Boolean> getAnnenForelderRettAvklaring() {
        var perioder = getPerioderAnnenforelderHarRett();
        if (perioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(!perioder.get().getPerioder().isEmpty());
    }

    public OppgittFordelingEntitet getGjeldendeSøknadsperioder() {
        if (getOverstyrtFordeling().isPresent()) {
            return getOverstyrtFordeling().get();
        }
        if (getJustertFordeling().isPresent()) {
            return getJustertFordeling().get();
        }
        return getOppgittFordeling();
    }

    public Optional<OppgittFordelingEntitet> getJustertFordeling() {
        return Optional.ofNullable(justertFordeling);
    }

    public Optional<AvklarteUttakDatoerEntitet> getAvklarteDatoer() {
        return Optional.ofNullable(avklarteDatoer);
    }
    public Optional<OppgittFordelingEntitet> getOverstyrtFordeling() {
        return Optional.ofNullable(overstyrtFordeling);
    }

    public Optional<PerioderUttakDokumentasjonEntitet> getPerioderUttakDokumentasjon() {
        return Optional.ofNullable(perioderUttakDokumentasjon);
    }

    public static Builder oppdatere(Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.map(Builder::oppdatere).orElseGet(Builder::nytt);
    }

    public LocalDate getGjeldendeEndringsdato() {
        return getAvklarteDatoer()
            .orElseThrow(() -> new IllegalStateException("Finner ikke avklarte datoer"))
            .getGjeldendeEndringsdato();
    }

    public static class Builder {
        private YtelseFordelingAggregat kladd;

        private Builder() {
            this.kladd = new YtelseFordelingAggregat();
        }

        private Builder(YtelseFordelingAggregat ytelseFordelingAggregat) {
            this.kladd = ytelseFordelingAggregat;
        }

        public static Builder nytt() {
            return new Builder();
        }

        private static Builder oppdatere(YtelseFordelingAggregat ytelseFordelingAggregat) {
            return new Builder(ytelseFordelingAggregat);
        }

        public static Builder oppdatere(Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
            return ytelseFordelingAggregat.map(Builder::oppdatere).orElseGet(Builder::nytt);
        }

        public Builder medOppgittFordeling(OppgittFordelingEntitet fordeling) {
            kladd.oppgittFordeling = fordeling;
            return this;
        }

        public Builder medJustertFordeling(OppgittFordelingEntitet fordeling) {
            kladd.justertFordeling = fordeling;
            return this;
        }

        public Builder medOverstyrtFordeling(OppgittFordelingEntitet fordeling) {
            if (fordeling != null && kladd.getOppgittFordeling() == null) {
                throw YtelseFordelingFeil.FACTORY.kanIkkeOverstyreDetFinnesIkkeOrginalSøknadsperiode().toException();
            }
            kladd.overstyrtFordeling = fordeling;
            return this;
        }

        public Builder medOppgittDekningsgrad(OppgittDekningsgradEntitet oppgittDekningsgrad) {
            kladd.oppgittDekningsgrad = oppgittDekningsgrad;
            return this;
        }

        public Builder medOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
            kladd.oppgittRettighet = oppgittRettighet;
            return this;
        }

        public Builder medPerioderUtenOmsorg(PerioderUtenOmsorgEntitet perioderUtenOmsorg) {
            kladd.perioderUtenOmsorg = perioderUtenOmsorg;
            return this;
        }

        public Builder medPerioderAleneOmsorg(PerioderAleneOmsorgEntitet perioderAleneOmsorg) {
            kladd.perioderAleneOmsorg = perioderAleneOmsorg;
            return this;
        }

        public Builder medOpprinneligeAktivitetskravPerioder(AktivitetskravPerioderEntitet aktivitetskravPerioder) {
            kladd.opprinneligeAktivitetskravPerioder = aktivitetskravPerioder;
            return this;
        }

        public Builder medSaksbehandledeAktivitetskravPerioder(AktivitetskravPerioderEntitet aktivitetskravPerioder) {
            kladd.saksbehandledeAktivitetskravPerioder = aktivitetskravPerioder;
            return this;
        }

        public Builder medPerioderAnnenforelderHarRett(PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRett) {
            kladd.perioderAnnenforelderHarRett = perioderAnnenforelderHarRett;
            return this;
        }

        public Builder medPerioderUttakDokumentasjon(PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon) {
            kladd.perioderUttakDokumentasjon = perioderUttakDokumentasjon;
            return this;
        }

        public Builder medAvklarteDatoer(AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
            kladd.avklarteDatoer = avklarteUttakDatoer;
            return this;
        }

        public YtelseFordelingAggregat build() {
            return kladd;
        }
    }
}
