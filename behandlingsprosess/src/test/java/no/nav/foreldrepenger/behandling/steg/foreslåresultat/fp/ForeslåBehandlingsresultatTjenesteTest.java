package no.nav.foreldrepenger.behandling.steg.foreslåresultat.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingBehandlingsresultatutleder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

class ForeslåBehandlingsresultatTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;

    private final DokumentBehandlingTjeneste dokumentBehandlingTjeneste = mock(DokumentBehandlingTjeneste.class);
    private final OpphørUttakTjeneste opphørUttakTjeneste = mock(OpphørUttakTjeneste.class);
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private ForeslåBehandlingsresultatTjenesteImpl tjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository fpUttakRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private final MedlemTjeneste medlemTjeneste = mock(MedlemTjeneste.class);
    private final BeregningTjeneste beregningTjeneste = mock(BeregningTjeneste.class);

    @BeforeEach
    public void setup() {
        when(medlemTjeneste.utledVilkårUtfall(any())).thenReturn(new MedlemTjeneste.VilkårUtfallMedÅrsak(VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.empty());
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        fpUttakRepository = this.repositoryProvider.getFpUttakRepository();
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(fpUttakRepository);
        revurderingBehandlingsresultatutleder = spy(new RevurderingBehandlingsresultatutleder(repositoryProvider,
                grunnlagRepositoryProvider,
                beregningTjeneste,
                opphørUttakTjeneste,
                skjæringstidspunktTjeneste,
                medlemTjeneste,
                uttakTjeneste, new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository())));
        tjeneste = new ForeslåBehandlingsresultatTjenesteImpl(this.repositoryProvider,
                new ForeldrepengerUttakTjeneste(this.repositoryProvider.getFpUttakRepository()),
                dokumentBehandlingTjeneste,
                revurderingBehandlingsresultatutleder);
        behandlingRepository = this.repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = this.repositoryProvider.getBehandlingVedtakRepository();
    }

    @Test
    void skalSetteBehandlingsresultatInnvilgetNårVilkårOppfylt() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, VilkårUtfallType.OPPFYLT);

        // Act
        var behandlingsresultat = foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
    }

    private Behandlingsresultat foreslåBehandlingsresultat(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        return tjeneste.foreslåBehandlingsresultat(ref);
    }

    @Test
    void skalFjerneAvslagsårsakNårInnvilget() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, VilkårUtfallType.OPPFYLT);
        getBehandlingsresultat(behandling).setAvslagsårsak(Avslagsårsak.MANGLENDE_DOKUMENTASJON);

        // Act
        var behandlingsresultat = foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getAvslagsårsak()).isNull();
    }

    @Test
    void skalKalleBestemBehandlingsresultatForRevurderingNårInnvilgetRevurdering() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, VilkårUtfallType.OPPFYLT);

        // Act
        foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(ArgumentMatchers.any(), anyBoolean());
    }

    @Test
    void skalSetteBehandlingsresultatAvslåttNårVilkårAvslåttFørstegangsbehandling() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        lagBehandlingsresultat(behandling);

        // Act
        var behandlingsresultat = foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
    }

    @Test
    void skalKalleBestemBehandlingsresultatNårAvslåttRevurdering() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, VilkårUtfallType.IKKE_OPPFYLT);

        // Act
        foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), anyBoolean());
    }

    @Test
    void skalSetteBehandlingsresultatAvslåttNårVilkårAvslåttFørstegangsbehandlingInfotrygd() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        behandling.getFagsak().setStengt(true);
        lagBehandlingsresultat(behandling);

        // Act
        var behandlingsresultat = foreslåBehandlingsresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
    }

    @Test
    void skalKalleBestemBehandlingsresultatNårVilkårAvslåttRevurderingInfotrygd() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, VilkårUtfallType.IKKE_OPPFYLT);
        revurdering.getFagsak().setStengt(true);

        // Act
        foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), anyBoolean());
    }

    @Test
    void skalKalleBestemBehandlingsresultatNårAvslåttRevurderingPåAvslåttFørstegangsbehandling() {

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, VilkårUtfallType.IKKE_OPPFYLT);
        foreslåBehandlingsresultat(behandling);
        behandling.avsluttBehandling();
        var revurdering = lagRevurdering(behandling);

        // Act
        var behandlingsresultat = foreslåBehandlingsresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(any(), anyBoolean());

        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medKopiAvForrigeBehandlingsresultat()
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                                .medManueltOpprettet(true)
                                .medOriginalBehandlingId(originalBehandling.getId()))
                .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(LocalDate.now())
            .build();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
            .medAvklarteDatoer(avklarteUttakDatoer)
            .build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), ytelseFordelingAggregat);
        return revurdering;
    }

    private void inngangsvilkårOgUttak(Behandling behandling, VilkårUtfallType vilkårUtfallType) {
        var lås = behandlingRepository.taSkriveLås(behandling);

        var vilkårsresultatBuilder = VilkårResultat.builder();
        if (vilkårUtfallType.equals(VilkårUtfallType.OPPFYLT)) {
            vilkårsresultatBuilder.leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR);
            vilkårsresultatBuilder.leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET);
        } else {
            vilkårsresultatBuilder.manueltVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.IKKE_OPPFYLT,
                    Avslagsårsak.FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT);
        }
        behandlingRepository.lagre(vilkårsresultatBuilder.buildFor(behandling), lås);
        behandlingRepository.lagre(behandling, lås);
        if (vilkårUtfallType.equals(VilkårUtfallType.OPPFYLT)) {
            lagreUttak(behandling);
        }
    }

    private void lagreUttak(Behandling behandling) {
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();
        var uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(6))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);
    }

    private void lagBehandlingsresultat(Behandling behandling) {
        var behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
                .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
                .buildFor(behandling);
        var behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now())
                .medBehandlingsresultat(behandlingsresultat)
                .medVedtakResultatType(VedtakResultatType.AVSLAG).medAnsvarligSaksbehandler("asdf").build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(getBehandlingsresultat(behandling).getVilkårResultat(), behandlingRepository.taSkriveLås(behandling));
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }
}
