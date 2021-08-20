package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static java.time.LocalDate.of;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.INSTITUSJON_BARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

class PleiepengerJusteringTest {

    @Test
    void pleiepenger_med_utbetaling_skal_opprette_utsettelse() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN);
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
    void exception_hvis_overlappende_ytelse() {
        var aktørId = AktørId.dummy();
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN);
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
        assertThrows(IllegalStateException.class, () -> PleiepengerJustering.juster(aktørId, iay, List.of(mødrekvote)));
    }

    @Test
    void ikke_opprette_pleiepenger_hvis_søknadsperiode_mottatt_etter_pleiepenge_vedtak() {
        var aktørId = AktørId.dummy();
        var vedtakTidspunkt = LocalDateTime.of(2021, 1, 1, 1, 1, 1);
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(Fagsystem.K9SAK)
            .medVedtattTidspunkt(vedtakTidspunkt)
            .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN);
        var pleiepengerFom = of(2020, 2, 1);
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

    private InntektArbeidYtelseGrunnlag iay(AktørId aktørId, YtelseBuilder ytelseBuilder) {
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
                .leggTilAktørYtelse(InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty())
                    .medAktørId(aktørId)
                    .leggTilYtelse(ytelseBuilder)))
            .build();
    }

    @Test
    void combine() {
        var pleiepengerFørFp = pleiepenger(of(2020, 11, 11), of(2020, 12, 12));
        var pleiepengerIFp1 = pleiepenger(of(2021, 1, 15), of(2021, 2, 1));
        var pleiepengerIFp2 = pleiepenger(of(2021, 2, 5), of(2021, 2, 5));
        var pleiepengerEtterFp = pleiepenger(of(2021, 3, 15), of(2021, 3, 20));
        var pleiepengerUtsettelser = List.of(pleiepengerFørFp, pleiepengerEtterFp, pleiepengerIFp1, pleiepengerIFp2);
        var foreldrepenger = List.of(
            OppgittPeriodeBuilder.ny().medPeriode(of(2021, 1, 1), of(2021, 3, 3)).build());
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

    private PleiepengerJustering.PleiepengerUtsettelse pleiepenger(LocalDate fom, LocalDate tom) {
        var oppgittPeriode = OppgittPeriodeBuilder.ny().medPeriode(fom, tom).medÅrsak(INSTITUSJON_BARN).build();
        return new PleiepengerJustering.PleiepengerUtsettelse(LocalDateTime.now(), oppgittPeriode);
    }
}
