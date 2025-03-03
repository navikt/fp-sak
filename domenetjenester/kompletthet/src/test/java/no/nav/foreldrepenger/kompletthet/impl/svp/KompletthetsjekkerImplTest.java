package no.nav.foreldrepenger.kompletthet.impl.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerImpl;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerSøknadTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeVedleggTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class KompletthetsjekkerImplTest extends EntityManagerAwareTest {

    private static final LocalDate STARTDATO_PERMISJON = LocalDate.now().plusWeeks(1);
    private BehandlingRepositoryProvider repositoryProvider;

    private KompletthetssjekkerTestUtil testUtil;

    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private PersonopplysningTjeneste personopplysningTjeneste;

    private Kompletthetsjekker kompletthetsjekker;
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
        .medUtledetSkjæringstidspunkt(STARTDATO_PERMISJON)
        .build();

    @BeforeEach
    public void before() {
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong()))
            .thenReturn(skjæringstidspunkt);
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerForKompletthet(any(), any()))
            .thenReturn(new HashMap<>());

        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());

        var manglendeVedleggTjeneste = new ManglendeVedleggTjeneste(repositoryProvider, dokumentArkivTjeneste);
        var kompletthetsjekkerSøknad = new KompletthetsjekkerSøknadTjeneste(repositoryProvider, manglendeVedleggTjeneste);
        var manglendeInntektsmeldingTjeneste = new ManglendeInntektsmeldingTjeneste(
            repositoryProvider,
            dokumentBehandlingTjenesteMock,
            inntektsmeldingArkivTjeneste,
            inntektsmeldingTjeneste
        );
        kompletthetsjekker = new KompletthetsjekkerImpl(repositoryProvider, kompletthetsjekkerSøknad, personopplysningTjeneste, manglendeInntektsmeldingTjeneste);

        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
    }

    @Test
    void kompletthet_skal_ikke_være_oppfylt_hvis_behandling_mangler_inntektsmelding() {
        // Arrange
        var behandling =  ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagre(repositoryProvider);
        mockManglendeInntektsmeldingGrunnlag();
        // Har ventet 10 dager fra søknad = forespørsel om inntektsmelding
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusDays(10),
            STARTDATO_PERMISJON);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderEtterlysningInntektsmelding(
            lagRef(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(STARTDATO_PERMISJON).build());

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(2));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    private void mockManglendeInntektsmeldingGrunnlag() {
        var manglendeInntektsmeldinger = new HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerForKompletthet(any(), any())).thenReturn(
            manglendeInntektsmeldinger);
    }
}
