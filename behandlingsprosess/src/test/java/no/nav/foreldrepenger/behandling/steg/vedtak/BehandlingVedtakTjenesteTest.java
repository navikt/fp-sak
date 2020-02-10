package no.nav.foreldrepenger.behandling.steg.vedtak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingVedtakTjenesteTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final InternalManipulerBehandling manipulerBehandling = new InternalManipulerBehandling();
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private UttakRepository uttakRepository = repositoryProvider.getUttakRepository();
    private BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();

    @Before
    public void setUp() {
        BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer = mock(BehandlingVedtakEventPubliserer.class);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        OpphørUttakTjeneste opphørUttakTjeneste = new OpphørUttakTjeneste(new UttakRepositoryProvider(repositoryProvider.getEntityManager()));
        behandlingVedtakTjeneste = new BehandlingVedtakTjeneste(behandlingVedtakEventPubliserer, repositoryProvider, opphørUttakTjeneste, skjæringstidspunktTjeneste);
    }

    // Tester behandlingsresultattype OPPHØR med opphør etter skjæringstidspunkt
    @Test
    public void skal_opprette_behandlingsvedtak_for_revurdering_med_opphør_etter_skjæringstidspunkt() {
        // Arrange
        Behandling originalBehandling = lagInnvilgetOriginalBehandling();
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();
        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, BehandlingResultatType.OPPHØR);
        lagUttaksresultatOpphørEtterSkjæringstidspunkt(revurdering);

        // Act
        behandlingVedtakTjeneste.opprettBehandlingVedtak(revurderingKontekst, revurdering);

        // Assert
        Optional<BehandlingVedtak> vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(revurdering.getId());
        assertThat(vedtak).isPresent();
        assertThat(vedtak.get().getVedtakResultatType()).isEqualTo(VedtakResultatType.INNVILGET);
    }

    // Tester behandlingsresultattype opphør for opphør på skjæringstidspunkt
    @Test
    public void skal_opprette_behandlingsvedtak_for_revurdering_med_opphør_på_skjæringstidspunkt() {
        // Arrange
        Behandling originalBehandling = lagInnvilgetOriginalBehandling();
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandling(originalBehandling)).build();
        manipulerBehandling.forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, BehandlingResultatType.OPPHØR);
        lagUttaksresultatOpphørPåSkjæringstidspunkt(revurdering);

        // Act
        behandlingVedtakTjeneste.opprettBehandlingVedtak(revurderingKontekst, revurdering);

        // Assert
        Optional<BehandlingVedtak> vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(revurdering.getId());
        assertThat(vedtak).isPresent();
        assertThat(vedtak.get().getVedtakResultatType()).isEqualTo(VedtakResultatType.AVSLAG);
    }

    private Behandling lagInnvilgetOriginalBehandling() {
        BehandlingskontrollKontekst kontekst = byggBehandlingsgrunnlagFPForFødsel(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(kontekst, BehandlingResultatType.INNVILGET);
        return behandlingRepository.hentBehandling(kontekst.getBehandlingId());
    }

    private void lagUttaksresultatOpphørEtterSkjæringstidspunkt(Behandling revurdering) {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(lagInnvilgetUttakPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(1)));
        uttakResultatPerioderEntitet.leggTilPeriode(lagOpphørtPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1), SKJÆRINGSTIDSPUNKT.plusMonths(6)));
        uttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatPerioderEntitet);
    }

    private void lagUttaksresultatOpphørPåSkjæringstidspunkt(Behandling revurdering) {
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(lagOpphørtPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(6)));
        uttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatPerioderEntitet);
    }


    private UttakResultatPeriodeEntitet lagInnvilgetUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT).build();
    }

    private UttakResultatPeriodeEntitet lagOpphørtPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.opphørsAvslagÅrsaker().iterator().next()).build();
    }

    private void opprettFamilieHendelseGrunnlag(Behandling originalBehandling, Behandling revurdering) {
        repositoryProvider.getFamilieHendelseRepository().kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
    }

    private BehandlingLås lagreBehandling(Behandling behandling) {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandlingLås;
    }


    private void oppdaterMedBehandlingsresultat(BehandlingskontrollKontekst kontekst, BehandlingResultatType behandlingResultatType) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        Behandlingsresultat.builderForInngangsvilkår()
            .medBehandlingResultatType(behandlingResultatType)
            .buildFor(behandling);
        boolean ikkeAvslått = !behandlingResultatType.equals(BehandlingResultatType.AVSLÅTT);
        VilkårResultat.builder()
            .leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, ikkeAvslått ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT,
                null, new Properties(), null, false, false, null, null)
            .medVilkårResultatType(ikkeAvslått ? VilkårResultatType.INNVILGET : VilkårResultatType.AVSLÅTT)
            .buildFor(behandling);

        BehandlingLås lås = kontekst.getSkriveLås();
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);

        behandlingRepository.lagre(behandling, lås);
        repository.flush();
    }


    private BehandlingskontrollKontekst byggBehandlingsgrunnlagFPForFødsel(BehandlingStegType behandlingStegType) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBekreftetHendelse().medFødselsDato(LocalDate.now())
            .medAntallBarn(1);

        Behandling behandling = scenario
            .medBehandlingStegStart(behandlingStegType)
            .medBehandlendeEnhet("Stord")
            .lagre(repositoryProvider);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        Fagsak fagsak = behandling.getFagsak();
        return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

}
