package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import static java.lang.Boolean.TRUE;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.vedtak.exception.TekniskException;

public class YtelseFordelingAggregat {

    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private OppgittFordelingEntitet overstyrtFordeling;

    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittRettighetEntitet avklartRettighet;
    private Rettighetstype overstyrtRettighetstype;

    private AvklarteUttakDatoerEntitet avklarteDatoer;
    private Dekningsgrad oppgittDekningsgrad;
    private Dekningsgrad sakskompleksDekningsgrad;
    private Boolean overstyrtOmsorg;

    protected YtelseFordelingAggregat() {
    }

    // Brukes i tilfelle og steder der aleneomsorg ventes å ha vært manuelt/maskinelt avklart. OBS på nye førstegangssøknader
    public boolean robustHarAleneomsorg(RelasjonsRolleType relasjonsRolleType) {
        return RelasjonsRolleType.erMor(relasjonsRolleType) ? harAleneomsorg() : TRUE.equals(getAleneomsorgAvklaring());
    }

    public boolean harAleneomsorg() {
        return Optional.ofNullable(getAleneomsorgAvklaring())
            .orElseGet(() -> TRUE.equals(getOppgittRettighet().getHarAleneomsorgForBarnet()));
    }

    public boolean harAnnenForelderRett() {
        return harAnnenForelderRett(false);
    }

    public boolean harAnnenForelderRett(boolean annenpartHarForeldrepengerUtbetaling) {
        if ((getOverstyrtRettighetstype().isEmpty() && annenpartHarForeldrepengerUtbetaling) || avklartAnnenForelderHarRettEØS()) {
            return true;
        }
        return Optional.ofNullable(getAnnenForelderRettAvklaring())
            .orElseGet(() -> {
                var or = getOppgittRettighet();
                Objects.requireNonNull(or, "oppgittRettighet");
                return or.getHarAnnenForeldreRett() == null || or.getHarAnnenForeldreRett();
            });
    }

    public boolean morMottarUføretrygd(UføretrygdGrunnlagEntitet uføretrygdGrunnlag) {
        // Inntil videre er oppgittrettighet ikke komplett - derfor ser vi på om det finnes et UFO-grunnlag
        return Optional.ofNullable(getMorUføretrygdAvklaring())
            .orElseGet(() -> Optional.ofNullable(uføretrygdGrunnlag).filter(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd).isPresent());
    }

    public boolean avklartAnnenForelderHarRettEØS() {
        return TRUE.equals(getAnnenForelderRettEØSAvklaring());
    }

    public boolean oppgittAnnenForelderTilknytningEØS() {
        //Tidligere søknaden hadde ikke spørsmål om opphold, bare rett
        return Objects.equals(getOppgittRettighet().getAnnenForelderOppholdEØS(), TRUE)
            || oppgittAnnenForelderRettEØS();
    }

    public boolean oppgittAnnenForelderRettEØS() {
        return getOppgittRettighet().getAnnenForelderRettEØS();
    }

    public OppgittFordelingEntitet getOppgittFordeling() {
        return oppgittFordeling;
    }

    public OppgittRettighetEntitet getOppgittRettighet() {
        return oppgittRettighet;
    }

    public Optional<OppgittRettighetEntitet> getAvklartRettighet() {
        return Optional.ofNullable(avklartRettighet);
    }

    public Optional<Rettighetstype> getOverstyrtRettighetstype() {
        return Optional.ofNullable(overstyrtRettighetstype);
    }

    public Boolean getOverstyrtOmsorg() {
        return overstyrtOmsorg;
    }

    public boolean harOmsorg() {
        return overstyrtOmsorg == null || overstyrtOmsorg;
    }

    public Boolean getAleneomsorgAvklaring() {
        return getOverstyrtRettighetstype()
            .map(Rettighetstype.ALENEOMSORG::equals)
            .or(() -> getAvklartRettighet().map(OppgittRettighetEntitet::getHarAleneomsorgForBarnet))
            .orElse(null);
    }

    public Boolean getAnnenForelderRettAvklaring() {
        return getOverstyrtRettighetstype()
            .map(Rettighetstype.BEGGE_RETT::equals)
            .or(() -> getAvklartRettighet().map(OppgittRettighetEntitet::getHarAnnenForeldreRett))
            .orElse(null);
    }

    public Boolean getAnnenForelderRettEØSAvklaring() {
        return getOverstyrtRettighetstype()
            .map(Rettighetstype.BEGGE_RETT_EØS::equals)
            .or(() -> getAvklartRettighet().map(OppgittRettighetEntitet::getAnnenForelderRettEØSNullable))
            .orElse(null);
    }

    public Boolean getMorUføretrygdAvklaring() {
        return getOverstyrtRettighetstype()
            .map(Rettighetstype.BARE_FAR_RETT_MOR_UFØR::equals)
            .or(() -> getAvklartRettighet().map(OppgittRettighetEntitet::getMorMottarUføretrygd))
            .orElse(null);
    }

    public OppgittFordelingEntitet getGjeldendeFordeling() {
        return getOverstyrtFordeling()
            .or(this::getJustertFordeling)
            .orElseGet(this::getOppgittFordeling);
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

    public Rettighetstype getGjeldendeRettighetstype(boolean annenpartHarForeldrepengerUtbetaling,
                                                     RelasjonsRolleType relasjonsRolleType,
                                                     UføretrygdGrunnlagEntitet uføretrygdGrunnlag) {
        if (overstyrtRettighetstype != null) {
            return overstyrtRettighetstype;
        }
        if (annenpartHarForeldrepengerUtbetaling) {
            return Rettighetstype.BEGGE_RETT;
        }
        if (avklartAnnenForelderHarRettEØS()) {
            return Rettighetstype.BEGGE_RETT_EØS;
        }
        if (robustHarAleneomsorg(relasjonsRolleType)) {
            return Rettighetstype.ALENEOMSORG;
        }
        if (relasjonsRolleType.erFarEllerMedMor() && morMottarUføretrygd(uføretrygdGrunnlag)) { //Legger gamle saker med avklart annenpart ufør selv om søker er mor
            return Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
        }
        return Boolean.TRUE.equals(Optional.ofNullable(getAnnenForelderRettAvklaring())
            .orElseGet(() -> {
                var or = getOppgittRettighet();
                Objects.requireNonNull(or, "oppgittRettighet");
                return or.getHarAnnenForeldreRett() == null || or.getHarAnnenForeldreRett();
            })) ? Rettighetstype.BEGGE_RETT : bareSøkerRett(relasjonsRolleType);
    }

    private static Rettighetstype bareSøkerRett(RelasjonsRolleType relasjonsRolleType) {
        return relasjonsRolleType.erFarEllerMedMor() ? Rettighetstype.BARE_FAR_RETT : Rettighetstype.BARE_MOR_RETT;
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

        public Builder medAvklartRettighet(OppgittRettighetEntitet avklartRettighet) {
            kladd.avklartRettighet = avklartRettighet;
            return this;
        }

        public Builder medOverstyrtRettighetstype(Rettighetstype overstyrtRettighetstype) {
            kladd.overstyrtRettighetstype = overstyrtRettighetstype;
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
