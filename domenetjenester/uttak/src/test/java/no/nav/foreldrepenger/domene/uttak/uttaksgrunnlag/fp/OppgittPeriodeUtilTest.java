package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;

class OppgittPeriodeUtilTest {

    @Test
    void sorterEtterFom() {
        var periode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2018, 6, 4), LocalDate.of(2018, 6, 10))
            .build();

        var perioder = OppgittPeriodeUtil.sorterEtterFom(List.of(periode1, periode2));

        assertThat(perioder.get(0)).isEqualTo(periode2);
        assertThat(perioder.get(1)).isEqualTo(periode1);
    }

    @Test
    void første_søkte_dato_kan_være_overføringsperiode() {

        var førstePeriodeOverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeOverføring, andrePeriodeUttaksperiode));

        assertThat(førsteSøkteUttaksdato).contains(førstePeriodeOverføring.getFom());
    }

    @Test
    void første_søkte_dato_kan_være_vanlig_uttaksperiode() {

        var førstePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeOverføring = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeUttaksperiode, andrePeriodeOverføring));

        assertThat(førsteSøkteUttaksdato).contains(førstePeriodeUttaksperiode.getFom());
    }

    @Test
    void første_søkte_dato_kan_være_utsettelseperioder() {

        var førstePeriodeUtsettelse = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeUtsettelse, andrePeriodeUttaksperiode));

        assertThat(førsteSøkteUttaksdato).contains(førstePeriodeUtsettelse.getFom());
    }

    @Test
    void første_søkte_dato_skal_ikke_være_oppholdsperioder() {

        var førstePeriodeOpphold = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER)
            .medPeriode(LocalDate.of(2018, 6, 11), LocalDate.of(2018, 6, 17))
            .build();

        var andrePeriodeUttaksperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(LocalDate.of(2018, 6, 18), LocalDate.of(2018, 6, 28))
            .build();

        var førsteSøkteUttaksdato = OppgittPeriodeUtil.finnFørsteSøkteUttaksdato(List.of(førstePeriodeOpphold, andrePeriodeUttaksperiode));

        assertThat(førsteSøkteUttaksdato).contains(andrePeriodeUttaksperiode.getFom());
    }

    @Test
    void skal_slå_sammen_like_perioder() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2021, 8, 20), LocalDate.of(2021, 8, 25))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2021, 8, 26), LocalDate.of(2021, 9, 3))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(1);
    }

    @Test
    void skal_slå_sammen_like_perioder_hvis_eneste_hull_er_helg() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2021, 8, 20), LocalDate.of(2021, 8, 27))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2021, 8, 30), LocalDate.of(2021, 9, 3))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(1);
    }

    @Test
    void skal_ikke_slå_sammen_like_perioder_hvis_hull() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2021, 8, 20), LocalDate.of(2021, 8, 27))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2021, 8, 31), LocalDate.of(2021, 9, 3))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_hvis_ulik_dok_vurdering() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medDokumentasjonVurdering(new DokumentasjonVurdering(MORS_AKTIVITET_GODKJENT))
            .medPeriode(LocalDate.of(2021, 8, 20), LocalDate.of(2021, 8, 25))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medDokumentasjonVurdering(null)
            .medPeriode(LocalDate.of(2021, 8, 26), LocalDate.of(2021, 9, 3))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(2);
    }

    @Test
    void skal_slå_sammen_like_perioder_hvis_to_perioder_i_helg() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 25), LocalDate.of(2025, 1, 25))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 26), LocalDate.of(2025, 1, 26))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(1);
        assertThat(slåttSammen.getFirst().getFom()).isEqualTo(p1.getFom());
        assertThat(slåttSammen.getFirst().getTom()).isEqualTo(p2.getTom());
    }

    @Test
    void fjerner_første_like_periode_hvis_siste_periode_i_helg() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 25), LocalDate.of(2025, 1, 25))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 26), LocalDate.of(2025, 1, 29))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(1);
        assertThat(slåttSammen.getFirst().getFom()).isEqualTo(p2.getFom().plusDays(1));
        assertThat(slåttSammen.getFirst().getTom()).isEqualTo(p2.getTom());
    }

    @Test
    void fjerner_siste_like_periode_hvis_siste_periode_i_helg() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 23), LocalDate.of(2025, 1, 24))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 25), LocalDate.of(2025, 1, 25))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(1);
        assertThat(slåttSammen.getFirst().getFom()).isEqualTo(p1.getFom());
        assertThat(slåttSammen.getFirst().getTom()).isEqualTo(p1.getTom());
    }

    @Test
    void fjerner_siste_like_periode_hvis_siste_periode_i_helg_2() {
        var p1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 23), LocalDate.of(2025, 1, 25))
            .build();

        var p2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2025, 1, 26), LocalDate.of(2025, 1, 26))
            .build();

        var slåttSammen = OppgittPeriodeUtil.slåSammenLikePerioder(List.of(p1, p2));

        assertThat(slåttSammen).hasSize(1);
        assertThat(slåttSammen.getFirst().getFom()).isEqualTo(p1.getFom());
        assertThat(slåttSammen.getFirst().getTom()).isEqualTo(p1.getTom().minusDays(1));
    }
}
