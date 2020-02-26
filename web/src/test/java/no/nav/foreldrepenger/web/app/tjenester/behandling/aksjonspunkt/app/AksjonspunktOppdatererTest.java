package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.FatterVedtakAksjonspunkt;
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
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AksjonspunktOppdatererTest {

    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String ANSVARLIG_SAKSBEHANLDER = "saksbehandler";
    private static final String OVERSKRIFT = "overskrift";
    private static final String FRITEKST = "fritekst";

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private LocalDate now = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private LagretVedtakRepository lagretVedtakRepository = new LagretVedtakRepository(entityManager);
    private InnsynRepository innsynRepository = new InnsynRepository(entityManager);
    private KlageRepository klageRepository = new KlageRepository(entityManager);
    private AnkeRepository ankeRepository = new AnkeRepository(entityManager);

    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;
    private HistorikkRepository historikkRepository = new HistorikkRepository(entityManager);
    private TotrinnRepository totrinnRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Inject
    private VedtakTjeneste vedtakTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpprettToTrinnsgrunnlag opprettTotrinnsgrunnlag;

    @Inject
    private BehandlingDokumentRepository behandlingDokumentRepository;

    @Before
    public void setup() {
        totrinnRepository = new TotrinnRepository(entityManager);
        var beregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(entityManager);
        var ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var uttakRepository = new UttakRepository(entityManager);
        TotrinnTjeneste totrinnTjeneste = new TotrinnTjeneste(totrinnRepository);
        opprettTotrinnsgrunnlag = new OpprettToTrinnsgrunnlag(
            beregningsgrunnlagTjeneste,
            ytelsesFordelingRepository,
            uttakRepository,
            totrinnTjeneste,
            iayTjeneste
            );

        VedtakTjeneste vedtakTjeneste = new VedtakTjeneste(lagretVedtakRepository, historikkRepository, klageRepository, totrinnTjeneste,
            innsynRepository, ankeRepository);
        fatterVedtakAksjonspunkt = new FatterVedtakAksjonspunkt(behandlingskontrollTjeneste, klageRepository, ankeRepository, vedtakTjeneste, totrinnTjeneste);
    }

    @Test
    public void bekreft_foreslå_vedtak_aksjonspkt_setter_ansvarlig_saksbehandler() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medFødselsDato(now);
        Behandling behandling = scenario.lagre(repositoryProvider);

        ForeslaVedtakAksjonspunktDto dto = new ForeslaVedtakAksjonspunktDto(BEGRUNNELSE, null, null, false);
        ForeslåVedtakAksjonspunktOppdaterer foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste,
            behandlingDokumentRepository) {
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
        ForeslåVedtakAksjonspunktOppdaterer foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste,
            behandlingDokumentRepository);

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
