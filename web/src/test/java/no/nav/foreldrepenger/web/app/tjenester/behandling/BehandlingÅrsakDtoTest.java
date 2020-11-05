package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingÅrsakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;

public class BehandlingÅrsakDtoTest extends EntityManagerAwareTest {

    private Behandling behandling;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;

    @BeforeEach
    public void setup() {
        var stputil = new SkjæringstidspunktUtils();
        var entityManager = getEntityManager();
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider));
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            ytelseMaksdatoTjeneste, stputil);
        var beregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(entityManager);
        var opptjeningIUtlandDokStatusTjeneste = new OpptjeningIUtlandDokStatusTjeneste(
            new OpptjeningIUtlandDokStatusRepository(entityManager));
        var tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);
        var behandlingDokumentRepository = new BehandlingDokumentRepository(entityManager);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        behandlingDtoTjeneste = new BehandlingDtoTjeneste(repositoryProvider, beregningsgrunnlagTjeneste,
            tilbakekrevingRepository, skjæringstidspunktTjeneste, opptjeningIUtlandDokStatusTjeneste,
            behandlingDokumentRepository, relatertBehandlingTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()), null);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now())
            .medOpprinneligEndringsdato(LocalDate.now())
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        behandling = scenario.lagre(repositoryProvider);
        var behandlingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)
            .medManueltOpprettet(true);
        behandlingÅrsak.buildFor(behandling);
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
    }

    @Test
    public void skal_teste_at_behandlingÅrsakDto_får_korrekte_verdier() {

        UtvidetBehandlingDto dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, null);

        List<BehandlingÅrsakDto> årsaker = dto.getBehandlingÅrsaker();

        assertThat(årsaker).isNotNull();
        assertThat(årsaker).hasSize(1);
        assertThat(årsaker.get(0).getBehandlingArsakType()).isEqualTo(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);
        assertThat(årsaker.get(0).isManueltOpprettet()).isTrue();

    }
}
