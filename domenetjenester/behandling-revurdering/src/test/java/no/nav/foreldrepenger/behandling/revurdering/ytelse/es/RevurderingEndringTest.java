package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class RevurderingEndringTest {

    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private LegacyESBeregningRepository beregningRepository = new LegacyESBeregningRepository(repositoryRule.getEntityManager());

    @Test
    public void erRevurderingMedUendretUtfall() {
        Behandling originalBehandling = opprettOriginalBehandling(1L, BehandlingResultatType.INNVILGET);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        LegacyESBeregningsresultat beregningResultat = opprettBeregning(revurdering, 1L);
        beregningRepository.lagre(beregningResultat, lås);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isTrue();
    }


    @Test
    public void erRevurderingMedUendretUtfallEksplittBehandlingstype() {
        Behandling originalBehandling = opprettOriginalBehandling(1L, BehandlingResultatType.INNVILGET);
        RevurderingEndring revurderingEndring = new RevurderingEndringImpl(beregningRepository);

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        LegacyESBeregningsresultat beregningResultat = opprettBeregning(revurdering, 1L);
        beregningRepository.lagre(beregningResultat, lås);

        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(revurdering, BehandlingResultatType.INNVILGET)).isTrue();
    }

    @Test
    public void erRevurderingMedEndretAntallBarn() {
        Behandling originalBehandling = opprettOriginalBehandling(2L, BehandlingResultatType.INNVILGET);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();


        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        LegacyESBeregningsresultat beregningResultat = opprettBeregning(revurdering, 1L);
        beregningRepository.lagre(beregningResultat, lås);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    @Test
    public void erRevurderingDerBeggeErAvslått() {
        Behandling originalBehandling = opprettOriginalBehandling(2L, BehandlingResultatType.AVSLÅTT);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        LegacyESBeregningsresultat beregningResultat = opprettBeregning(revurdering, 1L);
        beregningRepository.lagre(beregningResultat, lås);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isTrue();
    }

    @Test
    public void skal_gi_false_dersom_behandling_ikke_er_revurdering() {
        Behandling originalBehandling = opprettOriginalBehandling(2L, BehandlingResultatType.AVSLÅTT);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(originalBehandling)).isFalse();
    }

    @Test(expected = TekniskException.class)
    public void skal_gi_feil_dersom_revurdering_ikke_har_original_behandling() {
        Behandling originalBehandling = opprettOriginalBehandling(2L, BehandlingResultatType.AVSLÅTT);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(null)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        LegacyESBeregningsresultat beregningResultat = opprettBeregning(revurdering, 1L);
        beregningRepository.lagre(beregningResultat, lås);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isTrue();
    }

    @Test
    public void erRevurderingMedEndretResultatFraInnvilgetTilAvslått() {
        Behandling originalBehandling = opprettOriginalBehandling(1L, BehandlingResultatType.INNVILGET);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();


        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    @Test
    public void erRevurderingMedEndretResultatFraAvslåttTilInnvilget() {
        Behandling originalBehandling = opprettOriginalBehandling(1L, BehandlingResultatType.AVSLÅTT);
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, originalBehandling.getFagsak().getYtelseType()).orElseThrow();

        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(revurdering);
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        LegacyESBeregningsresultat beregningResultat = opprettBeregning(revurdering, 1L);
        beregningRepository.lagre(beregningResultat, lås);

        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }


    private Behandling opprettOriginalBehandling(Long antallBarn, BehandlingResultatType behandlingResultatType) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medDefaultBekreftetTerminbekreftelse();
        Behandling originalBehandling = scenario.lagre(repositoryProvider);
        Behandlingsresultat originalResultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .buildFor(originalBehandling);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(originalBehandling);
        behandlingRepository.lagre(originalBehandling, behandlingLås);

        if (behandlingResultatType.equals(BehandlingResultatType.INNVILGET)) {
            LegacyESBeregningsresultat originalBeregning = opprettBeregning(originalBehandling, antallBarn);
            beregningRepository.lagre(originalBeregning, behandlingLås);
        }
        BehandlingVedtak originalVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(originalResultat)
            .medVedtakResultatType(behandlingResultatType.equals(BehandlingResultatType.INNVILGET) ?
                VedtakResultatType.INNVILGET : VedtakResultatType.AVSLAG)
            .medAnsvarligSaksbehandler("asdf")
            .build();

        repositoryProvider.getBehandlingVedtakRepository().lagre(originalVedtak, behandlingLås);
        return originalBehandling;
    }

    private LegacyESBeregningsresultat opprettBeregning(Behandling behandling, long antallBarn) {
        LegacyESBeregning beregning = new LegacyESBeregning(1000L, antallBarn, antallBarn * 1000, LocalDateTime.now());
        return LegacyESBeregningsresultat.builder().medBeregning(beregning).buildFor(behandling, behandling.getBehandlingsresultat());
    }

}
