package no.nav.foreldrepenger.kompletthet.impl.fp;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerImpl;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerSøknadTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeVedleggTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

class KompletthetsjekkerRevurderingImplTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private KompletthetssjekkerTestUtil testUtil;

    private ManglendeVedleggTjeneste manglendeVedleggTjeneste = mock(ManglendeVedleggTjeneste.class);

    private KompletthetsjekkerImpl kompletthetsjekker;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
        var dokumentBehandlingTjeneste = mock(DokumentBehandlingTjeneste.class);
        var kompletthetsjekkerSøknad = new KompletthetsjekkerSøknadTjeneste(repositoryProvider, manglendeVedleggTjeneste);
        var manglendeInntektsmeldingTjeneste = new ManglendeInntektsmeldingTjeneste(
            repositoryProvider,
            dokumentBehandlingTjeneste,
            null,
                new InntektsmeldingTjeneste(new AbakusInMemoryInntektArbeidYtelseTjeneste())
        );
        kompletthetsjekker = new KompletthetsjekkerImpl(repositoryProvider.getBehandlingRepository(), kompletthetsjekkerSøknad, manglendeInntektsmeldingTjeneste);

        var skjæringstidspunkt = Skjæringstidspunkt .builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        when(mock(SkjæringstidspunktTjeneste.class).getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
    }

    @Test
    void skal_finne_at_endringssøknad_er_mottatt_og_sette_på_vent_når_vedlegg_mangler() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        var behandling = lagre(scenario);
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, true);
        when(manglendeVedleggTjeneste.utledManglendeVedleggForSøknad(any())).thenReturn(singletonList(new ManglendeVedlegg(DokumentTypeId.LEGEERKLÆRING)));

        // Act
        assertThat(kompletthetsjekker).isNotNull();
        var kompletthetResultat = kompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling), null);

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.venteårsak()).isEqualTo(Venteårsak.AVV_DOK);
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(1));
    }

    @Test
    void skal_finne_at_endringssøknad_er_mottatt_og_at_forsendelsen_er_komplett_når_ingen_vedlegg_mangler() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        var behandling = lagre(scenario);
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, true);
        when(manglendeVedleggTjeneste.utledManglendeVedleggForSøknad(any())).thenReturn(emptyList());

        // Act
        assertThat(kompletthetsjekker).isNotNull();
        var kompletthetResultat = kompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling), null);

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
    }

    private Behandling lagre(ScenarioMorSøkerForeldrepenger scenario) {
        return scenario.lagre(repositoryProvider);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }
}
