package no.nav.foreldrepenger.web.app.tjenester.behandling;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.AktivitetskravDokumentasjonUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakPeriodeDtoTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BehandlingÅrsakDtoTest extends EntityManagerAwareTest {

    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup() {
        var stputil = new SkjæringstidspunktUtils();
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider));
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste,
            stputil, mock(UtsettelseBehandling2021.class), mock(MinsterettBehandling2022.class));
        var beregningtjeneste = mock(BeregningTjeneste.class);
        var opptjeningIUtlandDokStatusTjeneste = new OpptjeningIUtlandDokStatusTjeneste(
            new OpptjeningIUtlandDokStatusRepository(entityManager));
        var tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);
        var behandlingDokumentRepository = new BehandlingDokumentRepository(entityManager);
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var foreldrepengerUttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        var uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider, mock(HentOgLagreBeregningsgrunnlagTjeneste.class),
            new AbakusInMemoryInntektArbeidYtelseTjeneste(), skjæringstidspunktTjeneste, mock(MedlemTjeneste.class),
            new BeregningUttakTjeneste(foreldrepengerUttakTjeneste, repositoryProvider.getYtelsesFordelingRepository()));
        var dokumentasjonVurderingBehovDtoTjeneste = new DokumentasjonVurderingBehovDtoTjeneste(repositoryProvider.getBehandlingRepository(),
            uttakInputTjeneste, new VurderUttakDokumentasjonAksjonspunktUtleder(ytelseFordelingTjeneste, new AktivitetskravDokumentasjonUtleder(foreldrepengerUttakTjeneste)));
        var faktaUttakPeriodeDtoTjeneste = new FaktaUttakPeriodeDtoTjeneste(ytelseFordelingTjeneste, repositoryProvider.getBehandlingRepository(),
            repositoryProvider.getFpUttakRepository(), skjæringstidspunktTjeneste);

        behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningtjeneste,
            tilbakekrevingRepository, skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste,
            behandlingDokumentRepository, foreldrepengerUttakTjeneste, null, mock(TotrinnTjeneste.class),
            dokumentasjonVurderingBehovDtoTjeneste, faktaUttakPeriodeDtoTjeneste);
    }

    @Test
    void skal_teste_at_behandlingÅrsakDto_får_korrekte_verdier() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultFordeling(LocalDate.now());
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now())
            .medOpprinneligEndringsdato(LocalDate.now())
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)
            .medManueltOpprettet(true);
        behandlingÅrsak.buildFor(behandling);
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, null);

        var årsaker = dto.getBehandlingÅrsaker();

        assertThat(årsaker).isNotNull();
        assertThat(årsaker).hasSize(1);
        assertThat(årsaker.get(0).getBehandlingArsakType()).isEqualTo(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);
        assertThat(årsaker.get(0).isManueltOpprettet()).isTrue();

    }
}
