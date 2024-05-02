package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

class SjekkOverlappTest {

    @Test
    void overlapp_og_mer_enn_100_prosent() {
        LocalDate FRA = LocalDate.now();
        LocalDate TIL = LocalDate.now().plusWeeks(4);
        var beregningsresultatEnitet = lagBeregningsresultat(FRA, TIL, 500, BigDecimal.valueOf(100));
        var anvist = genererAnvist(FRA, TIL, new Desimaltall(BigDecimal.valueOf(100)));
        var periode = new Periode();
        periode.setFom(FRA);
        periode.setTom(TIL);
        var aktør = new Aktør();
        aktør.setVerdi("111111");
        var ytelse = genererYtelseAbakus(aktør, periode, List.of(anvist));

        assertThat(SjekkOverlapp.erOverlappOgMerEnn100Prosent(Optional.of(beregningsresultatEnitet), List.of(ytelse))).isTrue();
    }

    @Test
    void ingen_overlapp_under_100_prosent() {
        LocalDate FRA = LocalDate.now();
        LocalDate TIL = LocalDate.now().plusWeeks(4);
        var beregningsresultatEnitet = lagBeregningsresultat(FRA, TIL, 500, BigDecimal.valueOf(40));
        var anvist = genererAnvist(FRA, TIL, new Desimaltall(BigDecimal.valueOf(30)));
        var periode = new Periode();
        periode.setFom(FRA);
        periode.setTom(TIL);
        var aktør = new Aktør();
        aktør.setVerdi("111111");
        var ytelse = genererYtelseAbakus(aktør, periode, List.of(anvist));

        assertThat(SjekkOverlapp.erOverlappOgMerEnn100Prosent(Optional.of(beregningsresultatEnitet), List.of(ytelse))).isFalse();
    }
    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, int dagsats, BigDecimal utbetalingsgrad) {
        int dagSatsRedusertMedUtbetalingsgrad;

        dagSatsRedusertMedUtbetalingsgrad = new BigDecimal(dagsats).multiply(utbetalingsgrad)
            .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
            .intValue();

        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(dagSatsRedusertMedUtbetalingsgrad)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(utbetalingsgrad)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private YtelseV1 genererYtelseAbakus(Aktør aktør, Periode periode, List<Anvisning> anvist) {
        var ytelse = new YtelseV1();
        ytelse.setKildesystem(Kildesystem.K9SAK);
        ytelse.setSaksnummer("6T5NM");
        ytelse.setVedtattTidspunkt(LocalDateTime.now());
        ytelse.setVedtakReferanse("1001-ABC");
        ytelse.setAktør(aktør);
        ytelse.setYtelse(mapYtelseType(YtelseType.PLEIEPENGER_SYKT_BARN));
        ytelse.setYtelseStatus(Status.LØPENDE);
        ytelse.setPeriode(periode);
        ytelse.setAnvist(anvist);
        return ytelse;
    }

    private Anvisning genererAnvist(LocalDate dateFom, LocalDate dateTom, Desimaltall utbetGrad) {
        var anvist = new Anvisning();
        var dagsats = new Desimaltall();
        var periode = new Periode();
        periode.setFom(dateFom);
        periode.setTom(dateTom);
        dagsats.setVerdi(BigDecimal.valueOf(300));

        anvist.setDagsats(dagsats);
        anvist.setPeriode(periode);
        anvist.setUtbetalingsgrad(utbetGrad);

        return anvist;
    }

    private Ytelser mapYtelseType(YtelseType type) {
        return switch (type) {
            case ENGANGSTØNAD -> Ytelser.ENGANGSTØNAD;
            case FORELDREPENGER -> Ytelser.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelser.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> Ytelser.OMSORGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelser.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelser.PLEIEPENGER_NÆRSTÅENDE;
            case OPPLÆRINGSPENGER -> Ytelser.OPPLÆRINGSPENGER;
            case FRISINN -> Ytelser.FRISINN;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }
}
