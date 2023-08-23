package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.event.BehandlingRelasjonEventPubliserer;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormkravAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormkravOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageTilbakekrevingDto;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.TilbakekrevingVedtakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KlageFormkravOppdatererTest extends EntityManagerAwareTest {

    private static final UUID TILBAKEKREVING_BEHANDLING_UUID = UUID.randomUUID();
    private static final String TILBAKEKREVING_BEHANDLING_TYPE_NAVN = "Tilbakekreving";
    private static final String TILBAKEKREVING_BEHANDLING_TYPE = "BT-007";

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    @Mock
    private FptilbakeRestKlient mockFptilbakeRestKlient;

    private KlageFormkravOppdaterer klageFormkravOppdaterer;
    private Behandling behandling;
    ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        klageRepository = new KlageRepository(getEntityManager());
        historikkTjenesteAdapter = new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(), mock(DokumentArkivTjeneste.class),
            behandlingRepository);
        klageVurderingTjeneste = new KlageVurderingTjeneste(null, null, null, behandlingRepository, klageRepository, null,
            repositoryProvider.getBehandlingsresultatRepository(), mock(BehandlingRelasjonEventPubliserer.class));
        klageFormkravOppdaterer = new KlageFormkravOppdaterer(klageVurderingTjeneste, historikkTjenesteAdapter, behandlingRepository,
            repositoryProvider.getBehandlingVedtakRepository(), mockFptilbakeRestKlient, mock(BehandlingskontrollTjeneste.class));

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP, BehandlingStegType.KLAGE_NFP);
    }

    private void initKlage() {
        behandling = scenario.lagre(repositoryProvider);
        klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_for_tilbakekreving_behandling() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling, null), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        var oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();

        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdBehandlingId()).isEmpty();
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isEqualTo(Optional.of(TILBAKEKREVING_BEHANDLING_UUID));
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_til_tilbakekreving_behandling() {
        initKlage();
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(false, null);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling, null), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));
        var oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);
        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdBehandlingId()).isNotEmpty();

        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdBehandlingId()).isEmpty();
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isEqualTo(Optional.of(TILBAKEKREVING_BEHANDLING_UUID));

        var historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer(),
            URI.create("http://dummy/dummy")).stream().sorted(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt)).toList();
        assertThat(historikkInnslager).hasSize(2);
        var historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        var historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && BehandlingType.FØRSTEGANGSSØKNAD.getNavn().equals(historikkinnslagEndretFeltDto.getTilVerdi().toString().trim()))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_tilbakekreving_behandling() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling, null), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));
        var oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isNotEmpty();

        var nyTilbakekrevingUUID = UUID.randomUUID();
        var vedtakDato = LocalDate.now().minusDays(1);
        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(
            new TilbakekrevingVedtakDto(1L, klageTilbakekrevingDto.tilbakekrevingVedtakDato(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType()));
        klageTilbakekrevingDto = new KlageTilbakekrevingDto(nyTilbakekrevingUUID, vedtakDato, TILBAKEKREVING_BEHANDLING_TYPE);
        klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdBehandlingId()).isEmpty();
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isEqualTo(Optional.of(nyTilbakekrevingUUID));

        var historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer(),
            URI.create("http://dummy/dummy")).stream().sorted(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt)).toList();
        assertThat(historikkInnslager).hasSize(2);
        var historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        var historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && historikkinnslagEndretFeltDto.getTilVerdi()
                    .equals(TILBAKEKREVING_BEHANDLING_TYPE_NAVN + " " + vedtakDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))).isTrue();
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_ingen_påklagd_vedtak() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling, null), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));
        var oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isNotEmpty();

        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(
            new TilbakekrevingVedtakDto(1L, klageTilbakekrevingDto.tilbakekrevingVedtakDato(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType()));
        klageFormkravAksjonspunktDto = new KlageFormkravAksjonspunktDto(true, true, true, true, null, "test", false, null);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        var historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer(),
            URI.create("http://dummy/dummy")).stream().sorted(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt)).toList();
        assertThat(historikkInnslager).hasSize(2);
        var historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        var historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && historikkinnslagEndretFeltDto.getTilVerdi().equals("Ikke påklagd et vedtak"))).isTrue();
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_fpsak_behandling() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling, null), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));
        var oppdateringResultat = klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isNotEmpty();

        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(
            new TilbakekrevingVedtakDto(1L, klageTilbakekrevingDto.tilbakekrevingVedtakDato(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType()));
        klageFormkravAksjonspunktDto = new KlageFormkravAksjonspunktDto(true, true, true, true, behandling.getUuid(), "test", false, null);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        var historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer(),
            URI.create("http://dummy/dummy")).stream().sorted(Comparator.comparing(HistorikkinnslagDto::getOpprettetTidspunkt)).toList();
        assertThat(historikkInnslager).hasSize(2);
        var historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        var historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && historikkinnslagEndretFeltDto.getTilVerdi().toString().contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslagDto = historikkInnslager.get(1);
        assertThat(historikkinnslagDto.getHistorikkinnslagDeler()).hasSize(1);
        historikkinnslagEndretFelter = historikkinnslagDto.getHistorikkinnslagDeler().get(0).getEndredeFelter();
        assertThat(historikkinnslagEndretFelter.stream()
            .anyMatch(historikkinnslagEndretFeltDto ->
                historikkinnslagEndretFeltDto.getEndretFeltNavn().equals(HistorikkEndretFeltType.PA_KLAGD_BEHANDLINGID)
                    && BehandlingType.FØRSTEGANGSSØKNAD.getNavn().equals(historikkinnslagEndretFeltDto.getTilVerdi().toString().trim()))).isTrue();
    }

    private void fellesKlageAssert() {
        var klageFormkravEntitet = klageRepository.hentKlageFormkrav(behandling.getId(), KlageVurdertAv.NFP);
        assertThat(klageFormkravEntitet).isPresent();
        var formkravEntitet = klageFormkravEntitet.get();
        assertThat(formkravEntitet.erFristOverholdt()).isTrue();
        assertThat(formkravEntitet.erKlagerPart()).isTrue();
        assertThat(formkravEntitet.erKonkret()).isTrue();
        assertThat(formkravEntitet.erSignert()).isTrue();
    }

    private void fellesKlageHistoriskAssert() {
        var historikkInnslager = historikkTjenesteAdapter.hentAlleHistorikkInnslagForSak(behandling.getFagsak().getSaksnummer(),
            URI.create("http://dummy/dummy"));
        assertThat(historikkInnslager).hasSize(1);
        var historikkinnslagDto = historikkInnslager.get(0);
        assertThat(historikkinnslagDto.getType()).isEqualByComparingTo(HistorikkinnslagType.KLAGE_BEH_NFP);
    }

    private KlageFormkravAksjonspunktDto lagKlageAksjonspunktDto(boolean erTilbakekreving, KlageTilbakekrevingDto klageTilbakekrevingDto) {
        return new KlageFormkravAksjonspunktDto(true, true, true, true, behandling.getUuid(), "test", erTilbakekreving, klageTilbakekrevingDto);
    }

}
