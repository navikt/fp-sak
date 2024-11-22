package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.FatterVedtakAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.FatterVedtakAksjonspunktOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakAksjonspunktOppdaterer;

class AksjonspunktOppdatererTest extends EntityManagerAwareTest {

    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String ANSVARLIG_SAKSBEHANLDER = "saksbehandler";
    private static final String OVERSKRIFT = "overskrift";
    private static final String FRITEKST = "fritekst";

    private LocalDate now = LocalDate.now();

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingRepositoryProvider repositoryProvider;
    private LagretVedtakRepository lagretVedtakRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;
    private TotrinnRepository totrinnRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;

    @BeforeEach
    public void setup() {
        var em = getEntityManager();

        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(new BehandlingskontrollServiceProvider(em, new BehandlingModellRepository(),
                mock(BehandlingskontrollEventPubliserer.class)));
        behandlingDokumentRepository = new BehandlingDokumentRepository(em);
        lagretVedtakRepository = new LagretVedtakRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        behandlingsresultatRepository = new BehandlingsresultatRepository(em);
        totrinnRepository = new TotrinnRepository(em);
        var totrinnTjeneste = new TotrinnTjeneste(totrinnRepository);
        vedtakTjeneste = new VedtakTjeneste(behandlingRepository, behandlingsresultatRepository, new HistorikkRepository(em), lagretVedtakRepository,
                totrinnTjeneste);
        historikkinnslagRepository = repositoryProvider.getHistorikkinnslag2Repository();
        fatterVedtakAksjonspunkt = new FatterVedtakAksjonspunkt(behandlingskontrollTjeneste, vedtakTjeneste,
                totrinnTjeneste, mock(InntektArbeidYtelseTjeneste.class), behandlingRepository);
    }

    @Test
    void bekreft_foreslå_vedtak_aksjonspunkt_lagrer_begrunnelse_og_overstyrende_fritekst_i_behandling_dokument() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medFødselsDato(now);
        var behandling = scenario.lagre(repositoryProvider);

        var dto = new ForeslåVedtakAksjonspunktDto(BEGRUNNELSE, OVERSKRIFT, FRITEKST, true);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
                behandlingRepository, behandlingsresultatRepository, historikkinnslagRepository,
                vedtakTjeneste,
                behandlingDokumentRepository);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        // Assert
        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument).isPresent();
        assertThat(behandlingDokument.get().getVedtakFritekst()).isEqualTo(BEGRUNNELSE);
        assertThat(behandlingDokument.get().getOverstyrtBrevOverskrift()).isEqualTo(OVERSKRIFT);
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekst()).isEqualTo(FRITEKST);
    }

    @Test
    void bekreft_foreslå_vedtak_aksjonspunkt_uten_overstyrende_fritekst_fjerner_fritekst_i_behandling_dokument() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(now);
        var behandling = scenario.lagre(repositoryProvider);

        var eksisterendeDok = BehandlingDokumentEntitet.Builder.ny()
                .medOverstyrtBrevFritekst("123")
                .medOverstyrtBrevOverskrift("345")
                .medBehandling(behandling.getId())
                .build();
        behandlingDokumentRepository.lagreOgFlush(eksisterendeDok);

        var dto = new ForeslåVedtakAksjonspunktDto(null, null, null, false);
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
    }

    @Test
    void oppdaterer_aksjonspunkt_med_beslutters_vurdering_ved_totrinnskontroll() {
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);

        scenario.medSøknadHendelse().medFødselsDato(now);
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
        scenario.medSøknadHendelse().medFødselsDato(now);
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

}
