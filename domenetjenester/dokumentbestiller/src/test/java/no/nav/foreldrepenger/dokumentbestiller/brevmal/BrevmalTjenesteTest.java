package no.nav.foreldrepenger.dokumentbestiller.brevmal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;

@ExtendWith(MockitoExtension.class)
class BrevmalTjenesteTest {

    @Mock
    DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    @Mock
    Behandling behandling;

    @Test
    void hent_brevmaler_for_es_førstegangsbehandling() {
        BrevmalTjeneste brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandling.getType()).thenReturn(BehandlingType.FØRSTEGANGSSØKNAD);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(3);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(),
            DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.getKode(), DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.getKode());

    }

    @Test
    void hent_brevmaler_for_es_revurdering() {
        BrevmalTjeneste brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(1);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode());

    }

    @Test
    void hent_brevmaler_for_fp_revurdering() {
        BrevmalTjeneste brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(3);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(), DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode());
    }

    @Test
    void hent_brevmaler_for_fp_revurdering_varsel_sendt() {
        BrevmalTjeneste brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandling.erManueltOpprettet()).thenReturn(true);
        when(dokumentBehandlingTjeneste.erDokumentBestilt(anyLong(), eq(DokumentMalType.VARSEL_OM_REVURDERING))).thenReturn(true);

        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(3);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList()).containsExactlyInAnyOrder(DokumentMalType.VARSEL_OM_REVURDERING.getKode(),
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(), DokumentMalType.ETTERLYS_INNTEKTSMELDING.getKode());

    }

    @Test
    void hent_brevmaler_for_fp_innsyn() {
        BrevmalTjeneste brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.INNSYN);

        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(2);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList())
            .containsExactlyInAnyOrder(
            DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(),
                DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.getKode());

    }

    @Test
    void hent_brevmaler_for_fp_klage() {
        BrevmalTjeneste brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjeneste);
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(behandling.getType()).thenReturn(BehandlingType.KLAGE);

        var brevmalDtos = brevmalTjeneste.hentBrevmalerFor(behandling);

        assertThat(brevmalDtos).hasSize(2);
        assertThat(brevmalDtos.stream().map(BrevmalDto::kode).toList())
            .containsExactlyInAnyOrder(
                DokumentMalType.INNHENTE_OPPLYSNINGER.getKode(),
                DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.getKode());

    }
}
