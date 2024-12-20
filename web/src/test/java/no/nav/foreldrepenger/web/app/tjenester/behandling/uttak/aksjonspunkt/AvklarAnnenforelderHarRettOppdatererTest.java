package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.AvklarFaktaTestUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

class AvklarAnnenforelderHarRettOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;

    private BehandlingRepositoryProvider repositoryProvider;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private HistorikkInnslagTekstBuilder tekstBuilder;

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private final UføretrygdRepository uføretrygdRepository = mock(UføretrygdRepository.class);

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        historikkApplikasjonTjeneste = mock(HistorikkTjenesteAdapter.class);
        tekstBuilder = new HistorikkInnslagTekstBuilder();
        var inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);
        this.ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
    }

    @Test
    void skal_opprette_historikkinnslag_ved_endring() {
        //Scenario med avklar fakta annen forelder har rett
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
                .medRegisterUføretrygd(false, null, null).build()));
        dto.setAnnenforelderMottarUføretrygd(Boolean.TRUE);

        oppdaterer().oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        //assert
        assertThat(historikkinnslagDeler).hasSize(1);
        var del = historikkinnslagDeler.get(0);
        var rettOpt = del.getEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER);
        assertThat(rettOpt).hasValueSatisfying(rett -> {
            assertThat(rett.getNavn()).isEqualTo(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER.getKode());
            assertThat(rett.getFraVerdi()).isNull();
            assertThat(rett.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_RETT.getKode());
        });
        assertThat(del.getEndretFelt(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD)).isNotEmpty();
        assertThat(del.getSkjermlenke()).hasValueSatisfying(
            skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OMSORG_OG_RETT.getKode()));
        assertThat(del.getBegrunnelse()).hasValueSatisfying(
            begrunnelse -> assertThat(begrunnelse).isEqualTo("Har rett"));
    }

    @Test
    void skal_sette_totrinns_ved_avkreft_søkers_opplysning() {
        //Scenario med avklar fakta annen forelder har ikke rett
        var scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
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
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var rettighet = new OppgittRettighetEntitet(false, false, true, false, false);
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        var behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaTestUtil.opprettBehandlingGrunnlag(getEntityManager(), behandling.getId());
        var dto = new AvklarAnnenforelderHarRettDto("Har rett");
        dto.setAnnenforelderHarRett(false);
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon()).get();

        var resultat = oppdaterer().oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        //assert
        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();

        when(uføretrygdRepository.hentGrunnlag(anyLong())).thenReturn(Optional.of(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medBehandlingId(behandling.getId()).medAktørIdUføretrygdet(AktørId.dummy())
            .medRegisterUføretrygd(false, null, null).build()));
        dto.setAnnenforelderMottarUføretrygd(true);  //skal ikke påvirker her
        var resultat1 = oppdaterer().oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        //assert
        assertThat(resultat1.kreverTotrinnsKontroll()).isTrue();
    }

    private AvklarAnnenforelderHarRettOppdaterer oppdaterer() {
        return new AvklarAnnenforelderHarRettOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory(), mock(FagsakEgenskapRepository.class)));
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        Mockito.when(historikkApplikasjonTjeneste.tekstBuilder()).thenReturn(tekstBuilder);
        return historikkApplikasjonTjeneste;
    }
}
