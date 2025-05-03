package no.nav.foreldrepenger.behandling.steg.vedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class BehandlingVedtakTjenesteTest extends EntityManagerAwareTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository fpUttakRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @BeforeEach
    public void setUp() {
        var behandlingVedtakEventPubliserer = mock(BehandlingEventPubliserer.class);
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingVedtakTjeneste = new BehandlingVedtakTjeneste(behandlingVedtakEventPubliserer, repositoryProvider);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fpUttakRepository = repositoryProvider.getFpUttakRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    @Test
    void skal_opprette_behandlingsvedtak_for_revurdering_med_opphør() {
        // Arrange
        var originalBehandling = lagInnvilgetOriginalBehandling();
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL).medOriginalBehandlingId(originalBehandling.getId()))
                .build();
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.FATTE_VEDTAK);
        var behandlingLås = lagreBehandling(revurdering);
        opprettFamilieHendelseGrunnlag(originalBehandling, revurdering);
        var revurderingKontekst = new BehandlingskontrollKontekst(revurdering, behandlingLås);
        oppdaterMedBehandlingsresultat(revurderingKontekst, BehandlingResultatType.OPPHØR);
        lagUttaksresultatOpphørEtterSkjæringstidspunkt(revurdering);

        // Act
        behandlingVedtakTjeneste.opprettBehandlingVedtak(revurderingKontekst, revurdering);

        // Assert
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(revurdering.getId());
        assertThat(vedtak).isPresent();
        assertThat(vedtak.get().getVedtakResultatType()).isEqualTo(VedtakResultatType.OPPHØR);
    }

    private Behandling lagInnvilgetOriginalBehandling() {
        var kontekst = byggBehandlingsgrunnlagFPForFødsel(BehandlingStegType.FATTE_VEDTAK);
        oppdaterMedBehandlingsresultat(kontekst, BehandlingResultatType.INNVILGET);
        return behandlingRepository.hentBehandling(kontekst.getBehandlingId());
    }

    private void lagUttaksresultatOpphørEtterSkjæringstidspunkt(Behandling revurdering) {
        var uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        uttakResultatPerioderEntitet.leggTilPeriode(lagInnvilgetUttakPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(1)));
        uttakResultatPerioderEntitet
                .leggTilPeriode(lagOpphørtPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1), SKJÆRINGSTIDSPUNKT.plusMonths(6)));
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(revurdering.getId(), uttakResultatPerioderEntitet);
    }

    private UttakResultatPeriodeEntitet lagInnvilgetUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER).build();
    }

    private UttakResultatPeriodeEntitet lagOpphørtPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
                .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.opphørsAvslagÅrsaker().iterator().next()).build();
    }

    private void opprettFamilieHendelseGrunnlag(Behandling originalBehandling, Behandling revurdering) {
        repositoryProvider.getFamilieHendelseRepository().kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), revurdering.getId());
    }

    private BehandlingLås lagreBehandling(Behandling behandling) {
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandlingLås;
    }

    private void oppdaterMedBehandlingsresultat(BehandlingskontrollKontekst kontekst, BehandlingResultatType behandlingResultatType) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        Behandlingsresultat.builderForInngangsvilkår()
                .medBehandlingResultatType(behandlingResultatType)
                .buildFor(behandling);
        var ikkeAvslått = !behandlingResultatType.equals(BehandlingResultatType.AVSLÅTT);
        var builder = VilkårResultat.builder();
        if (ikkeAvslått) {
            builder.leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR);
        } else {
            builder.leggTilVilkårAvslått(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallMerknad.VM_1026);
        }
        builder.buildFor(behandling);

        var lås = kontekst.getSkriveLås();
        var behandlingsresultat = behandling.getBehandlingsresultat();
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);

        behandlingRepository.lagre(behandling, lås);
    }

    private BehandlingskontrollKontekst byggBehandlingsgrunnlagFPForFødsel(BehandlingStegType behandlingStegType) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBekreftetHendelse().medFødselsDato(LocalDate.now())
                .medAntallBarn(1);

        var behandling = scenario
                .medBehandlingStegStart(behandlingStegType)
                .medBehandlendeEnhet("Stord")
                .lagre(repositoryProvider);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        return new BehandlingskontrollKontekst(behandling, behandlingRepository.taSkriveLås(behandling));
    }
}
