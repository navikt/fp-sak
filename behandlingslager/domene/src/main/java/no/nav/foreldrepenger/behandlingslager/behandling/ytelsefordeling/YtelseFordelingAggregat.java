package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.vedtak.exception.TekniskException;

public class YtelseFordelingAggregat {

    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private OppgittFordelingEntitet overstyrtFordeling;
    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittRettighetEntitet overstyrtRettighet;
    private AvklarteUttakDatoerEntitet avklarteDatoer;
    private Dekningsgrad oppgittDekningsgrad;
    private Dekningsgrad sakskompleksDekningsgrad;
    private Boolean overstyrtOmsorg;

    protected YtelseFordelingAggregat() {
    }

    public OppgittFordelingEntitet getOppgittFordeling() {
        return oppgittFordeling;
    }

    public OppgittRettighetEntitet getOppgittRettighet() {
        return oppgittRettighet;
    }

    public Optional<OppgittRettighetEntitet> getOverstyrtRettighet() {
        return Optional.ofNullable(overstyrtRettighet);
    }

    public Boolean getOverstyrtOmsorg() {
        return overstyrtOmsorg;
    }

    public boolean harOmsorg() {
        return overstyrtOmsorg == null || overstyrtOmsorg;
    }

    public Boolean getAleneomsorgAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getHarAleneomsorgForBarnet)
            .orElse(null);
    }

    public Boolean getAnnenForelderRettAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getHarAnnenForeldreRett)
            .orElse(null);
    }

    public Boolean getAnnenForelderRettEØSAvklaring() {
        return getOverstyrtRettighet().map(OppgittRettighetEntitet::getAnnenForelderRettEØSNullable)
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

    public Dekningsgrad getOppgittDekningsgrad() {
        return oppgittDekningsgrad;
    }

    public Dekningsgrad getSakskompleksDekningsgrad() {
        return sakskompleksDekningsgrad;
    }

    public Dekningsgrad getGjeldendeDekningsgrad() {
        return Optional.ofNullable(getSakskompleksDekningsgrad()).orElse(getOppgittDekningsgrad());
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

        public Builder medOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
            kladd.oppgittRettighet = oppgittRettighet;
            return this;
        }

        public Builder medOverstyrtRettighet(OppgittRettighetEntitet overstyrtRettighet) {
            kladd.overstyrtRettighet = overstyrtRettighet;
            return this;
        }

        public Builder medOverstyrtOmsorg(Boolean harOmsorg) {
            kladd.overstyrtOmsorg = harOmsorg;
            return this;
        }

        public Builder medAvklarteDatoer(AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
            kladd.avklarteDatoer = avklarteUttakDatoer;
            return this;
        }

        public Builder medOppgittDekningsgrad(Dekningsgrad dekningsgrad) {
            kladd.oppgittDekningsgrad = dekningsgrad;
            return this;
        }

        public Builder medSakskompleksDekningsgrad(Dekningsgrad dekningsgrad) {
            kladd.sakskompleksDekningsgrad = dekningsgrad;
            return this;
        }

        public YtelseFordelingAggregat build() {
            return kladd;
        }
    }
}
