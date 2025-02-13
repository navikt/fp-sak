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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.kompletthet.implV2.KompletthetsjekkerFelles;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

class KompletthetsjekkerRevurderingImplTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private KompletthetssjekkerTestUtil testUtil;

    private final KompletthetssjekkerSøknad kompletthetssjekkerSøknad = mock(KompletthetssjekkerSøknad.class);

    private KompletthetsjekkerRevurderingImpl kompletthetsjekkerRevurderingImpl;
    private final FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste = new FpInntektsmeldingTjeneste();

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
        var dokumentBestillerApplikasjonTjeneste = mock(DokumentBestillerTjeneste.class);
        var dokumentBehandlingTjeneste = mock(DokumentBehandlingTjeneste.class);
        var kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider, dokumentBestillerApplikasjonTjeneste,
            dokumentBehandlingTjeneste, null, new InntektsmeldingTjeneste(new AbakusInMemoryInntektArbeidYtelseTjeneste()), fpInntektsmeldingTjeneste);
        kompletthetsjekkerRevurderingImpl = new KompletthetsjekkerRevurderingImpl(
            kompletthetssjekkerSøknad, kompletthetsjekkerFelles,
            new SøknadRepository(entityManager, new BehandlingRepository(entityManager)),
            new BehandlingVedtakRepository(entityManager));

        var skjæringstidspunkt = Skjæringstidspunkt .builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        when(mock(SkjæringstidspunktTjeneste.class).getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
    }

    @Test
    void skal_finne_at_endringssøknad_er_mottatt_og_sette_på_vent_når_vedlegg_mangler() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenarioForMor();
        var behandling = lagre(scenario);
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, true);
        when(kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(any()))
            .thenReturn(singletonList(new ManglendeVedlegg(DokumentTypeId.LEGEERKLÆRING)));

        // Act
        assertThat(kompletthetsjekkerRevurderingImpl).isNotNull();
        var kompletthetResultat = kompletthetsjekkerRevurderingImpl.vurderForsendelseKomplett(lagRef(behandling), null);

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
        when(kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(any()))
            .thenReturn(emptyList());

        // Act
        assertThat(kompletthetsjekkerRevurderingImpl).isNotNull();
        var kompletthetResultat = kompletthetsjekkerRevurderingImpl.vurderForsendelseKomplett(lagRef(behandling), null);

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
