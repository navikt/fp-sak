package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.FatterVedtakAksjonspunktDto;
import no.nav.vedtak.exception.TekniskException;

class ForeslåOgFatteVedtakAksjonspunktTest extends EntityManagerAwareTest {

    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String FRITEKST = "fritekst";
    private static final LocalDate NOW = LocalDate.now();

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;
    private TotrinnRepository totrinnRepository;
    private VedtakTjeneste vedtakTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @BeforeEach
    public void setup() {
        var em = getEntityManager();
        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(new BehandlingskontrollServiceProvider(em, new BehandlingModellRepository(),
                mock(BehandlingskontrollEventPubliserer.class)));
        var lagretVedtakRepository = new LagretVedtakRepository(em);
        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingDokumentRepository = new BehandlingDokumentRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        behandlingsresultatRepository = new BehandlingsresultatRepository(em);
        totrinnRepository = new TotrinnRepository(em);
        var totrinnTjeneste = new TotrinnTjeneste(totrinnRepository);
        vedtakTjeneste = new VedtakTjeneste(behandlingRepository, behandlingsresultatRepository, repositoryProvider.getHistorikkinnslagRepository(),
            lagretVedtakRepository, totrinnTjeneste);
        historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        fatterVedtakAksjonspunkt = new FatterVedtakAksjonspunkt(behandlingskontrollTjeneste, vedtakTjeneste, totrinnTjeneste,
            mock(InntektArbeidYtelseTjeneste.class), behandlingRepository);
    }

    @Test
    void bekreft_foreslå_vedtak_aksjonspunkt_overstyrer_behandlingresultat_vedtaksbrev_ved_overstyring() {
        // Arrange
        var behandling = behandlingMedTidligereOverstyringAvBrev(FRITEKST);
        var dto = new ForeslåVedtakAksjonspunktDto(BEGRUNNELSE, true);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            behandlingRepository,
            behandlingsresultatRepository,
            historikkinnslagRepository,
            vedtakTjeneste,
            behandlingDokumentRepository);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getVedtakFritekst()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekstHtml()).isEqualTo(FRITEKST);

        var behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.FRITEKST);
    }

    @Test
    void bekreft_foreslå_vedtak_aksjonspunkt_skal_hive_exception_hvis_det_ikke_foreligger_en_overstyring() {
        // Arrange
        var behandling = behandlingMedTidligereOverstyringAvBrev(null);

        var dto = new ForeslåVedtakAksjonspunktDto(BEGRUNNELSE, true);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            behandlingRepository,
            behandlingsresultatRepository,
            historikkinnslagRepository,
            vedtakTjeneste,
            behandlingDokumentRepository);

        // Act
        assertThatThrownBy(() -> foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto)))
            .isInstanceOf(TekniskException.class)
            .hasMessageContaining("FP-666916:");
    }


    @Test
    void foreslå_vedtak_med_automatisk_brev_skal_fjerne_gammel_overstyring_men_ikke_utfyllende_tekst() {
        // Arrange
        var behandling = behandlingMedTidligereOverstyringAvBrev(FRITEKST);

        var dto = new ForeslåVedtakAksjonspunktDto("begrunnelse", false);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
                behandlingRepository, behandlingsresultatRepository, historikkinnslagRepository,
                vedtakTjeneste,
                behandlingDokumentRepository);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getOverstyrtBrevOverskrift()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekst()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekstHtml()).isNull();
        assertThat(behandlingDokument.get().getVedtakFritekst()).isEqualTo(dto.getBegrunnelse()); // Utfyllende tekst for automatisk vedtaksbrev

        var behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
    }

    @Test
    void foreslå_vedtak_med_automatisk_brev_skal_fjerne_gammel_overstyring_og_utfyllende_tekst_hvis_begrunnelse_er_null() {
        // Arrange
        var behandling = behandlingMedTidligereOverstyringAvBrev(FRITEKST);

        var dto = new ForeslåVedtakAksjonspunktDto(null, false);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            behandlingRepository, behandlingsresultatRepository, historikkinnslagRepository,
            vedtakTjeneste,
            behandlingDokumentRepository);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getOverstyrtBrevOverskrift()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekst()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekstHtml()).isNull();
        assertThat(behandlingDokument.get().getVedtakFritekst()).isNull();

        var behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
    }

    @Test
    void bekreft_foreslå_vedtak_ap_med_utfyllende_tekst_for_automatisk_overstyrt_brev() {
        // Arrange
        var behandling = behandlingMedTidligereOverstyringAvBrev(FRITEKST);

        var dto = new ForeslåVedtakAksjonspunktDto("begrunnelse", false);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            behandlingRepository, behandlingsresultatRepository, historikkinnslagRepository,
            vedtakTjeneste,
            behandlingDokumentRepository);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getOverstyrtBrevOverskrift()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekst()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekstHtml()).isNull();

        var behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        assertThat(behandlingsresultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.AUTOMATISK);
    }


    @Test
    void oppdaterer_aksjonspunkt_med_beslutters_vurdering_ved_totrinnskontroll() {
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);

        scenario.medSøknadHendelse().medFødselsDato(NOW);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);
        var behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setArsaker(Set.of(VurderÅrsak.FEIL_FAKTA));
        aksGodkjDto.setGodkjent(false);
        var besluttersBegrunnelse = "Må ha bedre dokumentasjon.";
        aksGodkjDto.setBegrunnelse(besluttersBegrunnelse);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
                new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), aksjonspunktDto, null));

        var totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        assertThat(totrinnsvurderinger).hasSize(1);
        var totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isFalse();
        assertThat(totrinnsvurdering.getBegrunnelse()).isEqualTo(besluttersBegrunnelse);
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).hasSize(1);
        var vurderPåNyttÅrsak = totrinnsvurdering.getVurderPåNyttÅrsaker().iterator().next();
        assertThat(vurderPåNyttÅrsak.getÅrsaksType()).isEqualTo(VurderÅrsak.FEIL_FAKTA);
    }

    @Test
    void oppdaterer_aksjonspunkt_med_godkjent_totrinnskontroll() {
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medFødselsDato(NOW);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);

        var behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setGodkjent(true);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
                new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), aksjonspunktDto, null));

        var totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        assertThat(totrinnsvurderinger).hasSize(1);
        var totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isTrue();
        assertThat(totrinnsvurdering.getBegrunnelse()).isNullOrEmpty();
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).isEmpty();
    }

    private Behandling behandlingMedTidligereOverstyringAvBrev(String redigertBrev) {
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builder().medVedtaksbrev(Vedtaksbrev.FRITEKST));
        scenario.medSøknadHendelse().medFødselsDato(NOW);
        var behandling = scenario.lagre(repositoryProvider);

        var eksisterendeDok = BehandlingDokumentEntitet.Builder.ny()
            .medBehandling(behandling.getId())
            .medOverstyrtBrevOverskrift("123")
            .medOverstyrtBrevFritekst("456")
            .medOverstyrtBrevFritekstHtml(redigertBrev)
            .build();
        behandlingDokumentRepository.lagreOgFlush(eksisterendeDok);
        return behandling;
    }

}
