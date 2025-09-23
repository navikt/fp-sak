package no.nav.foreldrepenger.web.server.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.pdp.ForeldrepengerDataKeys;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipBehandlingStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipFagsakStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipOverstyring;

@ExtendWith(MockitoExtension.class)
class PdpRequestBuilderTest {

    private static final Long BEHANDLING_ID = 333L;
    private static final UUID BEHANDLING_UUID = UUID.randomUUID();
    private static final String JOURNALPOST_ID = "444";
    private static final Saksnummer SAKSNUMMER = new Saksnummer("7777");
    private static final Saksnummer SAKSNUMMER_2 = new Saksnummer("7778");

    private static final AktørId AKTØR_1 = AktørId.dummy();

    private static final String PERSON_0 = "00000000000";

    @Mock
    private PipRepository pipRepository;

    private AppPdpRequestBuilderImpl requestBuilder;

    @BeforeEach
    void beforeEach() {
        requestBuilder = new AppPdpRequestBuilderImpl(pipRepository);

    }

    @Test
    void skal_hente_saksstatus_og_behandlingsstatus_når_behandlingId_er_input() {
        var attributter = TilbakeRestTjeneste.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, BEHANDLING_UUID);

        var behandligStatus = BehandlingStatus.OPPRETTET;
        var ansvarligSaksbehandler = "Z123456";
        var fagsakStatus = FagsakStatus.UNDER_BEHANDLING;
        lenient().when(pipRepository.hentDataForBehandlingUuid(BEHANDLING_UUID)).thenReturn(
                Optional.of(new PipBehandlingsData(behandligStatus, ansvarligSaksbehandler, BEHANDLING_ID, BEHANDLING_UUID, fagsakStatus, SAKSNUMMER)));

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getSaksnummer()).isEqualTo(SAKSNUMMER.getVerdi());
        assertThat(request.getResource(ForeldrepengerDataKeys.SAKSBEHANDLER).verdi()).isEqualTo(ansvarligSaksbehandler);
        assertThat(request.getResource(ForeldrepengerDataKeys.BEHANDLING_STATUS).verdi())
                .isEqualTo(PipBehandlingStatus.OPPRETTET.getVerdi());
        assertThat(request.getResource(ForeldrepengerDataKeys.FAGSAK_STATUS).verdi())
                .isEqualTo(PipFagsakStatus.UNDER_BEHANDLING.getVerdi());
    }

    @Test
    void skal_angi_saksnummer_gitt_journalpost_id_som_input() {
        var attributter = TilbakeRestTjeneste.opprett()
                .leggTil(AppAbacAttributtType.JOURNALPOST_ID, JOURNALPOST_ID);

        lenient().when(pipRepository.saksnummerForJournalpostId(Collections.singleton(JOURNALPOST_ID))).thenReturn(Collections.singleton(SAKSNUMMER));

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getSaksnummer()).isEqualTo(SAKSNUMMER.getVerdi());
    }

    @Test
    void skal_bare_sende_fnr_vider_til_pdp() {
        var attributter = TilbakeRestTjeneste.opprett().leggTil(AppAbacAttributtType.FNR, PERSON_0);

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getIdenter()).containsOnly(PERSON_0);
    }

    @Test
    void skal_ta_inn_aksjonspunkt_id_og_sende_videre_aksjonspunkt_typer() {
        var attributter = TilbakeRestTjeneste.opprett()
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL)
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET);

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getResource(ForeldrepengerDataKeys.AKSJONSPUNKT_OVERSTYRING).verdi()).isEqualTo(PipOverstyring.OVERSTYRING.getVerdi());
    }

    @Test
    void skal_slå_opp_og_sende_videre_fnr_når_aktør_id_er_input() {
        var attributter = TilbakeRestTjeneste.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, AKTØR_1.getId());

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getIdenter()).containsOnly(AKTØR_1.getId());
    }

    @Test
    void skal_ikke_godta_at_det_sendes_inn_fagsak_id_og_behandling_id_som_ikke_stemmer_overens() {

        var attributter = TilbakeRestTjeneste.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, SAKSNUMMER_2.getVerdi());
        attributter.leggTil(TilbakeRestTjeneste.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, BEHANDLING_UUID));

        when(pipRepository.hentDataForBehandlingUuid(BEHANDLING_UUID)).thenReturn(Optional.of(
                new PipBehandlingsData(BehandlingStatus.OPPRETTET, "Z1234", 666L, BEHANDLING_UUID, FagsakStatus.OPPRETTET, SAKSNUMMER)));

        var e = assertThrows(TekniskException.class, () -> requestBuilder.lagAppRessursData(attributter));
        assertThat(e.getMessage()).contains("Ugyldig input. Støtter bare 0 eller 1 sak, men har");

    }

}
