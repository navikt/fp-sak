package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.Forespørsel;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.YtelseType;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse.Vurdering;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse.Årsak;

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
    void ukjent_saksnummer_gir_sak_ikke_funnet() {
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(eq(new Saksnummer(SAKSNUMMER)), eq(false))).thenReturn(Optional.empty());

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.vurdering()).isEqualTo(Vurdering.UKJENT);
        assertThat(svar.årsak()).isEqualTo(Årsak.SAK_IKKE_FUNNET);
    }

    @Test
    void feil_ytelse_paa_sak_gir_sak_ikke_funnet() {
        var fagsak = fagsakMed(FagsakYtelseType.SVANGERSKAPSPENGER, FagsakStatus.LØPENDE);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(eq(new Saksnummer(SAKSNUMMER)), eq(false))).thenReturn(Optional.of(fagsak));

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.SAK_IKKE_FUNNET);
    }

    @Test
    void avsluttet_fagsak_gir_sak_avsluttet() {
        var fagsak = fagsakMed(FagsakYtelseType.FORELDREPENGER, FagsakStatus.AVSLUTTET);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(eq(new Saksnummer(SAKSNUMMER)), eq(false))).thenReturn(Optional.of(fagsak));

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.vurdering()).isEqualTo(Vurdering.TRENGS_IKKE);
        assertThat(svar.årsak()).isEqualTo(Årsak.SAK_AVSLUTTET);
    }

    @Test
    void ingen_ytelsesbehandling_gir_ingen_behandling_som_trengs_ikke() {
        var fagsak = fagsakMed(FagsakYtelseType.FORELDREPENGER, FagsakStatus.LØPENDE);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(eq(new Saksnummer(SAKSNUMMER)), eq(false))).thenReturn(Optional.of(fagsak));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_ID)).thenReturn(Optional.empty());

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.vurdering()).isEqualTo(Vurdering.TRENGS_IKKE);
        assertThat(svar.årsak()).isEqualTo(Årsak.INGEN_BEHANDLING);
    }

    @Test
    void avsluttet_og_avslaatt_behandling_gir_behandling_avslaatt() {
        var behandling = avsluttetBehandlingMedResultat(true, false);
        stubFagsakOgBehandling(behandling);

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.BEHANDLING_AVSLÅTT);
    }

    @Test
    void avsluttet_og_henlagt_behandling_gir_behandling_henlagt() {
        var behandling = avsluttetBehandlingMedResultat(false, true);
        stubFagsakOgBehandling(behandling);

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.BEHANDLING_HENLAGT);
    }

    @Test
    void ingen_arbeidsforhold_for_orgnummer_gir_orgnr_ikke_paakrevd() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(List.of());

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.ORGNR_IKKE_PÅKREVD);
    }

    @Test
    void minst_en_ikke_mottatt_gir_mangler_inntektsmelding() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(statusFor(ORGNR, InntektsmeldingStatus.MOTTATT), statusFor(ORGNR, InntektsmeldingStatus.IKKE_MOTTAT)));

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.vurdering()).isEqualTo(Vurdering.TRENGS);
        assertThat(svar.årsak()).isEqualTo(Årsak.MANGLER_INNTEKTSMELDING);
    }

    @Test
    void alle_mottatt_gir_inntektsmelding_mottatt() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(statusFor(ORGNR, InntektsmeldingStatus.MOTTATT), statusFor(ORGNR, InntektsmeldingStatus.MOTTATT)));

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.INNTEKTSMELDING_MOTTATT);
    }

    @Test
    void mottatt_og_avklart_ikke_paakrevd_uten_ikke_mottatt_gir_avklart_ikke_paakrevd() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(statusFor(ORGNR, InntektsmeldingStatus.MOTTATT), statusFor(ORGNR, InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD)));

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.AVKLART_IKKE_PÅKREVD);
    }

    @Test
    void statuser_for_annet_orgnummer_filtreres_bort() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any())).thenReturn(
            List.of(statusFor(ANNET_ORGNR, InntektsmeldingStatus.IKKE_MOTTAT)));

        var svar = vurderEn(gyldigForespørsel());

        assertThat(svar.årsak()).isEqualTo(Årsak.ORGNR_IKKE_PÅKREVD);
    }

    @Test
    void batchen_gir_ett_svar_per_forespoersel_i_samme_rekkefoelge() {
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(eq(new Saksnummer(SAKSNUMMER)), eq(false))).thenReturn(Optional.empty());

        var request = new ForespørselStatusRequest(
            List.of(gyldigForespørsel(), new Forespørsel(SAKSNUMMER, ANNET_ORGNR, YtelseType.FORELDREPENGER)));
        var svar = tjeneste.vurder(request);

        assertThat(svar).hasSize(2);
        assertThat(svar.get(0).fagsakSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(svar.get(1).fagsakSaksnummer()).isEqualTo(SAKSNUMMER);
    }

    private void stubFagsakOgBehandling(Behandling behandling) {
        var fagsak = fagsakMed(FagsakYtelseType.FORELDREPENGER, FagsakStatus.LØPENDE);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(eq(new Saksnummer(SAKSNUMMER)), eq(false))).thenReturn(Optional.of(fagsak));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_ID)).thenReturn(Optional.of(behandling));
    }

    private Fagsak fagsakMed(FagsakYtelseType ytelseType, FagsakStatus status) {
        var fagsak = mock(Fagsak.class);
        when(fagsak.getYtelseType()).thenReturn(ytelseType);
        lenient().when(fagsak.getStatus()).thenReturn(status);
        lenient().when(fagsak.getId()).thenReturn(FAGSAK_ID);
        return fagsak;
    }

    private Behandling avsluttetBehandlingMedResultat(boolean avslått, boolean henlagt) {
        var behandling = mock(Behandling.class);
        var behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandling.erAvsluttet()).thenReturn(true);
        when(behandling.getBehandlingsresultat()).thenReturn(behandlingsresultat);
        when(behandlingsresultat.isBehandlingsresultatAvslått()).thenReturn(avslått);
        if (!avslått) {
            when(behandlingsresultat.isBehandlingHenlagt()).thenReturn(henlagt);
        }
        return behandling;
    }

    private static ArbeidsforholdInntektsmeldingStatus statusFor(String orgnummer, InntektsmeldingStatus status) {
        return new ArbeidsforholdInntektsmeldingStatus(Arbeidsgiver.virksomhet(orgnummer), InternArbeidsforholdRef.nullRef(), status);
    }

    private ForespørselStatusResponse vurderEn(Forespørsel forespørsel) {
        return tjeneste.vurder(new ForespørselStatusRequest(List.of(forespørsel))).getFirst();
    }

    private static Forespørsel gyldigForespørsel() {
        return new Forespørsel(SAKSNUMMER, ORGNR, YtelseType.FORELDREPENGER);
    }
}
