package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles.erAnnulleringAvUttak;
import static no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles.erAvslagPåAvslag;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;

import no.nav.foreldrepenger.domene.uttak.SvangerskapspengerUttak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(JpaExtension.class)
class RevurderingBehandlingsresultatutlederFellesTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_ikke_gi_avslag_på_avslag() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        // Act
        var erAvslagPåAvslag = erAvslagPåAvslag(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
                KonsekvensForYtelsen.INGEN_ENDRING, VilkårUtfallType.OPPFYLT),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.INNVILGET,
                KonsekvensForYtelsen.ENDRING_I_UTTAK, VilkårUtfallType.OPPFYLT));

        // Assert
        assertThat(erAvslagPåAvslag).isFalse();
    }

    @Test
    void skal_gi_avslag_på_avslag() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        // Act
        var erAvslagPåAvslag = erAvslagPåAvslag(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
                KonsekvensForYtelsen.INGEN_ENDRING, VilkårUtfallType.IKKE_OPPFYLT),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.AVSLÅTT, KonsekvensForYtelsen.ENDRING_I_UTTAK, VilkårUtfallType.IKKE_OPPFYLT));

        // Assert
        assertThat(erAvslagPåAvslag).isTrue();
    }

    private Behandling opprettOriginalBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var originalBehandling = scenario.lagre(repositoryProvider);
        originalBehandling.avsluttBehandling();
        return originalBehandling;
    }

    private Behandlingsresultat lagBehandlingsresultat(Behandling behandling, BehandlingResultatType resultatType,
                                                                 KonsekvensForYtelsen konsekvensForYtelsen, VilkårUtfallType utfallType) {
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(resultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen).buildFor(behandling);

        VilkårResultat.builder()
            .overstyrVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, utfallType,
                VilkårUtfallType.IKKE_OPPFYLT.equals(utfallType) ? Avslagsårsak.MANGLENDE_DOKUMENTASJON : Avslagsårsak.UDEFINERT)
            .buildFor(behandling);
        behandlingRepository.lagre(behandling.getBehandlingsresultat().getVilkårResultat(),
            behandlingRepository.taSkriveLås(behandling));

        return behandlingsresultat;
    }

    @Test
    void skal_returnere_true_når_uttak_er_tomt_med_riktig_årsak() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);

        // Act
        var resultat = erAnnulleringAvUttak(Optional.empty(), revurdering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_returnere_false_når_feil_årsak_satt() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.REBEREGN_FERIEPENGER);

        // Act
        var resultat = erAnnulleringAvUttak(Optional.empty(), revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_returnere_true_når_uttak_har_perioder_uten_utbetaling_og_trekk_med_riktig_årsak() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);

        var foreldrepengerUttak = new ForeldrepengerUttak(List.of(lagPeriode(List.of(lagAktivitet(Utbetalingsgrad.ZERO, Trekkdager.ZERO)))));

        // Act
        var resultat = erAnnulleringAvUttak(Optional.of(foreldrepengerUttak), revurdering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_returnere_false_når_uttak_har_perioder_med_utbetaling_og_trekk_med_riktig_årsak() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);

        var foreldrepengerUttak = new ForeldrepengerUttak(List.of(lagPeriode(List.of(lagAktivitet(Utbetalingsgrad.HUNDRED, Trekkdager.ZERO)))));

        // Act
        var resultat = erAnnulleringAvUttak(Optional.of(foreldrepengerUttak), revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_returnere_false_når_uttak_har_perioder_uten_utbetaling_men_med_trekk_og_riktig_årsak() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER);

        var foreldrepengerUttak = new ForeldrepengerUttak(List.of(lagPeriode(List.of(lagAktivitet(Utbetalingsgrad.ZERO, new Trekkdager(BigDecimal.TEN))))));

        // Act
        var resultat = erAnnulleringAvUttak(Optional.of(foreldrepengerUttak), revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_returnere_false_hvis_ikke_foreldrepenger_uttak() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER);

        var svangerskapspengerUttak = new SvangerskapspengerUttak(null);

        // Act
        var resultat = erAnnulleringAvUttak(Optional.of(svangerskapspengerUttak), revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    private static ForeldrepengerUttakPeriode lagPeriode(List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        return new ForeldrepengerUttakPeriode.Builder()
            .medResultatÅrsak(PeriodeResultatÅrsak.SØKNADSFRIST)
            .medTidsperiode(LocalDate.now(), LocalDate.now().plusDays(7))
            .medAktiviteter(aktiviteter)
            .build();
    }

    private static ForeldrepengerUttakPeriodeAktivitet lagAktivitet(Utbetalingsgrad utbetalingsgrad, Trekkdager trekkdager) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, null, null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(utbetalingsgrad)
            .medTrekkdager(trekkdager)
            .build();
    }

    @Test
    void skal_returnere_true_når_uttak_er_null_med_riktig_årsak() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        // Act
        var resultat = erAnnulleringAvUttak(Optional.empty(), revurdering);

        // Assert
        assertThat(resultat).isTrue();
    }

    private Behandling lagRevurdering(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsak) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(behandlingÅrsak)
                    .medManueltOpprettet(true)
                    .medOriginalBehandlingId(originalBehandling.getId()))
            .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }

}
