package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class KompletthetsjekkerRevurderingImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final KompletthetssjekkerTestUtil testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);

    private final KompletthetssjekkerSøknad kompletthetssjekkerSøknad = mock(KompletthetssjekkerSøknad.class);
    private final DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste = mock(DokumentBestillerApplikasjonTjeneste.class);
    private final DokumentBehandlingTjeneste dokumentBehandlingTjeneste = mock(DokumentBehandlingTjeneste.class);

    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private final KompletthetsjekkerFelles kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider, dokumentBestillerApplikasjonTjeneste, dokumentBehandlingTjeneste);
    private final KompletthetsjekkerRevurderingImpl kompletthetsjekkerRevurderingImpl = new KompletthetsjekkerRevurderingImpl(
        kompletthetssjekkerSøknad, kompletthetsjekkerFelles,
        inntektsmeldingTjeneste,
        repositoryProvider.getSøknadRepository(),
        repositoryProvider.getBehandlingVedtakRepository());

    @Before
    public void before() {
        var skjæringstidspunkt = Skjæringstidspunkt .builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
    }

    @Test
    public void skal_finne_at_endringssøknad_er_mottatt_og_sette_på_vent_når_vedlegg_mangler() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = testUtil.opprettRevurderingsscenarioForMor();
        Behandling behandling = lagre(scenario);
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, true);
        when(kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(any()))
            .thenReturn(singletonList(new ManglendeVedlegg(DokumentTypeId.LEGEERKLÆRING)));

        // Act
        assertThat(kompletthetsjekkerRevurderingImpl).isNotNull();
        KompletthetResultat kompletthetResultat = kompletthetsjekkerRevurderingImpl.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVenteårsak()).isEqualTo(Venteårsak.AVV_DOK);
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
    }

    @Test
    public void skal_finne_at_endringssøknad_er_mottatt_og_at_forsendelsen_er_komplett_når_ingen_vedlegg_mangler() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = testUtil.opprettRevurderingsscenarioForMor();
        Behandling behandling = lagre(scenario);
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, true);
        when(kompletthetssjekkerSøknad.utledManglendeVedleggForSøknad(any()))
            .thenReturn(emptyList());

        // Act
        assertThat(kompletthetsjekkerRevurderingImpl).isNotNull();
        KompletthetResultat kompletthetResultat = kompletthetsjekkerRevurderingImpl.vurderForsendelseKomplett(lagRef(behandling));

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
