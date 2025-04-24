package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.FptilbakeRestKlient;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageHistorikkinnslag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlagevurderingOppdaterer;

@CdiDbAwareTest
class KlagevurderingOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private KlageRepository klageRepository;
    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    @Mock
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;


    @Test
    void skal_bestille_dokument_ved_stadfestet_ytelsesvedtak_og_lagre_KlageVurderingResultat() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();

        var klageScenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        var behandling = klageScenario.lagre(repositoryProvider, klageRepository);

        var klageVurdering = KlageVurdering.STADFESTE_YTELSESVEDTAK;
        var dto = new KlageVurderingResultatAksjonspunktDto("begrunnelse bla. bla.",
                klageVurdering, null,  "Fritekst til brev", null, null);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        getKlageVurderer(repositoryProvider, klageRepository).oppdater(dto, new AksjonspunktOppdaterParameter(
            BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert

        // verifiserer KlageVurderingResultat
        var klageVurderingResultat = klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP).get();
        assertThat(klageVurderingResultat.getKlageVurdering()).isEqualTo(KlageVurdering.STADFESTE_YTELSESVEDTAK);
        assertThat(klageVurderingResultat.getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NFP);
        assertThat(klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP)).isEqualTo(Optional.of(klageVurderingResultat));

        // verifiserer BrevBestilling
        var brevDtoCaptor = ArgumentCaptor.forClass(DokumentBestilling.class);
        verify(dokumentBestillerTjeneste).bestillDokument(brevDtoCaptor.capture());
        var dokumentBestilling = brevDtoCaptor.getValue();
        assertThat(dokumentBestilling.dokumentMal()).isEqualTo(DokumentMalType.KLAGE_OVERSENDT);
        assertThat(dokumentBestilling.fritekst()).isNull();

        // Verifiserer HistorikkinnslagDto
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.KLAGE_BEH_NFP);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var tekstlinje = historikkinnslag.getLinjer().get(0).getTekst();
        assertThat(tekstlinje).contains("Resultat");

        // Verifiserer at behandlende enhet er byttet til Nav klageinstans
        var enhetCapture = ArgumentCaptor.forClass(OrganisasjonsEnhet.class);
        verify(behandlingsutredningTjeneste).byttBehandlendeEnhet(anyLong(), enhetCapture.capture(), eq(""),
                eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        var enhet = enhetCapture.getValue();
        assertThat(enhet.enhetId()).isEqualTo(BehandlendeEnhetTjeneste.getKlageInstans().enhetId());
        assertThat(enhet.enhetNavn()).isEqualTo(BehandlendeEnhetTjeneste.getKlageInstans().enhetNavn());
        assertThat(behandling.getBehandlingsresultat().getBehandlingResultatType())
                .isEqualTo(BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET);
    }


    private KlagevurderingOppdaterer getKlageVurderer(BehandlingRepositoryProvider repositoryProvider, KlageRepository klageRepository) {
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var klageVurderingTjeneste = new KlageVurderingTjeneste(dokumentBestillerTjeneste, Mockito.mock(DokumentBehandlingTjeneste.class),
            prosesseringAsynkTjeneste, behandlingRepository, klageRepository, behandlingskontrollTjeneste,
            repositoryProvider.getBehandlingsresultatRepository(), mock(BehandlingEventPubliserer.class));
        var klageHistorikk = new KlageHistorikkinnslag(repositoryProvider.getHistorikkinnslagRepository(),
            behandlingRepository, repositoryProvider.getBehandlingVedtakRepository(), mock(FptilbakeRestKlient.class));
        return new KlagevurderingOppdaterer(klageHistorikk, behandlingsutredningTjeneste, mock(BehandlingskontrollTjeneste.class), klageVurderingTjeneste,
            behandlingRepository);
    }


}
