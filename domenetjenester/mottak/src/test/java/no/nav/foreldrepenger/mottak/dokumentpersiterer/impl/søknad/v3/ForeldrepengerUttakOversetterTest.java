package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadDataFraTidligereVedtakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Uttaksperiodetyper;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Fordeling;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;

class ForeldrepengerUttakOversetterTest {

    private static final LocalDate IKRAFT_FRA_DATO = UtsettelseCore2021.IKRAFT_FRA_DATO;

    @ParameterizedTest
    @CsvSource({"2024-01-06, 2024-01-07", "2024-01-07, 2024-01-07", "2024-01-08, 2024-01-07", ","})
    void skalAvviseEndringssøknadUtenVirkedager(LocalDate fom, LocalDate tom) {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(1L);
        when(behandling.erRevurdering()).thenReturn(true);
        var repository = mock(YtelsesFordelingRepository.class);
        when(repository.opprettBuilder(1L)).thenReturn(YtelseFordelingAggregat.oppdatere(Optional.empty()));
        var tidligereVedtak = mock(SøknadDataFraTidligereVedtakTjeneste.class);
        var oversetter = new ForeldrepengerUttakOversetter(repository, null, null, tidligereVedtak);
        var fordeling = new Fordeling();
        fordeling.setAnnenForelderErInformert(true);
        if (fom != null) {
            var periode = new Uttaksperiode();
            periode.setFom(fom);
            periode.setTom(tom);
            var type = new Uttaksperiodetyper();
            type.setKode(UttakPeriodeType.FEDREKVOTE.getKode());
            periode.setType(type);
            fordeling.getPerioder().add(periode);
        }
        var søknad = new Endringssoeknad();
        søknad.setFordeling(fordeling);

        assertThatThrownBy(() -> oversetter.oversettForeldrepengerEndringssøknad(søknad, behandling, LocalDate.of(2024, 1, 8)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Fordelingen må inneholde perioder med minst en virkedag");
        verifyNoInteractions(tidligereVedtak);
    }

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
    void skalKonvertereFørsteUtsettelseTilFri(UtsettelseÅrsak årsak) {
        var periode = lagPeriode(IKRAFT_FRA_DATO, årsak);
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        assertThat(filtrer(periode, uttak)).satisfiesExactly(
            fri -> assertThat(fri.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI),
            resultatUttak -> assertThat(resultatUttak).isEqualTo(uttak));
    }

    @Test
    void skalKonvertereFørsteOppholdsperiodeTilFri() {
        var periode = lagPeriode(IKRAFT_FRA_DATO, OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        assertThat(filtrer(periode, uttak)).satisfiesExactly(
            fri -> assertThat(fri.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI),
            resultatUttak -> assertThat(resultatUttak).isEqualTo(uttak));
    }

    @ParameterizedTest
    @EnumSource(value = MorsAktivitet.class, names = {"ARBEID", "IKKE_OPPGITT"})
    void skalVurdereUtsettelseUtFraMorsAktivitet(MorsAktivitet morsAktivitet) {
        var periode = OppgittPeriodeBuilder.fraEksisterende(lagPeriode(IKRAFT_FRA_DATO, UtsettelseÅrsak.ARBEID))
            .medMorsAktivitet(morsAktivitet)
            .build();
        var uttak = lagUttaksperiode(IKRAFT_FRA_DATO.plusDays(2));

        assertThat(filtrer(periode, uttak)).satisfiesExactly(
            fri -> assertThat(fri.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI),
            resultatUttak -> assertThat(resultatUttak).isEqualTo(uttak));
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
    void skalBeholdeFørsteUtsettelseSomFriNårSenereFriFiltreresBort() {
        var irrelevantUtsettelse = lagPeriode(IKRAFT_FRA_DATO, UtsettelseÅrsak.ARBEID);
        var fri = lagPeriode(IKRAFT_FRA_DATO.plusDays(2), UtsettelseÅrsak.FRI);

        assertThat(filtrer(irrelevantUtsettelse, fri))
            .singleElement()
            .satisfies(resultat -> assertThat(resultat.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI));
    }

    @Test
    void skalKonvertereFerieTilFriNårSøknadenBareInneholderFerie() {
        var ferie = lagPeriode(IKRAFT_FRA_DATO, UtsettelseÅrsak.FERIE);

        assertThat(filtrer(ferie)).singleElement().satisfies(fri -> {
            assertThat(fri.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI);
            assertThat(fri.getFom()).isEqualTo(ferie.getFom());
            assertThat(fri.getTom()).isEqualTo(ferie.getTom());
        });
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
