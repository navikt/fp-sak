package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class ForespørselStatusVurderingTjenesteTest {

    private static final String SAKSNUMMER = "1234567890";
    private static final String ORGNR = "999999999";
    private static final String ANNET_ORGNR = "888888888";
    private static final Long FAGSAK_ID = 42L;

    @Mock
    private FagsakTjeneste fagsakTjenesteMock;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjenesteMock;

    private ForespørselStatusVurderingTjeneste tjeneste;

    @BeforeEach
    void setup() {
        tjeneste = new ForespørselStatusVurderingTjeneste(fagsakTjenesteMock, behandlingRepositoryMock, arbeidsforholdInntektsmeldingMangelTjenesteMock);
    }

    @Test
    void avsluttet_fagsak_gir_trengs_ikke() {
        var fagsak = lagFagsak(FagsakStatus.AVSLUTTET);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(new Saksnummer(SAKSNUMMER), false)).thenReturn(Optional.of(fagsak));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR))).isFalse();
    }

    @Test
    void ingen_arbeidsforhold_for_orgnummer_gir_trengs_ikke() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(List.of());

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR))).isFalse();
    }

    @Test
    void minst_en_ikke_mottatt_gir_trengs() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT), lagIMStatus(ORGNR, InntektsmeldingStatus.IKKE_MOTTAT)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR))).isTrue();
    }

    @Test
    void alle_mottatt_gir_trengs_ikke() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT), lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR))).isFalse();
    }

    @Test
    void avklart_ikke_paakrevd_gir_trengs_fortsatt_skal_ikke_lukkes_automatisk() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT), lagIMStatus(ORGNR, InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR))).isTrue();
    }

    @Test
    void statuser_for_annet_orgnummer_filtreres_bort() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(lagIMStatus(ANNET_ORGNR, InntektsmeldingStatus.IKKE_MOTTAT)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR))).isFalse();
    }

    private void stubFagsakOgBehandling(Behandling behandling) {
        var fagsak = lagFagsak(FagsakStatus.LØPENDE);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(new Saksnummer(SAKSNUMMER), false)).thenReturn(Optional.of(fagsak));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_ID)).thenReturn(Optional.of(behandling));
    }

    private Fagsak lagFagsak(FagsakStatus status) {
        var fagsak = mock(Fagsak.class);
        lenient().when(fagsak.getStatus()).thenReturn(status);
        lenient().when(fagsak.getId()).thenReturn(FAGSAK_ID);
        return fagsak;
    }

    private static ArbeidsforholdInntektsmeldingStatus lagIMStatus(String orgnummer, InntektsmeldingStatus status) {
        return new ArbeidsforholdInntektsmeldingStatus(Arbeidsgiver.virksomhet(orgnummer), InternArbeidsforholdRef.nullRef(), status);
    }
}
