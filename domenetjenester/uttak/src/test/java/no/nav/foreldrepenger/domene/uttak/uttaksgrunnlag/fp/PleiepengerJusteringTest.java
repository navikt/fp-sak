package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static java.time.LocalDate.of;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.INSTITUSJON_BARN;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.JusterFordelingTjeneste.flyttFraHelgTilFredag;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.tidsserie.LocalDateSegment;

class PleiepengerJusteringTest {

    @Test
    void pleiepenger_med_utbetaling_skal_opprette_utsettelse() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval1 = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 1),
            of(2020, 1, 13));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval1)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var pleiepengerUtenUtbetalingInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 14),
            of(2020, 1, 14));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerUtenUtbetalingInterval)
            .medUtbetalingsgradProsent(BigDecimal.ZERO)
            .build());
        var pleiepengerInterval2 = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 15),
            of(2020, 1, 15));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval2)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2019, 12, 10), of(2020, 4, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote));

        assertThat(resultat).hasSize(5);
        assertThat(resultat.get(0).isUtsettelse()).isFalse();
        assertThat(resultat.get(0).getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(resultat.get(0).getTom()).isEqualTo(pleiepengerInterval1.getFomDato().minusDays(1));
        assertThat(resultat.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);

        assertThat(resultat.get(1).isUtsettelse()).isTrue();
        assertThat(resultat.get(1).getFom()).isEqualTo(pleiepengerInterval1.getFomDato());
        assertThat(resultat.get(1).getTom()).isEqualTo(pleiepengerInterval1.getTomDato());
        assertThat(resultat.get(1).getÅrsak()).isEqualTo(INSTITUSJON_BARN);
        assertThat(resultat.get(1).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.ANDRE_NAV_VEDTAK);

        assertThat(resultat.get(2).isUtsettelse()).isFalse();
        assertThat(resultat.get(2).getFom()).isEqualTo(pleiepengerUtenUtbetalingInterval.getFomDato());
        assertThat(resultat.get(2).getTom()).isEqualTo(pleiepengerUtenUtbetalingInterval.getTomDato());
        assertThat(resultat.get(2).getPeriodeType()).isEqualTo(MØDREKVOTE);

        assertThat(resultat.get(3).isUtsettelse()).isTrue();
        assertThat(resultat.get(3).getFom()).isEqualTo(pleiepengerInterval2.getFomDato());
        assertThat(resultat.get(3).getTom()).isEqualTo(pleiepengerInterval2.getTomDato());
        assertThat(resultat.get(3).getÅrsak()).isEqualTo(INSTITUSJON_BARN);
        assertThat(resultat.get(3).getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.ANDRE_NAV_VEDTAK);

        assertThat(resultat.get(4).isUtsettelse()).isFalse();
        assertThat(resultat.get(4).getFom()).isEqualTo(pleiepengerInterval2.getTomDato().plusDays(1));
        assertThat(resultat.get(4).getTom()).isEqualTo(mødrekvote.getTom());
        assertThat(resultat.get(4).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    @Test
    void annen_ytelse_med_utbetaling_skal_opprette_ikke_utsettelse() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medYtelseType(RelatertYtelseType.SYKEPENGER);
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 1), of(2020, 1, 13));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2019, 12, 10), of(2020, 4, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).isUtsettelse()).isFalse();
        assertThat(resultat.get(0).getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(resultat.get(0).getTom()).isEqualTo(mødrekvote.getTom());
        assertThat(resultat.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    @Test
    void pleiepenger_fra_annet_system_enn_k9sak_skal_ikke_opprette_utsettelse() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.INFOTRYGD)
            .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN);
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 1), of(2020, 1, 13));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2019, 12, 10), of(2020, 4, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).isUtsettelse()).isFalse();
    }

    @Test
    void slå_sammen_hvis_overlappende_ytelse() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval1 = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 1), of(2020, 1, 13));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval1)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var pleiepengerInterval2 = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 5), of(2020, 1, 10));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval2)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2019, 12, 10), of(2020, 4, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote));

        assertThat(resultat).hasSize(3);
        assertThat(resultat.get(0).isUtsettelse()).isFalse();
        assertThat(resultat.get(1).isUtsettelse()).isTrue();
        assertThat(resultat.get(1).getFom()).isEqualTo(of(2020, 1, 1));
        assertThat(resultat.get(1).getTom()).isEqualTo(of(2020, 1, 13));
        assertThat(resultat.get(2).isUtsettelse()).isFalse();
    }

    @Test
    void ikke_opprette_pleiepenger_hvis_søknadsperiode_mottatt_etter_pleiepenge_vedtak() {
        var aktørId = AktørId.dummy();
        var vedtakTidspunkt = LocalDateTime.of(2021, 1, 1, 1, 1, 1);
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medVedtattTidspunkt(vedtakTidspunkt)
            .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN);
        var pleiepengerFom = of(2020, 1, 15);
        var pleiepengerTom = of(2020, 2, 13);
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(pleiepengerFom, pleiepengerTom);
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote1 = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2020, 1, 1), of(2020, 2, 1))
            .medPeriodeType(MØDREKVOTE)
            .medMottattDato(vedtakTidspunkt.minusWeeks(1).toLocalDate())
            .build();
        var mødrekvoteFraEndringssøknad = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2020, 2, 2), of(2020, 4, 4))
            .medPeriodeType(MØDREKVOTE)
            .medMottattDato(vedtakTidspunkt.plusWeeks(1).toLocalDate())
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote1, mødrekvoteFraEndringssøknad));

        assertThat(resultat).hasSize(3);
        assertThat(resultat.get(0).isUtsettelse()).isFalse();
        assertThat(resultat.get(0).getTom()).isEqualTo(pleiepengerFom.minusDays(1));
        assertThat(resultat.get(1).isUtsettelse()).isTrue();
        assertThat(resultat.get(1).getFom()).isEqualTo(pleiepengerFom);
        assertThat(resultat.get(1).getTom()).isEqualTo(mødrekvoteFraEndringssøknad.getFom().minusDays(1));
        assertThat(resultat.get(2).isUtsettelse()).isFalse();
    }

    @Test
    void combine() {
        var pleiepengerFørFp = pleiepenger(of(2020, 11, 11), of(2020, 12, 12));
        var pleiepengerIFp1 = pleiepenger(of(2021, 1, 15), of(2021, 2, 1));
        var pleiepengerIFp2 = pleiepenger(of(2021, 2, 5), of(2021, 2, 5));
        var pleiepengerEtterFp = pleiepenger(of(2021, 3, 15), of(2021, 3, 20));
        var pleiepengerUtsettelser = List.of(pleiepengerFørFp, pleiepengerEtterFp, pleiepengerIFp1, pleiepengerIFp2);
        var foreldrepenger = List.of(
            OppgittPeriodeBuilder.ny().medPeriodeType(MØDREKVOTE).medPeriode(of(2021, 1, 1), of(2021, 3, 3)).build());
        var resultat = PleiepengerJustering.combine(pleiepengerUtsettelser, foreldrepenger);
        assertThat(resultat).hasSize(5);
        assertThat(resultat.get(0).getFom()).isEqualTo(of(2021, 1, 1));
        assertThat(resultat.get(0).getTom()).isEqualTo(of(2021, 1, 14));
        assertThat(resultat.get(0).isUtsettelse()).isFalse();
        assertThat(resultat.get(1).getFom()).isEqualTo(of(2021, 1, 15));
        assertThat(resultat.get(1).getTom()).isEqualTo(of(2021, 2, 1));
        assertThat(resultat.get(1).isUtsettelse()).isTrue();
        assertThat(resultat.get(2).getFom()).isEqualTo(of(2021, 2, 2));
        assertThat(resultat.get(2).getTom()).isEqualTo(of(2021, 2, 4));
        assertThat(resultat.get(2).isUtsettelse()).isFalse();
        assertThat(resultat.get(3).getFom()).isEqualTo(of(2021, 2, 5));
        assertThat(resultat.get(3).getTom()).isEqualTo(of(2021, 2, 5));
        assertThat(resultat.get(3).isUtsettelse()).isTrue();
        assertThat(resultat.get(4).getFom()).isEqualTo(of(2021, 2, 6));
        assertThat(resultat.get(4).getTom()).isEqualTo(of(2021, 3, 3));
        assertThat(resultat.get(4).isUtsettelse()).isFalse();
    }

    @Test
    void ikke_opprette_pleiepenger_hvis_før_startdato_eller_etter_sluttdato_for_fp() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 2, 1), of(2020, 5, 5));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriode(pleiepengerInterval.getFomDato().plusWeeks(1), pleiepengerInterval.getTomDato().minusWeeks(1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).isUtsettelse()).isTrue();
        assertThat(resultat.get(0).getFom()).isEqualTo(mødrekvote.getFom());
        assertThat(resultat.get(0).getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    void opprette_pleiepenger_hvis_hvis_hull_i_fp() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 1), of(2020, 2, 1));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote1 = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2020, 1, 1), of(2020, 1, 10))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var mødrekvote2 = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2020, 1, 15), of(2020, 2, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote1, mødrekvote2));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).isUtsettelse()).isTrue();
        assertThat(resultat.get(0).getFom()).isEqualTo(pleiepengerInterval.getFomDato());
        assertThat(resultat.get(0).getTom()).isEqualTo(flyttFraHelgTilFredag(pleiepengerInterval.getTomDato()));
    }

    @Test
    void skal_slå_sammen_utsettelser_hvis_anviste_perioder_har_hull_i_helger() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval1 = DatoIntervallEntitet.fraOgMedTilOgMed(of(2021, 8, 23),
            of(2021, 8, 27));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval1)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var pleiepengerInterval2 = DatoIntervallEntitet.fraOgMedTilOgMed(of(2021, 8, 30),
            of(2021, 9, 3));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval2)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2021, 8, 23), of(2021, 10, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote));

        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).isUtsettelse()).isTrue();
        assertThat(resultat.get(0).getFom()).isEqualTo(pleiepengerInterval1.getFomDato());
        assertThat(resultat.get(0).getTom()).isEqualTo(pleiepengerInterval2.getTomDato());
        assertThat(resultat.get(1).isUtsettelse()).isFalse();
        assertThat(resultat.get(1).getFom()).isEqualTo(pleiepengerInterval2.getTomDato().plusDays(1));
        assertThat(resultat.get(1).getTom()).isEqualTo(mødrekvote.getTom());
    }

    @Test
    void skal_håndtere_empty_oppgitteperioder() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 1), of(2020, 2, 1));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of());

        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_feile_hvis_overlapp_i_oppgitte_perioder() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = pleiepengerFraK9();
        var pleiepengerInterval = DatoIntervallEntitet.fraOgMedTilOgMed(of(2020, 1, 15),
            of(2020, 1, 15));
        ytelseBuilder.medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
            .medAnvistPeriode(pleiepengerInterval)
            .medUtbetalingsgradProsent(BigDecimal.TEN)
            .build());
        var iay = iay(aktørId, ytelseBuilder);
        var mødrekvote1 = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2019, 12, 10), of(2020, 4, 1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var mødrekvote2 = OppgittPeriodeBuilder.ny()
            .medPeriode(of(2020, 1, 1), of(2020, 5, 5))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var resultat = PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote1, mødrekvote2));

        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).getFom()).isEqualTo(mødrekvote1.getFom());
        assertThat(resultat.get(0).getPeriodeType()).isEqualTo(MØDREKVOTE);
        assertThat(resultat.get(1).getFom()).isEqualTo(mødrekvote2.getFom());
        assertThat(resultat.get(1).getPeriodeType()).isEqualTo(MØDREKVOTE);
    }

    private InntektArbeidYtelseGrunnlag iay(AktørId aktørId, YtelseBuilder ytelseBuilder) {
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
                .leggTilAktørYtelse(InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty())
                    .medAktørId(aktørId)
                    .leggTilYtelse(ytelseBuilder)))
            .build();
    }

    private YtelseBuilder pleiepengerFraK9() {
        return YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN)
            .medVedtattTidspunkt(LocalDateTime.now().minusMinutes(5));
    }

    private LocalDateSegment<PleiepengerJustering.PleiepengerUtsettelse> pleiepenger(LocalDate fom, LocalDate tom) {
        return new LocalDateSegment<>(fom, tom, new PleiepengerJustering.PleiepengerUtsettelse(LocalDateTime.now(), INSTITUSJON_BARN));
    }
}
