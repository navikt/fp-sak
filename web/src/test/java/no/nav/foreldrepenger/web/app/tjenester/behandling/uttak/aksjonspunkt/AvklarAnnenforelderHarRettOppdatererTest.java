package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

class AvklarAnnenforelderHarRettOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;

    private BehandlingRepositoryProvider repositoryProvider;

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
        this.ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        this.historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
    }

    @Test
    void skal_opprette_historikkinnslag_ved_endring() {
        //Scenario med avklar fakta annen forelder har rett
        var uførRettighet = new OppgittRettighetEntitet(false, false, true, false, false);
        var scenario = AvklarFaktaTestUtil.opprettScenarioFarSøkerForeldrepenger(uførRettighet);
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderHarRett();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        oppdaterer().oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var historikk = historikkinnslagRepository.hent(behandling.getSaksnummer()).getFirst();

        //assert
        assertThat(historikk.getLinjer()).hasSize(2);
        assertThat(historikk.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OMSORG_OG_RETT);
        assertThat(historikk.getLinjer().getFirst().getTekst()).contains("Annen forelder har rett");
        assertThat(historikk.getLinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_sette_totrinns_ved_avkreft_søkers_opplysning() {
        //Scenario med avklar fakta annen forelder har ikke rett
        var scenario = AvklarFaktaTestUtil.opprettScenarioFarSøkerForeldrepenger(OppgittRettighetEntitet.aleneomsorg());
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderHarRett();
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon()).get();

        var resultat = oppdaterer().oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        //assert
        assertThat(behandling.harAksjonspunktMedType(AKSONSPUNKT_DEF)).isTrue();
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_sette_totrinns_ved_bekreft_uføre() {
        //Scenario med avklar fakta annen forelder har ikke rett
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var rettighet = new OppgittRettighetEntitet(false, false, true, false, false);
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = new AvklarAnnenforelderHarRettDto("Har rett");
        dto.setAnnenforelderHarRett(false);
        dto.setAnnenForelderHarRettEØS(false);
        dto.setAnnenforelderMottarUføretrygd(true);
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon()).get();

        var resultat = oppdaterer().oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        //assert
        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();

        dto.setAnnenforelderMottarUføretrygd(false);
        var resultat1 = oppdaterer().oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        //assert
        assertThat(resultat1.kreverTotrinnsKontroll()).isTrue();
    }

    private AvklarAnnenforelderHarRettOppdaterer oppdaterer() {
        return new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslagRepository());
    }
}
