package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.vedtak.exception.TekniskException;

@CdiDbAwareTest
class RevurderingEndringTest {
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private LegacyESBeregningRepository beregningRepository;

    @Test
    void erRevurderingMedUendretUtfall() {
        var originalBehandling = opprettFørstegangsBehandling(1L, BehandlingResultatType.INNVILGET);
        var revurderingTjeneste = tjeneste();

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var revurdering = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);

        opprettBeregning(revurdering);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isTrue();
    }

    @Test
    void erRevurderingMedUendretUtfallEksplittBehandlingstype() {
        var originalBehandling = opprettFørstegangsBehandling(1L, BehandlingResultatType.INNVILGET);
        var revurderingEndring = new RevurderingEndringImpl(repositoryProvider.getBehandlingRepository(),
            beregningRepository, repositoryProvider.getBehandlingsresultatRepository());

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var revurdering = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ANNET)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);

        opprettBeregning(revurdering);

        assertThat(
            revurderingEndring.erRevurderingMedUendretUtfall(revurdering, BehandlingResultatType.INNVILGET)).isTrue();
    }

    @Test
    void erRevurderingMedEndretAntallBarn() {
        var originalBehandling = opprettFørstegangsBehandling(2L, BehandlingResultatType.INNVILGET);
        var revurderingTjeneste = tjeneste();

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var revurdering = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ANNET)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);

        opprettBeregning(revurdering);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    @Test
    void erRevurderingDerBeggeErAvslått() {
        var originalBehandling = opprettFørstegangsBehandling(2L, BehandlingResultatType.AVSLÅTT);
        var revurderingTjeneste = tjeneste();

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        var revurdering = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ANNET)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);

        opprettBeregning(revurdering);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isTrue();
    }

    @Test
    void skal_gi_false_dersom_behandling_ikke_er_revurdering() {
        var originalBehandling = opprettFørstegangsBehandling(2L, BehandlingResultatType.AVSLÅTT);
        var revurderingTjeneste = tjeneste();

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(originalBehandling)).isFalse();
    }

    @Test
    void skal_gi_feil_dersom_revurdering_ikke_har_original_behandling() {
        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingsresultat(behandlingsresultat)
            .medOriginalBehandling(null, BehandlingÅrsakType.RE_ANNET)
            .lagre(repositoryProvider);
        var revurderingTjeneste = tjeneste();

        opprettBeregning(behandling);

        assertThrows(TekniskException.class, () -> revurderingTjeneste.erRevurderingMedUendretUtfall(behandling));
    }

    private void opprettBeregning(Behandling behandling) {
        var beregningResultat = opprettBeregning(behandling, 1L);
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        beregningRepository.lagre(beregningResultat, lås);
    }

    private RevurderingTjeneste tjeneste() {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, FagsakYtelseType.ENGANGSTØNAD).orElseThrow();
    }

    @Test
    void erRevurderingMedEndretResultatFraInnvilgetTilAvslått() {
        var originalBehandling = opprettFørstegangsBehandling(1L, BehandlingResultatType.INNVILGET);
        var revurderingTjeneste = tjeneste();

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        var revurdering = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    @Test
    void erRevurderingMedEndretResultatFraAvslåttTilInnvilget() {
        var originalBehandling = opprettFørstegangsBehandling(1L, BehandlingResultatType.AVSLÅTT);
        var revurderingTjeneste = tjeneste();

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var revurdering = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL)
            .medBehandlingsresultat(behandlingsresultat)
            .lagre(repositoryProvider);

        opprettBeregning(revurdering);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    private Behandling opprettFørstegangsBehandling(Long antallBarn, BehandlingResultatType behandlingResultatType) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType))
            .medDefaultBekreftetTerminbekreftelse();
        var behandling = scenario.lagre(repositoryProvider);

        var behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);

        if (behandlingResultatType.equals(BehandlingResultatType.INNVILGET)) {
            var originalBeregning = opprettBeregning(behandling, antallBarn);
            beregningRepository.lagre(originalBeregning, behandlingLås);
        }
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        var originalVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(behandlingResultatType.equals(
                BehandlingResultatType.INNVILGET) ? VedtakResultatType.INNVILGET : VedtakResultatType.AVSLAG)
            .medAnsvarligSaksbehandler("asdf")
            .build();

        repositoryProvider.getBehandlingVedtakRepository().lagre(originalVedtak, behandlingLås);
        return behandling;
    }

    private LegacyESBeregningsresultat opprettBeregning(Behandling behandling, long antallBarn) {
        var beregning = new LegacyESBeregning(behandling.getId(), 1000L, antallBarn, antallBarn * 1000, LocalDateTime.now());
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        return LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, behandlingsresultat);
    }

}
