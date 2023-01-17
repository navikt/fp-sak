package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.vedtak.exception.TekniskException;

public class YtelseFordelingAggregat {

    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private OppgittFordelingEntitet overstyrtFordeling;
    private OppgittDekningsgradEntitet oppgittDekningsgrad;
    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittRettighetEntitet overstyrtRettighet;
    private PerioderUtenOmsorgEntitet perioderUtenOmsorg;
    private PerioderAleneOmsorgEntitet perioderAleneOmsorg;
    private PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon;
    private AvklarteUttakDatoerEntitet avklarteDatoer;
    private PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRett;
    private PerioderAnnenForelderRettEØSEntitet perioderAnnenForelderRettEØS;
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

    public Optional<OppgittRettighetEntitet> getOverstyrtRettighet() {
        return Optional.ofNullable(overstyrtRettighet);
    }

    public Optional<PerioderUtenOmsorgEntitet> getPerioderUtenOmsorg() {
        return Optional.ofNullable(perioderUtenOmsorg);
    }

    Optional<PerioderAleneOmsorgEntitet> getPerioderAleneOmsorg() {
        return Optional.ofNullable(perioderAleneOmsorg);
    }

    Optional<PerioderAnnenforelderHarRettEntitet> getPerioderAnnenforelderHarRett() {
        return Optional.ofNullable(perioderAnnenforelderHarRett);
    }

    Optional<PerioderAnnenForelderRettEØSEntitet> getPerioderAnnenForelderRettEØS() {
        return Optional.ofNullable(perioderAnnenForelderRettEØS);
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

    public Boolean getAleneomsorgAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getHarAleneomsorgForBarnet)
            .or(() -> getPerioderAleneOmsorg().map(p -> !p.getPerioder().isEmpty()))
            .orElse(null);
    }

    public Boolean getAnnenForelderRettAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getHarAnnenForeldreRett)
            .or(() -> getPerioderAnnenforelderHarRett().map(p -> !p.getPerioder().isEmpty()))
            .orElse(null);
    }

    public Boolean getAnnenForelderRettEØSAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getAnnenForelderRettEØSNullable)
            .or(() -> getPerioderAnnenForelderRettEØS().map(p -> !p.getPerioder().isEmpty()))
            .orElse(null);
    }

    public Boolean getMorUføretrygdAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getMorMottarUføretrygd).orElse(null);
    }

    public OppgittFordelingEntitet getGjeldendeFordeling() {
        return getOverstyrtFordeling().or(this::getJustertFordeling).orElseGet(this::getOppgittFordeling);
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

    public static Builder oppdatere(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return oppdatere(Optional.ofNullable(ytelseFordelingAggregat));
    }

    public LocalDate getGjeldendeEndringsdato() {
        return getGjeldendeEndringsdatoHvisEksisterer()
            .orElseThrow(() -> new IllegalStateException("Finner ikke endringsdato"));
    }

    public Optional<LocalDate> getGjeldendeEndringsdatoHvisEksisterer() {
        return getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato);
    }

    public static class Builder {
        private final YtelseFordelingAggregat kladd;

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
                throw new TekniskException("FP-852328",
                    "Kan ikke overstyre søknadsperioder før det finnes noen søknadsperioder å overstyre.");
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

        public Builder medOverstyrtRettighet(OppgittRettighetEntitet overstyrtRettighet) {
            kladd.overstyrtRettighet = overstyrtRettighet;
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

        public Builder medPerioderAnnenForelderRettEØS(PerioderAnnenForelderRettEØSEntitet perioderAnnenForelderRettEØS) {
            kladd.perioderAnnenForelderRettEØS = perioderAnnenForelderRettEØS;
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
