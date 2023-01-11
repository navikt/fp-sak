package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.event.BehandlingRelasjonEventPubliserer;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlagevurderingOppdaterer;

@CdiDbAwareTest
public class KlagevurderingOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private KlageRepository klageRepository;
    @Mock
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    @Mock
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;


    @Test
    public void skal_bestille_dokument_ved_stadfestet_ytelsesvedtak_og_lagre_KlageVurderingResultat() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();

        var klageScenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        var behandling = klageScenario.lagre(repositoryProvider, klageRepository);

        var klageVurdering = KlageVurdering.STADFESTE_YTELSESVEDTAK;
        var dto = new KlageVurderingResultatAksjonspunktDto("begrunnelse bla. bla.",
                klageVurdering, null, null, LocalDate.now(), "Fritekst til brev", null, null, false);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        getKlageVurderer(repositoryProvider, klageRepository).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert

        // verifiserer KlageVurderingResultat
        var klageVurderingResultat = klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP).get();
        assertThat(klageVurderingResultat.getKlageVurdering()).isEqualTo(KlageVurdering.STADFESTE_YTELSESVEDTAK);
        assertThat(klageVurderingResultat.getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NFP);
        assertThat(klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP)).isEqualTo(Optional.of(klageVurderingResultat));

        // verifiserer BestillBrevDto
        var brevDtoCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste).bestillDokument(brevDtoCaptor.capture(), eq(HistorikkAktør.SAKSBEHANDLER));
        var bestillBrevDto = brevDtoCaptor.getValue();
        assertThat(bestillBrevDto.getBrevmalkode()).isEqualTo(DokumentMalType.KLAGE_OVERSENDT);
        assertThat(bestillBrevDto.getFritekst()).isNull();

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkApplikasjonTjeneste).lagInnslag(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
                .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.KLAGE_BEH_NFP.getKode()));
        assertThat(del.getEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_NFP)).isNotNull();

        // Verifiserer at behandlende enhet er byttet til NAV Klageinstans
        var enhetCapture = ArgumentCaptor.forClass(OrganisasjonsEnhet.class);
        verify(behandlingsutredningTjeneste).byttBehandlendeEnhet(anyLong(), enhetCapture.capture(), eq(""),
                eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        var enhet = enhetCapture.getValue();
        assertThat(enhet.enhetId()).isEqualTo(BehandlendeEnhetTjeneste.getKlageInstans().enhetId());
        assertThat(enhet.enhetNavn()).isEqualTo(BehandlendeEnhetTjeneste.getKlageInstans().enhetNavn());
        assertThat(behandling.getBehandlingsresultat().getBehandlingResultatType())
                .isEqualTo(BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET);
    }

    @Test
    public void skal_ikke_bestille_dokument_ved_stadfestet_ytelsesvedtak_når_behandlingen_er_migrert() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();

        var klageScenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        var behandling = klageScenario.lagre(repositoryProvider, klageRepository);
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);

        var klageVurdering = KlageVurdering.STADFESTE_YTELSESVEDTAK;
        var dto = new KlageVurderingResultatAksjonspunktDto("begrunnelse bla. bla.",
                klageVurdering, null, null, LocalDate.now(), "Fritekst til brev", null, null, false);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        getKlageVurderer(repositoryProvider, klageRepository).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        verify(dokumentBestillerTjeneste, times(0)).bestillDokument(any(), any());
    }

    private KlagevurderingOppdaterer getKlageVurderer(BehandlingRepositoryProvider repositoryProvider, KlageRepository klageRepository) {
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        final var klageVurderingTjeneste = new KlageVurderingTjeneste(dokumentBestillerTjeneste, Mockito.mock(DokumentBehandlingTjeneste.class),
                prosesseringAsynkTjeneste, behandlingRepository, klageRepository, behandlingskontrollTjeneste,
                repositoryProvider.getBehandlingsresultatRepository(), mock(BehandlingRelasjonEventPubliserer.class));
        return new KlagevurderingOppdaterer(historikkApplikasjonTjeneste, behandlingsutredningTjeneste, mock(BehandlingskontrollTjeneste.class), klageVurderingTjeneste);
    }


}
