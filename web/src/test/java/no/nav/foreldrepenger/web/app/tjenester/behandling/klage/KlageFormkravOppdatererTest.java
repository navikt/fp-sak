package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageFormkravTjeneste;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagEndretFeltDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormkravAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormkravOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageTilbakekrevingDto;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.TilbakekrevingVedtakDto;

public class KlageFormkravOppdatererTest {

    private static final UUID TILBAKEKREVING_BEHANDLING_UUID = UUID.randomUUID();
    private static final String TILBAKEKREVING_BEHANDLING_TYPE_NAVN = "Tilbakekreving";
    private static final String TILBAKEKREVING_BEHANDLING_TYPE = "BT-007";

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private KlageRepository klageRepository = new KlageRepository(repositoryProvider.getEntityManager());
    private KlageFormkravTjeneste klageFormkravTjeneste = new KlageFormkravTjeneste(behandlingRepository,klageRepository);
    private HistorikkTjenesteAdapter historikkTjenesteAdapter = new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(),new HistorikkInnslagKonverter(), mock(DokumentArkivTjeneste.class));
    private KlageVurderingTjeneste mockKlageVurderingTjeneste = mock(KlageVurderingTjeneste.class);
    private FptilbakeRestKlient mockFptilbakeRestKlient = mock(FptilbakeRestKlient.class);

    private KlageFormkravOppdaterer klageFormkravOppdaterer = new KlageFormkravOppdaterer(klageFormkravTjeneste,mockKlageVurderingTjeneste, historikkTjenesteAdapter,behandlingRepository,klageRepository,repositoryProvider.getBehandlingVedtakRepository(),mockFptilbakeRestKlient);

    private ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
    private Behandling behandling;

    @Before
    public void setup(){
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP, BehandlingStegType.KLAGE_NFP);
        behandling = scenario.lagre(repositoryProvider);
        klageRepository.hentEvtOpprettKlageResultat(behandling);
    }

    @Test
    public void skal_oppdatere_klage_aksjonspunkt_for_tilbakekreving_behandling(){
        KlageTilbakekrevingDto klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        KlageFormkravAksjonspunktDto klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true,klageTilbakekrevingDto);
        AksjonspunktOppdaterParameter aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(behandling, behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP),klageFormkravAksjonspunktDto);

        OppdateringResultat oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);
        assertThat(oppdateringResultat.getNesteAksjonspunktStatus()).isEqualToComparingFieldByField(AksjonspunktStatus.UTFØRT);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();

        KlageResultatEntitet klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdBehandling()).isEmpty();
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandling()).isEqualTo(Optional.of(TILBAKEKREVING_BEHANDLING_UUID));
    }

    @Test
    public void skal_oppdatere_klage_aksjonspunkt_til_tilbakekreving_behandling(){
        KlageFormkravAksjonspunktDto klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(false,null);
        AksjonspunktOppdaterParameter aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(behandling, behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP),klageFormkravAksjonspunktDto);
        OppdateringResultat oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);
        assertThat(oppdateringResultat.getNesteAksjonspunktStatus()).isEqualToComparingFieldByField(AksjonspunktStatus.UTFØRT);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        KlageResultatEntitet klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdBehandling()).isNotEmpty();

        KlageTilbakekrevingDto klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true,klageTilbakekrevingDto);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdBehandling()).isEmpty();
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandling()).isEqualTo(Optional.of(TILBAKEKREVING_BEHANDLING_UUID));

        List<HistorikkinnslagDto> historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer());
        assertThat(historikkInnslager.size()).isEqualTo(2);
        historikkInnslager.sort(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt));
        HistorikkinnslagDto historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        List<HistorikkinnslagEndretFeltDto> historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && BehandlingType.FØRSTEGANGSSØKNAD.getNavn().equals(historikkinnslagEndretFeltDto.getTilVerdi().toString().trim()))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();
    }

    @Test
    public void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_tilbakekreving_behandling(){

        KlageTilbakekrevingDto klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        KlageFormkravAksjonspunktDto klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true,klageTilbakekrevingDto);
        AksjonspunktOppdaterParameter aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(behandling, behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP),klageFormkravAksjonspunktDto);
        OppdateringResultat oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        assertThat(oppdateringResultat.getNesteAksjonspunktStatus()).isEqualToComparingFieldByField(AksjonspunktStatus.UTFØRT);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        KlageResultatEntitet klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandling()).isNotEmpty();

        UUID nyTilbakekrevingUUID = UUID.randomUUID();
        LocalDate vedtakDato = LocalDate.now().minusDays(1);
        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(new TilbakekrevingVedtakDto(1L,klageTilbakekrevingDto.getTilbakekrevingVedtakDato(),klageTilbakekrevingDto.getTilbakekrevingBehandlingType()));
        klageTilbakekrevingDto = new KlageTilbakekrevingDto(nyTilbakekrevingUUID, vedtakDato, TILBAKEKREVING_BEHANDLING_TYPE);
        klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true,klageTilbakekrevingDto);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdBehandling()).isEmpty();
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandling()).isEqualTo(Optional.of(nyTilbakekrevingUUID));

        List<HistorikkinnslagDto> historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer());
        assertThat(historikkInnslager.size()).isEqualTo(2);
        historikkInnslager.sort(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt));
        HistorikkinnslagDto historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        List<HistorikkinnslagEndretFeltDto> historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && historikkinnslagEndretFeltDto.getTilVerdi().equals(TILBAKEKREVING_BEHANDLING_TYPE_NAVN + " "+vedtakDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))).isTrue();
    }

    @Test
    public void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_ingen_påklagd_vedtak(){
        KlageTilbakekrevingDto klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        KlageFormkravAksjonspunktDto klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true,klageTilbakekrevingDto);
        AksjonspunktOppdaterParameter aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(behandling, behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP),klageFormkravAksjonspunktDto);
        OppdateringResultat oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        assertThat(oppdateringResultat.getNesteAksjonspunktStatus()).isEqualToComparingFieldByField(AksjonspunktStatus.UTFØRT);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        KlageResultatEntitet klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandling()).isNotEmpty();

        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(new TilbakekrevingVedtakDto(1L,klageTilbakekrevingDto.getTilbakekrevingVedtakDato(),klageTilbakekrevingDto.getTilbakekrevingBehandlingType()));
        klageFormkravAksjonspunktDto = new KlageFormkravAksjonspunktDto.KlageFormkravNfpAksjonspunktDto(true,true,true,true,null,"test",false,null);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        List<HistorikkinnslagDto> historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer());
        assertThat(historikkInnslager.size()).isEqualTo(2);
        historikkInnslager.sort(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt));
        HistorikkinnslagDto historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        List<HistorikkinnslagEndretFeltDto> historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && historikkinnslagEndretFeltDto.getTilVerdi().equals("Ikke påklagd et vedtak"))).isTrue();
    }

    @Test
    public void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_fpsak_behandling(){
        KlageTilbakekrevingDto klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        KlageFormkravAksjonspunktDto klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true,klageTilbakekrevingDto);
        AksjonspunktOppdaterParameter aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(behandling, behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP),klageFormkravAksjonspunktDto);
        OppdateringResultat oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        assertThat(oppdateringResultat.getNesteAksjonspunktStatus()).isEqualToComparingFieldByField(AksjonspunktStatus.UTFØRT);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        KlageResultatEntitet klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling);
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandling()).isNotEmpty();

        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(new TilbakekrevingVedtakDto(1L,klageTilbakekrevingDto.getTilbakekrevingVedtakDato(),klageTilbakekrevingDto.getTilbakekrevingBehandlingType()));
        klageFormkravAksjonspunktDto = new KlageFormkravAksjonspunktDto.KlageFormkravNfpAksjonspunktDto(true,true,true,true,behandling.getId(),"test",false,null);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto,aksjonspunktOppdaterParameter);

        List<HistorikkinnslagDto> historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer());
        assertThat(historikkInnslager.size()).isEqualTo(2);
        historikkInnslager.sort(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt));
        HistorikkinnslagDto historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        List<HistorikkinnslagEndretFeltDto> historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler().size()).isEqualTo(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream().
            anyMatch(historikkinnslagEndretFeltDto -> historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                && BehandlingType.FØRSTEGANGSSØKNAD.getNavn().equals(historikkinnslagEndretFeltDto.getTilVerdi().toString().trim()))).isTrue();
    }


    private void fellesKlageAssert() {
        Optional<KlageFormkravEntitet> klageFormkravEntitet = klageRepository.hentKlageFormkrav(behandling, KlageVurdertAv.NFP);
        assertThat(klageFormkravEntitet).isPresent();
        KlageFormkravEntitet formkravEntitet = klageFormkravEntitet.get(); //NOSONAR
        assertThat(formkravEntitet.erFristOverholdt()).isTrue();
        assertThat(formkravEntitet.erKlagerPart()).isTrue();
        assertThat(formkravEntitet.erKonkret()).isTrue();
        assertThat(formkravEntitet.erSignert()).isTrue();
    }

    private void fellesKlageHistoriskAssert() {
        List<HistorikkinnslagDto> historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer());
        assertThat(historikkInnslager.size()).isEqualTo(1);
        HistorikkinnslagDto historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
    }

    private KlageFormkravAksjonspunktDto lagKlageAksjonspunktDto(boolean erTilbakekreving, KlageTilbakekrevingDto klageTilbakekrevingDto){
        return new KlageFormkravAksjonspunktDto.KlageFormkravNfpAksjonspunktDto(true,true,true,true,behandling.getId(),"test",erTilbakekreving,klageTilbakekrevingDto);
    }

}
