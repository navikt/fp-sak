package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormkravAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormkravOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageHistorikkinnslag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageTilbakekrevingDto;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.TilbakekrevingVedtakDto;

@ExtendWith(MockitoExtension.class)
class KlageFormkravOppdatererTest extends EntityManagerAwareTest {

    private static final UUID TILBAKEKREVING_BEHANDLING_UUID = UUID.randomUUID();
    private static final String TILBAKEKREVING_BEHANDLING_TYPE_NAVN = "Tilbakekreving";
    private static final String TILBAKEKREVING_BEHANDLING_TYPE = "BT-007";

    private KlageRepository klageRepository;
    @Mock
    private FptilbakeRestKlient mockFptilbakeRestKlient;

    private KlageFormkravOppdaterer klageFormkravOppdaterer;
    private Behandling behandling;
    ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
    private BehandlingRepositoryProvider repositoryProvider;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        klageRepository = new KlageRepository(getEntityManager());
        KlageVurderingTjeneste klageVurderingTjeneste = new KlageVurderingTjeneste(null, null, null, behandlingRepository, klageRepository, null,
            repositoryProvider.getBehandlingsresultatRepository(), mock(BehandlingEventPubliserer.class));
        var formHistorikk = new KlageHistorikkinnslag(repositoryProvider.getHistorikkinnslag2Repository(), behandlingRepository,
            repositoryProvider.getBehandlingVedtakRepository(), mockFptilbakeRestKlient);
        klageFormkravOppdaterer = new KlageFormkravOppdaterer(klageVurderingTjeneste, behandlingRepository, mock(BehandlingskontrollTjeneste.class),
            formHistorikk);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP, BehandlingStegType.KLAGE_NFP);
        historikkinnslag2Repository = repositoryProvider.getHistorikkinnslag2Repository();
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
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

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
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);
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

        var historikk = historikkinnslag2Repository.hent(behandling.getSaksnummer())
            .stream()
            .sorted(Comparator.comparing(Historikkinnslag2::getOpprettetTidspunkt))
            .toList();
        assertThat(historikk).hasSize(2);
        var historikkinnslag = historikk.getFirst();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualByComparingTo(SkjermlenkeType.FORMKRAV_KLAGE_NFP);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(6);
        var historikkinnslagTekstlinjer = historikkinnslag.getTekstlinjer();


        assertThat(historikkinnslagTekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(BehandlingType.FØRSTEGANGSSØKNAD.getNavn()))).isTrue();

        historikkinnslag = historikk.get(1);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        historikkinnslagTekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(historikkinnslagTekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_tilbakekreving_behandling() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

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

        var historikk = historikkinnslag2Repository.hent(behandling.getSaksnummer())
            .stream()
            .sorted(Comparator.comparing(Historikkinnslag2::getOpprettetTidspunkt))
            .toList();
        assertThat(historikk).hasSize(2);
        var historikkinnslag = historikk.get(0);
        assertThat(historikkinnslag.getSkjermlenke()).isEqualByComparingTo(SkjermlenkeType.FORMKRAV_KLAGE_NFP);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(6);
        var tekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(tekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslag = historikk.get(1);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        tekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(tekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN + " " + vedtakDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))).isTrue();
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_ingen_påklagd_vedtak() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isNotEmpty();

        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(
            new TilbakekrevingVedtakDto(1L, klageTilbakekrevingDto.tilbakekrevingVedtakDato(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType()));
        klageFormkravAksjonspunktDto = new KlageFormkravAksjonspunktDto(true, true, true, true, null, "test", false, null, null);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        var historikk = historikkinnslag2Repository.hent(behandling.getSaksnummer())
            .stream()
            .sorted(Comparator.comparing(Historikkinnslag2::getOpprettetTidspunkt))
            .toList();
        assertThat(historikk).hasSize(2);
        var historikkinnslag = historikk.get(0);
        assertThat(historikkinnslag.getSkjermlenke()).isEqualByComparingTo(SkjermlenkeType.FORMKRAV_KLAGE_NFP);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(6);
        var tekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(tekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslag = historikk.get(1);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        tekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(tekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains("Ikke påklagd et vedtak"))).isTrue();
    }

    @Test
    void skal_oppdatere_klage_aksjonspunkt_fra_tilbakekreving_til_fpsak_behandling() {
        initKlage();
        var klageTilbakekrevingDto = new KlageTilbakekrevingDto(TILBAKEKREVING_BEHANDLING_UUID, LocalDate.now(), TILBAKEKREVING_BEHANDLING_TYPE);
        var klageFormkravAksjonspunktDto = lagKlageAksjonspunktDto(true, klageTilbakekrevingDto);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageFormkravAksjonspunktDto,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        fellesKlageAssert();
        fellesKlageHistoriskAssert();
        var klageResultatEntitet = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        assertThat(klageResultatEntitet.getPåKlagdEksternBehandlingUuid()).isNotEmpty();

        when(mockFptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(TILBAKEKREVING_BEHANDLING_UUID)).thenReturn(
            new TilbakekrevingVedtakDto(1L, klageTilbakekrevingDto.tilbakekrevingVedtakDato(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType()));
        klageFormkravAksjonspunktDto = new KlageFormkravAksjonspunktDto(true, true, true, true, behandling.getUuid(), "test", false, null, null);
        klageFormkravOppdaterer.oppdater(klageFormkravAksjonspunktDto, aksjonspunktOppdaterParameter);

        var historikk = historikkinnslag2Repository.hent(behandling.getSaksnummer())
            .stream()
            .sorted(Comparator.comparing(Historikkinnslag2::getOpprettetTidspunkt))
            .toList();
        assertThat(historikk).hasSize(2);
        var historikkinnslag = historikk.get(0);
        assertThat(historikkinnslag.getSkjermlenke()).isEqualByComparingTo(SkjermlenkeType.FORMKRAV_KLAGE_NFP);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(6);
        var tekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(tekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(TILBAKEKREVING_BEHANDLING_TYPE_NAVN))).isTrue();

        historikkinnslag = historikk.get(1);
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        tekstlinjer = historikkinnslag.getTekstlinjer();
        assertThat(tekstlinjer.stream()
            .anyMatch(tekstlinje -> tekstlinje.getTekst().contains(KlageHistorikkinnslag.PÅKLAGD_BEHANDLING) && tekstlinje.getTekst()
                .contains(BehandlingType.FØRSTEGANGSSØKNAD.getNavn()))).isTrue();
    }

    @Test
    void skal_legge_på_fritekstTilBrev_på_avvist_klagebehandling() {
        initKlage();
        final String fritekstTilBrev = "Tester at fritekst lagres på vurderingsresulatet";

        var klageAvvistMedFritekst = new KlageFormkravAksjonspunktDto(true, true, false, true, behandling.getUuid(), "test", false, null,
            fritekstTilBrev);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageAvvistMedFritekst,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        klageFormkravOppdaterer.oppdater(klageAvvistMedFritekst, aksjonspunktOppdaterParameter);

        var klageResultatVurdering = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        assertThat(klageResultatVurdering).isPresent();
        assertThat(klageResultatVurdering.map(KlageVurderingResultat::getFritekstTilBrev)).containsSame(fritekstTilBrev);
    }

    @Test
    void skal_fjerne_fritekstTilBrev_hvis_klage_ikke_avist_og_tekst_finnes() {
        initKlage();
        final String fritekstTilBrev = "Tester at fritekst lagres på vurderingsresulatet";
        var vurderingResBuilder = KlageVurderingResultat.builder().medFritekstTilBrev(fritekstTilBrev).medKlageVurdertAv(KlageVurdertAv.NFP);
        klageRepository.lagreVurderingsResultat(behandling, vurderingResBuilder);

        var klageAvvistUtenFritekst = new KlageFormkravAksjonspunktDto(true, true, true, true, behandling.getUuid(), "test", false, null, null);
        var aksjonspunktOppdaterParameter = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), klageAvvistUtenFritekst,
            behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP));

        klageFormkravOppdaterer.oppdater(klageAvvistUtenFritekst, aksjonspunktOppdaterParameter);

        var klageResultatVurdering = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        assertThat(klageResultatVurdering).isPresent();
        assertThat(klageResultatVurdering.map(KlageVurderingResultat::getFritekstTilBrev)).isEmpty();
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
        var historikk = historikkinnslag2Repository.hent(behandling.getSaksnummer());
        assertThat(historikk).hasSize(1);
        var historikkinnslagDto = historikk.get(0);
        assertThat(historikkinnslagDto.getSkjermlenke()).isEqualByComparingTo(SkjermlenkeType.FORMKRAV_KLAGE_NFP);
    }

    private KlageFormkravAksjonspunktDto lagKlageAksjonspunktDto(boolean erTilbakekreving, KlageTilbakekrevingDto klageTilbakekrevingDto) {
        return new KlageFormkravAksjonspunktDto(true, true, true, true, behandling.getUuid(), "test", erTilbakekreving, klageTilbakekrevingDto, null);
    }

}
