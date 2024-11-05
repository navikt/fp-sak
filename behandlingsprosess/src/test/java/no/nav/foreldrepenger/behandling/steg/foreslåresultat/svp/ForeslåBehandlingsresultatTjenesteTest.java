package no.nav.foreldrepenger.behandling.steg.foreslåresultat.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjenesteImpl;
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
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.SvangerskapspengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;

class ForeslåBehandlingsresultatTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private final BeregningTjeneste beregningTjeneste = mock(BeregningTjeneste.class);
    private final DokumentBehandlingTjeneste dokumentBehandlingTjeneste = mock(DokumentBehandlingTjeneste.class);
    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder;
    private ForeslåBehandlingsresultatTjenesteImpl tjeneste;
    private BehandlingRepository behandlingRepository;
    private final MedlemTjeneste medlemTjeneste = mock(MedlemTjeneste.class);

    private BehandlingVedtakRepository behandlingVedtakRepository;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        var uttakTjeneste = new UttakTjeneste(behandlingRepository, null,
            new SvangerskapspengerUttakTjeneste(repositoryProvider.getSvangerskapspengerUttakResultatRepository()));
        revurderingBehandlingsresultatutleder = spy(
            new RevurderingBehandlingsresultatutlederFelles(repositoryProvider, grunnlagRepositoryProvider, beregningTjeneste, medlemTjeneste,
                mock(DekningsgradTjeneste.class), uttakTjeneste));

        tjeneste = new ForeslåBehandlingsresultatTjenesteImpl(repositoryProvider, uttakTjeneste,
            dokumentBehandlingTjeneste, revurderingBehandlingsresultatutleder);
    }

    @Test
    void skalSetteBehandlingsresultatInnvilgetNårVilkårOppfylt() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, VilkårUtfallType.OPPFYLT);

        // Act
        var behandlingsresultat = foreslåBehandlingresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
    }

    @Test
    void skalFjerneAvslagsårsakNårInnvilget() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, VilkårUtfallType.OPPFYLT);
        getBehandlingsresultat(behandling).setAvslagsårsak(Avslagsårsak.MANGLENDE_DOKUMENTASJON);

        // Act
        var behandlingsresultat = foreslåBehandlingresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getAvslagsårsak()).isNull();
    }

    @Test
    void skalKalleBestemBehandlingsresultatForRevurderingNårInnvilgetRevurdering() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        var revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, VilkårUtfallType.OPPFYLT);

        // Act
        foreslåBehandlingresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(ArgumentMatchers.any(), anyBoolean());
    }

    @Test
    void skalSetteBehandlingsresultatAvslåttNårVilkårAvslåttFørstegangsbehandling() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        lagBehandlingsresultat(behandling);

        // Act
        var behandlingsresultat = foreslåBehandlingresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
    }

    @Test
    void skalKalleBestemBehandlingsresultatNårAvslåttRevurdering() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        var revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, VilkårUtfallType.IKKE_OPPFYLT);

        // Act
        foreslåBehandlingresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(ArgumentMatchers.any(), anyBoolean());
    }

    @Test
    void skalSetteBehandlingsresultatAvslåttNårVilkårAvslåttFørstegangsbehandlingInfotrygd() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        behandling.getFagsak().setStengt(true);
        lagBehandlingsresultat(behandling);

        // Act
        var behandlingsresultat = foreslåBehandlingresultat(behandling);

        // Assert
        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.AVSLÅTT);
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
    }

    @Test
    void skalKalleBestemBehandlingsresultatNårVilkårAvslåttRevurderingInfotrygd() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        var revurdering = lagRevurdering(behandling);
        inngangsvilkårOgUttak(revurdering, VilkårUtfallType.IKKE_OPPFYLT);
        revurdering.getFagsak().setStengt(true);

        // Act
        foreslåBehandlingresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(ArgumentMatchers.any(), anyBoolean());
    }

    @Test
    void skalKalleBestemBehandlingsresultatNårAvslåttRevurderingPåAvslåttFørstegangsbehandling() {

        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        inngangsvilkårOgUttak(behandling, VilkårUtfallType.IKKE_OPPFYLT);
        foreslåBehandlingresultat(behandling);
        behandling.avsluttBehandling();
        var revurdering = lagRevurdering(behandling);

        // Act
        var behandlingsresultat = foreslåBehandlingresultat(revurdering);

        // Assert
        verify(revurderingBehandlingsresultatutleder).bestemBehandlingsresultatForRevurdering(ArgumentMatchers.any(), anyBoolean());

        assertThat(behandlingsresultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
    }

    private Behandlingsresultat foreslåBehandlingresultat(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        return tjeneste.foreslåBehandlingsresultat(ref);
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
        return revurdering;
    }

    private void inngangsvilkårOgUttak(Behandling behandling, VilkårUtfallType vilkårUtfallType) {
        var lås = behandlingRepository.taSkriveLås(behandling);

        var vilkårsresultatBuilder = VilkårResultat.builder();
        if (vilkårUtfallType.equals(VilkårUtfallType.OPPFYLT)) {
            vilkårsresultatBuilder.leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET);
            vilkårsresultatBuilder.leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET);
        } else {
            vilkårsresultatBuilder.manueltVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT,
                    Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING);
        }
        behandlingRepository.lagre(vilkårsresultatBuilder.buildFor(behandling), lås);
        behandlingRepository.lagre(behandling, lås);
        if (vilkårUtfallType.equals(VilkårUtfallType.OPPFYLT)) {
            lagreUttak(behandling);
        }
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

    public void lagreUttak(Behandling behandling) {

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
                .medRegelInput("{}")
                .medRegelEvaluering("{}")
                .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
                .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
                .build();

        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
                .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medPeriode(uttakPeriode)
                .build();
        var uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(behandling.getBehandlingsresultat())
                .medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttakResultat);
    }

}
