package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.FatterVedtakAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.FatterVedtakAksjonspunktOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslaVedtakAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakAksjonspunktOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AksjonspunktOppdatererTest extends EntityManagerAwareTest {

    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String ANSVARLIG_SAKSBEHANLDER = "saksbehandler";
    private static final String OVERSKRIFT = "overskrift";
    private static final String FRITEKST = "fritekst";

    private LocalDate now = LocalDate.now();

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingRepositoryProvider repositoryProvider;
    private OpprettToTrinnsgrunnlag opprettTotrinnsgrunnlag;
    private LagretVedtakRepository lagretVedtakRepository;
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;
    private TotrinnRepository totrinnRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private VedtakTjeneste vedtakTjeneste;

    @BeforeEach
    public void setup() {
        EntityManager em = getEntityManager();

        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(new BehandlingskontrollServiceProvider(em, new BehandlingModellRepository(),
                mock(BehandlingskontrollEventPubliserer.class)));
        behandlingDokumentRepository = new BehandlingDokumentRepository(em);
        lagretVedtakRepository = new LagretVedtakRepository(em);
        ankeRepository = new AnkeRepository(em);
        klageRepository = new KlageRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        behandlingsresultatRepository = new BehandlingsresultatRepository(em);
        totrinnRepository = new TotrinnRepository(em);
        var totrinnTjeneste = new TotrinnTjeneste(totrinnRepository);
        var klageAnkeVedtakTjeneste = new KlageAnkeVedtakTjeneste(klageRepository, ankeRepository);
        vedtakTjeneste = new VedtakTjeneste(behandlingRepository, behandlingsresultatRepository, new HistorikkRepository(em), lagretVedtakRepository,
                totrinnTjeneste, klageAnkeVedtakTjeneste);

        opprettTotrinnsgrunnlag = new OpprettToTrinnsgrunnlag(
                new HentOgLagreBeregningsgrunnlagTjeneste(em),
                new YtelsesFordelingRepository(em),
                new FpUttakRepository(em),
                totrinnTjeneste,
                new AbakusInMemoryInntektArbeidYtelseTjeneste());
        fatterVedtakAksjonspunkt = new FatterVedtakAksjonspunkt(behandlingskontrollTjeneste, klageAnkeVedtakTjeneste, vedtakTjeneste,
                totrinnTjeneste);
    }

    @Test
    public void bekreft_foreslå_vedtak_aksjonspkt_setter_ansvarlig_saksbehandler() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medFødselsDato(now);
        Behandling behandling = scenario.lagre(repositoryProvider);
        var dto = new ForeslaVedtakAksjonspunktDto(BEGRUNNELSE, null, null, false);
        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
                behandlingRepository, behandlingsresultatRepository, mock(HistorikkTjenesteAdapter.class),
                opprettTotrinnsgrunnlag,
                vedtakTjeneste,
                behandlingDokumentRepository, new KlageAnkeVedtakTjeneste(mock(KlageRepository.class), mock(AnkeRepository.class))) {
            @Override
            protected String getCurrentUserId() {
                // return test verdi
                return ANSVARLIG_SAKSBEHANLDER;
            }
        };

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        assertThat(behandling.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANLDER);
    }

    @Test
    public void bekreft_foreslå_vedtak_aksjonspunkt_lagrer_begrunnelse_og_overstyrende_fritekst_i_behandling_dokument() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medFødselsDato(now);
        Behandling behandling = scenario.lagre(repositoryProvider);

        ForeslaVedtakAksjonspunktDto dto = new ForeslaVedtakAksjonspunktDto(BEGRUNNELSE, OVERSKRIFT, FRITEKST, true);
        var klageAnkeVedtakTjeneste = new KlageAnkeVedtakTjeneste(mock(KlageRepository.class), mock(AnkeRepository.class));
        ForeslåVedtakAksjonspunktOppdaterer foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
                behandlingRepository, behandlingsresultatRepository, mock(HistorikkTjenesteAdapter.class),
                opprettTotrinnsgrunnlag,
                vedtakTjeneste,
                behandlingDokumentRepository, klageAnkeVedtakTjeneste);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        Optional<BehandlingDokumentEntitet> behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument.isPresent()).isTrue();
        assertThat(behandlingDokument.get().getVedtakFritekst()).isEqualTo(BEGRUNNELSE);
        assertThat(behandlingDokument.get().getOverstyrtBrevOverskrift()).isEqualTo(OVERSKRIFT);
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekst()).isEqualTo(FRITEKST);
    }

    @Test
    public void bekreft_foreslå_vedtak_aksjonspunkt_uten_overstyrende_fritekst_fjerner_fritekst_i_behandling_dokument() {
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

        ForeslaVedtakAksjonspunktDto dto = new ForeslaVedtakAksjonspunktDto(null, null, null, false);
        var klageAnkeVedtakTjeneste = new KlageAnkeVedtakTjeneste(mock(KlageRepository.class), mock(AnkeRepository.class));
        ForeslåVedtakAksjonspunktOppdaterer foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
                behandlingRepository, behandlingsresultatRepository, mock(HistorikkTjenesteAdapter.class),
                opprettTotrinnsgrunnlag,
                vedtakTjeneste,
                behandlingDokumentRepository, klageAnkeVedtakTjeneste);

        // Act
        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        Optional<BehandlingDokumentEntitet> behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        assertThat(behandlingDokument.isPresent()).isTrue();
        assertThat(behandlingDokument.get().getOverstyrtBrevOverskrift()).isNull();
        assertThat(behandlingDokument.get().getOverstyrtBrevFritekst()).isNull();
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_beslutters_vurdering_ved_totrinnskontroll() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);

        scenario.medSøknadHendelse().medFødselsDato(now);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);
        Behandling behandling = scenario.lagre(repositoryProvider);

        AksjonspunktGodkjenningDto aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setArsaker(Set.of(VurderÅrsak.FEIL_FAKTA));
        aksGodkjDto.setGodkjent(false);
        String besluttersBegrunnelse = "Må ha bedre dokumentasjon.";
        aksGodkjDto.setBegrunnelse(besluttersBegrunnelse);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);

        FatterVedtakAksjonspunktDto aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
                new AksjonspunktOppdaterParameter(behandling, Optional.empty(), aksjonspunktDto));

        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        assertThat(totrinnsvurderinger).hasSize(1);
        Totrinnsvurdering totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isFalse();
        assertThat(totrinnsvurdering.getBegrunnelse()).isEqualTo(besluttersBegrunnelse);
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).hasSize(1);
        VurderÅrsakTotrinnsvurdering vurderPåNyttÅrsak = totrinnsvurdering.getVurderPåNyttÅrsaker().iterator().next();
        assertThat(vurderPåNyttÅrsak.getÅrsaksType()).isEqualTo(VurderÅrsak.FEIL_FAKTA);
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_godkjent_totrinnskontroll() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
                .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medFødselsDato(now);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);

        AksjonspunktGodkjenningDto aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setGodkjent(true);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
                new AksjonspunktOppdaterParameter(behandling, Optional.empty(), aksjonspunktDto));

        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        assertThat(totrinnsvurderinger).hasSize(1);
        Totrinnsvurdering totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isTrue();
        assertThat(totrinnsvurdering.getBegrunnelse()).isNullOrEmpty();
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).isEmpty();
    }

}
