package no.nav.foreldrepenger.dokumentbestiller.brevmal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;

@ExtendWith(MockitoExtension.class)
class BrevmalTjenesteTest {

    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    @Mock
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    @Test
    void hent_brevmaler_for_es_førstegangsbehandling() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandling.getType()).thenReturn(BehandlingType.FØRSTEGANGSSØKNAD);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(),
            DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.getKode(), DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.getKode(),
            DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL_FORUTGÅENDE.getKode());

    }

    @Test
    void hent_brevmaler_for_es_revurdering_manuelt_opprettet() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.INNHENTE_OPPLYSNINGER.getKode());
    }

    @Test
    void hent_brevmaler_for_es_revurdering_automatisk_opprettet() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(false);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode());
    }

    @Test
    void hent_brevmaler_for_fp_revurdering() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        var fag = mock(Fagsak.class);
        when(fag.getSaksnummer()).thenReturn(new Saksnummer("123"));
        when(behandling.getFagsak()).thenReturn(fag);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(), DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode());
    }

    @Test
    void skal_ikke_kunne_bestille_elysim_hvis_ingen_inntektsmelding_mangler() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var arbeidsforhold = new ArbeidsforholdInntektsmeldingStatus(Arbeidsgiver.virksomhet("999999999"), InternArbeidsforholdRef.nyRef(),
            ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD);
        when(arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(any(BehandlingReferanse.class))).thenReturn(
            Collections.singletonList(arbeidsforhold));
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        var fag = mock(Fagsak.class);
        when(fag.getSaksnummer()).thenReturn(new Saksnummer("123"));
        when(behandling.getFagsak()).thenReturn(fag);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(), DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode());
        var elysim = brevmalDtos.stream()
            .filter(mal -> mal.kode().equals(DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode()))
            .findFirst()
            .orElseThrow();
        assertThat(elysim.tilgjengelig()).isFalse();
    }

    @Test
    void skal_kunne_bestille_elysim_hvis_inntektsmelding_mangler() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var arbeidsforhold = new ArbeidsforholdInntektsmeldingStatus(Arbeidsgiver.virksomhet("999999999"), InternArbeidsforholdRef.nyRef(),
            ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
        when(arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(any(BehandlingReferanse.class))).thenReturn(
            Collections.singletonList(arbeidsforhold));
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        var fag = mock(Fagsak.class);
        when(fag.getSaksnummer()).thenReturn(new Saksnummer("123"));
        when(behandling.getFagsak()).thenReturn(fag);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(), DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode());
        var elysim = brevmalDtos.stream()
            .filter(mal -> mal.kode().equals(DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode()))
            .findFirst()
            .orElseThrow();
        assertThat(elysim.tilgjengelig()).isTrue();
    }


    @Test
    void hent_brevmaler_for_fp_revurdering_varsel_sendt() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        var fag = mock(Fagsak.class);
        when(fag.getSaksnummer()).thenReturn(new Saksnummer("123"));
        when(behandling.getFagsak()).thenReturn(fag);
        when(dokumentBehandlingTjeneste.erDokumentBestilt(anyLong(), eq(DokumentMalType.VARSEL_OM_REVURDERING))).thenReturn(true);

        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(3);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(), DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode());

    }

    @Test
    void hent_brevmaler_for_fp_innsyn() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.INNSYN);

        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(),
            DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.getKode());

    }

    @Test
    void hent_brevmaler_for_fp_klage() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.KLAGE);

        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(),
            DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.getKode());

    }

    @Test
    void skal_hente_forlenget_saksbehandlingstid_for_es() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandling.getType()).thenReturn(BehandlingType.FØRSTEGANGSSØKNAD);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).anyMatch(dto -> dto.kode().equals(DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.getKode()));
    }

    @Test
    void skal_ikke_hente_forlenget_saksbehandlingstid_for_fp() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = førstegangsbehandling(FagsakYtelseType.FORELDREPENGER);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).noneMatch(dto -> dto.kode().equals(DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.getKode()));
    }

    @Test
    void skal_ikke_hente_forlenget_saksbehandlingstid_for_svp() {
        var brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste, arbeidsforholdInntektsmeldingMangelTjeneste);
        var behandling = førstegangsbehandling(FagsakYtelseType.SVANGERSKAPSPENGER);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).noneMatch(dto -> dto.kode().equals(DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.getKode()));
    }

    private Behandling førstegangsbehandling(FagsakYtelseType foreldrepenger) {
        var behandling = mock(Behandling.class);
        when(behandling.getFagsakYtelseType()).thenReturn(foreldrepenger);
        when(behandling.getType()).thenReturn(BehandlingType.FØRSTEGANGSSØKNAD);
        var fag = mock(Fagsak.class);
        when(fag.getSaksnummer()).thenReturn(new Saksnummer("123"));
        when(behandling.getFagsak()).thenReturn(fag);
        return behandling;
    }
}
