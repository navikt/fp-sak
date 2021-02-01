package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktDto.KlageVurderingResultatNfpAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktDto.KlageVurderingResultatNkAksjonspunktDto;
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
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    // TODO (FLUORITT): Renskriv tester med færre mocks
    @BeforeEach
    public void oppsett() {
        lenient().when(behandlendeEnhetTjeneste.getKlageInstans()).thenReturn(new OrganisasjonsEnhet("4292", "NAV Klageinstans Midt-Norge"));
    }

    @Test
    public void skal_bestille_dokument_ved_stadfestet_ytelsesvedtak_og_lagre_KlageVurderingResultat() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();

        var klageScenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        Behandling behandling = klageScenario.lagre(repositoryProvider, klageRepository);

        KlageVurdering klageVurdering = KlageVurdering.STADFESTE_YTELSESVEDTAK;
        KlageVurderingResultatNfpAksjonspunktDto dto = new KlageVurderingResultatNfpAksjonspunktDto("begrunnelse bla. bla.",
                klageVurdering, null, null, LocalDate.now(), "Fritekst til brev", null);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        getKlageVurderer(repositoryProvider, klageRepository).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert

        // verifiserer KlageVurderingResultat
        KlageVurderingResultat klageVurderingResultat = klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP).get();
        assertThat(klageVurderingResultat.getKlageVurdering()).isEqualTo(KlageVurdering.STADFESTE_YTELSESVEDTAK);
        assertThat(klageVurderingResultat.getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NFP);
        assertThat(klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP)).isEqualTo(Optional.of(klageVurderingResultat));

        // verifiserer BestillBrevDto
        ArgumentCaptor<BestillBrevDto> brevDtoCaptor = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentBestillerTjeneste).bestillDokument(brevDtoCaptor.capture(), eq(HistorikkAktør.SAKSBEHANDLER), eq(false));
        BestillBrevDto bestillBrevDto = brevDtoCaptor.getValue();
        assertThat(bestillBrevDto.getBrevmalkode()).isEqualTo(DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS.getKode());
        assertThat(bestillBrevDto.getFritekst()).isNull();

        // Verifiserer HistorikkinnslagDto
        ArgumentCaptor<Historikkinnslag> historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkApplikasjonTjeneste).lagInnslag(historikkCapture.capture());
        Historikkinnslag historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
                .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.KLAGE_BEH_NFP.getKode()));
        assertThat(del.getEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_NFP)).isNotNull();

        // Verifiserer at behandlende enhet er byttet til NAV Klageinstans
        ArgumentCaptor<OrganisasjonsEnhet> enhetCapture = ArgumentCaptor.forClass(OrganisasjonsEnhet.class);
        verify(behandlingsutredningTjeneste).byttBehandlendeEnhet(anyLong(), enhetCapture.capture(), eq(""),
                eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        OrganisasjonsEnhet enhet = enhetCapture.getValue();
        assertThat(enhet.getEnhetId()).isEqualTo(behandlendeEnhetTjeneste.getKlageInstans().getEnhetId());
        assertThat(enhet.getEnhetNavn()).isEqualTo(behandlendeEnhetTjeneste.getKlageInstans().getEnhetNavn());
        assertThat(behandling.getBehandlingsresultat().getBehandlingResultatType())
                .isEqualTo(BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET);
    }

    @Test
    public void skal_ikke_bestille_dokument_ved_stadfestet_ytelsesvedtak_når_behandlingen_er_migrert() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();

        var klageScenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        Behandling behandling = klageScenario.lagre(repositoryProvider, klageRepository);
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);

        KlageVurdering klageVurdering = KlageVurdering.STADFESTE_YTELSESVEDTAK;
        KlageVurderingResultatNfpAksjonspunktDto dto = new KlageVurderingResultatNfpAksjonspunktDto("begrunnelse bla. bla.",
                klageVurdering, null, null, LocalDate.now(), "Fritekst til brev", null);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        getKlageVurderer(repositoryProvider, klageRepository).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        verify(dokumentBestillerTjeneste, times(0)).bestillDokument(any(), any());
    }

    private KlagevurderingOppdaterer getKlageVurderer(BehandlingRepositoryProvider repositoryProvider, KlageRepository klageRepository) {
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        final KlageVurderingTjeneste klageVurderingTjeneste = new KlageVurderingTjeneste(dokumentBestillerTjeneste,
                prosesseringAsynkTjeneste, behandlingRepository, klageRepository, behandlingskontrollTjeneste,
                repositoryProvider.getBehandlingsresultatRepository());
        return new KlagevurderingOppdaterer(historikkApplikasjonTjeneste, behandlingsutredningTjeneste, klageVurderingTjeneste,
                behandlendeEnhetTjeneste);
    }

    @Test
    public void skal_sette_BehandlingResultatType_AvvisKlage_for_Nk_når_klaget_er_for_sent() {
        // Arrange
        KlageVurdering klageVurdering = KlageVurdering.AVVIS_KLAGE;
        KlageAvvistÅrsak klageAvvistÅrsak = KlageAvvistÅrsak.KLAGET_FOR_SENT;
        KlageVurderingResultatNkAksjonspunktDto dto = new KlageVurderingResultatNkAksjonspunktDto("begrunnelse for avvist klage NK...",
                klageVurdering, null, klageAvvistÅrsak, LocalDate.now(), "Fritekst til Brev", null, false);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var klageScenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(scenario);
        Behandling klageBehandling = klageScenario.lagre(repositoryProvider, klageRepository);

        // Act
        var aksjonspunkt = klageBehandling.getAksjonspunktFor(dto.getKode());
        getKlageVurderer(repositoryProvider, klageRepository).oppdater(dto, new AksjonspunktOppdaterParameter(klageBehandling, aksjonspunkt, dto));

        // Assert
        assertThat(klageBehandling.getBehandlingsresultat().getBehandlingResultatType()).isEqualTo(BehandlingResultatType.KLAGE_AVVIST);

        // verifiserer KlageVurderingResultat
        KlageVurderingResultat klageVurderingResultat = klageRepository.hentKlageVurderingResultat(klageBehandling.getId(), KlageVurdertAv.NK).get();
        assertThat(klageVurderingResultat.getKlageVurdering()).isEqualTo(KlageVurdering.AVVIS_KLAGE);
        assertThat(klageVurderingResultat.getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NK);

        // Verifiserer HistorikkinnslagDto
        ArgumentCaptor<Historikkinnslag> historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkApplikasjonTjeneste).lagInnslag(historikkCapture.capture());
        Historikkinnslag historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.KLAGE_BEH_NK);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke")
                .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.KLAGE_BEH_NK.getKode()));
        assertThat(del.getEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_KA)).isNotNull();
        assertThat(del.getEndretFelt(HistorikkEndretFeltType.KLAGE_OMGJØR_ÅRSAK)).isNotNull();
    }

}
