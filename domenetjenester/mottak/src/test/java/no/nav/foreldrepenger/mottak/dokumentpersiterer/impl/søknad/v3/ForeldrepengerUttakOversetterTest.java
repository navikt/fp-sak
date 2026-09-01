package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

class ForeldrepengerUttakOversetterTest {

    private static final LocalDate IKRAFT_FRA_DATO = UtsettelseCore2021.IKRAFT_FRA_DATO;

    @Test
    void skalBeholdePerioderSomKreverSammenhengendeUttak() {
        var periode = lagPeriode(IKRAFT_FRA_DATO.minusDays(1), UtsettelseÅrsak.ARBEID);

        assertThat(filtrer(periode)).containsExactly(periode);
    }

    @Test
    void skalBeholdeUttaksperiode() {
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(IKRAFT_FRA_DATO, IKRAFT_FRA_DATO.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        assertThat(filtrer(periode)).containsExactly(periode);
    }

    @Test
    void skalBeholdeOverføringsperiode() {
        var periode = lagPeriode(IKRAFT_FRA_DATO, OverføringÅrsak.ALENEOMSORG);

        assertThat(filtrer(periode)).containsExactly(periode);
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"SYKDOM", "INSTITUSJON_SØKER", "INSTITUSJON_BARN"})
    void skalBeholdeRelevantUtsettelse(UtsettelseÅrsak årsak) {
        var periode = lagPeriode(IKRAFT_FRA_DATO, årsak);

        assertThat(filtrer(periode)).containsExactly(periode);
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"ARBEID", "FERIE", "HV_OVELSE", "NAV_TILTAK", "UDEFINERT"})
    void skalFiltrereBortIrrelevantUtsettelse(UtsettelseÅrsak årsak) {
        var periode = lagPeriode(IKRAFT_FRA_DATO, årsak);
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        assertThat(filtrer(periode, uttak)).containsExactly(uttak);
    }

    @Test
    void skalFiltrereBortOppholdsperiode() {
        var periode = lagPeriode(IKRAFT_FRA_DATO, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        assertThat(filtrer(periode, uttak)).containsExactly(uttak);
    }

    @ParameterizedTest
    @EnumSource(value = MorsAktivitet.class, names = {"ARBEID", "IKKE_OPPGITT"})
    void skalVurdereUtsettelseUtFraMorsAktivitet(MorsAktivitet morsAktivitet) {
        var periode = OppgittPeriodeBuilder.fraEksisterende(lagPeriode(IKRAFT_FRA_DATO, UtsettelseÅrsak.ARBEID))
            .medMorsAktivitet(morsAktivitet)
            .build();
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        var forventet = MorsAktivitet.forventerDokumentasjon(morsAktivitet) ? List.of(periode, uttak) : List.of(uttak);
        assertThat(filtrer(periode, uttak)).containsExactlyElementsOf(forventet);
    }

    @Test
    void skalBeholdeFriUtsettelseNårSøknadenStarterMedFri() {
        var fri = lagPeriode(IKRAFT_FRA_DATO, UtsettelseÅrsak.FRI);
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        assertThat(filtrer(uttak, fri)).containsExactly(uttak, fri);
    }

    @Test
    void skalFiltrereBortFriUtsettelseSomIkkeErFørsteSøknadsperiode() {
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO);
        var fri = lagPeriode(IKRAFT_FRA_DATO.plusDays(2), UtsettelseÅrsak.FRI);

        assertThat(filtrer(uttak, fri)).containsExactly(uttak);
    }

    @Test
    void skalBrukeKandidatperiodeneNårAllePerioderFiltreresBort() {
        var irrelevantUtsettelse = lagPeriode(IKRAFT_FRA_DATO, UtsettelseÅrsak.ARBEID);
        var fri = lagPeriode(IKRAFT_FRA_DATO.plusDays(2), UtsettelseÅrsak.FRI);

        assertThat(filtrer(irrelevantUtsettelse, fri)).containsExactly(irrelevantUtsettelse, fri);
    }

    private static List<OppgittPeriodeEntitet> filtrer(OppgittPeriodeEntitet... perioder) {
        return ForeldrepengerUttakOversetter.filtrerPerioderSomSkalSaksbehandles(List.of(perioder));
    }

    private static OppgittPeriodeEntitet lagUttaksperiode(LocalDate fom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, fom.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
    }

    private static OppgittPeriodeEntitet lagPeriode(LocalDate fom, Årsak årsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, fom.plusDays(1))
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(årsak)
            .build();
    }
}
