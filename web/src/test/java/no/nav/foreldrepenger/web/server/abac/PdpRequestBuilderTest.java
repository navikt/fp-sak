package no.nav.foreldrepenger.web.server.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.pdp.ForeldrepengerDataKeys;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipBehandlingStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipFagsakStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipOverstyring;

@ExtendWith(MockitoExtension.class)
class PdpRequestBuilderTest {

    private static final Long FAGSAK_ID = 10001L;
    private static final Long FAGSAK_ID_2 = 10002L;
    private static final Long BEHANDLING_ID = 333L;
    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("444");
    private static final String SAKSNUMMER = "7777";

    private static final AktørId AKTØR_0 = AktørId.dummy();
    private static final AktørId AKTØR_1 = AktørId.dummy();
    private static final AktørId AKTØR_2 = AktørId.dummy();

    private static final String PERSON_0 = "00000000000";

    @Mock
    private PipRepository pipRepository;

    private AppPdpRequestBuilderImpl requestBuilder;

    @BeforeEach
    public void beforeEach() {
        requestBuilder = new AppPdpRequestBuilderImpl(pipRepository);

    }

    @Test
    void skal_hente_saksstatus_og_behandlingsstatus_når_behandlingId_er_input() {
        var attributter = AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_ID, BEHANDLING_ID);

        lenient().when(pipRepository.fagsakIdForJournalpostId(Collections.singleton(JOURNALPOST_ID))).thenReturn(Collections.singleton(FAGSAK_ID));
        lenient().when(pipRepository.hentAktørIdKnyttetTilFagsaker(Collections.singleton(FAGSAK_ID))).thenReturn(Collections.singleton(AKTØR_1));
        var behandligStatus = BehandlingStatus.OPPRETTET.getKode();
        var ansvarligSaksbehandler = "Z123456";
        var fagsakStatus = FagsakStatus.UNDER_BEHANDLING.getKode();
        lenient().when(pipRepository.hentDataForBehandling(BEHANDLING_ID)).thenReturn(
                Optional.of(new PipBehandlingsData(behandligStatus, ansvarligSaksbehandler, FAGSAK_ID, fagsakStatus)));

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getAktørIdSet()).containsOnly(AKTØR_1.getId());
        assertThat(request.getResource(ForeldrepengerDataKeys.SAKSBEHANDLER).verdi()).isEqualTo(ansvarligSaksbehandler);
        assertThat(request.getResource(ForeldrepengerDataKeys.BEHANDLING_STATUS).verdi())
                .isEqualTo(PipBehandlingStatus.OPPRETTET.getVerdi());
        assertThat(request.getResource(ForeldrepengerDataKeys.FAGSAK_STATUS).verdi())
                .isEqualTo(PipFagsakStatus.UNDER_BEHANDLING.getVerdi());
    }

    @Test
    void skal_angi_aktørId_gitt_journalpost_id_som_input() {
        var attributter = AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.JOURNALPOST_ID, JOURNALPOST_ID.getVerdi());

        lenient().when(pipRepository.fagsakIdForJournalpostId(Collections.singleton(JOURNALPOST_ID))).thenReturn(Collections.singleton(FAGSAK_ID));
        lenient().when(pipRepository.fagsakIdForSaksnummer(Collections.singleton(SAKSNUMMER))).thenReturn(Collections.singleton(FAGSAK_ID));
        lenient().when(pipRepository.hentAktørIdKnyttetTilFagsaker(Collections.singleton(FAGSAK_ID))).thenReturn(Collections.singleton(AKTØR_1));

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getAktørIdSet()).containsOnly(AKTØR_1.getId());
    }

    @Test
    void skal_hente_fnr_fra_alle_tilknyttede_saker_når_det_kommer_inn_søk_etter_saker_for_aktørid() {
        var attributter = AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKER_FOR_AKTØR, AKTØR_0.getId());

        Set<Long> fagsakIder = new HashSet<>();
        fagsakIder.add(FAGSAK_ID);
        fagsakIder.add(FAGSAK_ID_2);
        lenient().when(pipRepository.fagsakIderForSøker(Collections.singleton(AKTØR_0))).thenReturn(fagsakIder);
        Set<AktørId> aktører = new HashSet<>();
        aktører.add(AKTØR_0);
        aktører.add(AKTØR_1);
        aktører.add(AKTØR_2);
        lenient().when(pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIder)).thenReturn(aktører);

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getAktørIdSet()).containsOnly(AKTØR_0.getId(),
            AKTØR_1.getId(), AKTØR_2.getId());
    }

    @Test
    void skal_bare_sende_fnr_vider_til_pdp() {
        var attributter = AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.FNR, PERSON_0);

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getFødselsnumre()).containsOnly(PERSON_0);
    }

    @Test
    void skal_ta_inn_aksjonspunkt_id_og_sende_videre_aksjonspunkt_typer() {
        var attributter = AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL)
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET);

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getResource(ForeldrepengerDataKeys.AKSJONSPUNKT_OVERSTYRING).verdi()).isEqualTo(PipOverstyring.OVERSTYRING.getVerdi());
    }

    @Test
    void skal_slå_opp_og_sende_videre_fnr_når_aktør_id_er_input() {
        var attributter = AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, AKTØR_1.getId());

        var request = requestBuilder.lagAppRessursData(attributter);
        assertThat(request.getAktørIdSet()).containsOnly(AKTØR_1.getId());
    }

    @Test
    void skal_ikke_godta_at_det_sendes_inn_fagsak_id_og_behandling_id_som_ikke_stemmer_overens() {

        var attributter = AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.FAGSAK_ID, 123L);
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_ID, 1234L));

        when(pipRepository.hentDataForBehandling(1234L)).thenReturn(Optional.of(
                new PipBehandlingsData(BehandlingStatus.OPPRETTET.getKode(), "Z1234", 666L, FagsakStatus.OPPRETTET.getKode())));

        var e = assertThrows(ManglerTilgangException.class, () -> requestBuilder.lagAppRessursData(attributter));
        assertThat(e.getMessage()).contains("Ugyldig input. Ikke samsvar mellom behandlingId 1234 og fagsakId [123]");

    }

}
