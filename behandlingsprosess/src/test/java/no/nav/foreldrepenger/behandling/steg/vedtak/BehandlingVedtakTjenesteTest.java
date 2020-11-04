package no.nav.foreldrepenger.behandling.steg.vedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
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
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingVedtakTjenesteTest extends EntityManagerAwareTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private Repository repository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository fpUttakRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @BeforeEach
    public void setUp() {
        BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer = mock(BehandlingVedtakEventPubliserer.class);
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingVedtakTjeneste = new BehandlingVedtakTjeneste(behandlingVedtakEventPubliserer, repositoryProvider);
        repository = new Repository(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fpUttakRepository = repositoryProvider.getFpUttakRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Test
    public void skal_opprette_behandlingsvedtak_for_revurdering_med_opphør() {
        // Arrange
        Behandling originalBehandling = lagInnvilgetOriginalBehandling();
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId())).build();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        BehandlingLås behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingskontrollKontekst revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, BehandlingResultatType.OPPHØR);
        lagUttaksresultatOpphørEtterSkjæringstidspunkt(revurdering);

        // Act
        behandlingVedtakTjeneste.opprettBehandlingVedtak(revurderingKontekst, revurdering);

        // Assert
        Optional<BehandlingVedtak> vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(revurdering.getId());
        assertThat(vedtak).isPresent();
        assertThat(vedtak.get().getVedtakResultatType()).isEqualTo(VedtakResultatType.OPPHØR);
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
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatPerioderEntitet);
    }

    private UttakResultatPeriodeEntitet lagInnvilgetUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT).build();
    }

    private UttakResultatPeriodeEntitet lagOpphørtPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.opphørsAvslagÅrsaker().iterator().next()).build();
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
