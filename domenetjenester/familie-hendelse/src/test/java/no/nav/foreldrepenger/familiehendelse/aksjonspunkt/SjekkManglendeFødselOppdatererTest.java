package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsFamilieTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftEktefelleAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.vedtak.felles.jpa.TomtResultatException;

public class SjekkManglendeFødselOppdatererTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL;
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private final LocalDate now = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    private DateTimeFormatter formatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
        new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    private TpsFamilieTjeneste tpsFamilieTjeneste = Mockito.mock(TpsFamilieTjeneste.class);
    private FamiliehendelseEventPubliserer familiehendelseEventPubliserer = Mockito.mock(FamiliehendelseEventPubliserer.class);
    private final FamilieHendelseTjeneste familieHendelseTjeneste = new FamilieHendelseTjeneste(null, familiehendelseEventPubliserer, repositoryProvider);

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_ektefelle() {
        // Arrange
        boolean oppdatertEktefellesBarn = true;

        // Behandling
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));

        scenario.medSøknad()
            .medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.medBekreftetHendelse().medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now())
            .medAdoptererAlene(true));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        Behandling behandling = scenario.lagre(repositoryProvider);
        // Dto
        BekreftEktefelleAksjonspunktDto dto = new BekreftEktefelleAksjonspunktDto("begrunnelse", oppdatertEktefellesBarn);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        new BekreftEktefelleOppdaterer(repositoryProvider, lagMockHistory(), familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslagDeler = this.tekstBuilder.build(historikkinnslag);

        // Assert
        Optional<HistorikkinnslagFelt> feltOpt = historikkInnslagDeler.get(0).getEndretFelt(HistorikkEndretFeltType.EKTEFELLES_BARN);
        assertThat(feltOpt).hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).isEqualTo(HistorikkEndretFeltType.EKTEFELLES_BARN.getKode());
            assertThat(felt.getFraVerdi()).isNull();
            assertThat(felt.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.EKTEFELLES_BARN.getKode());
        });
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_antall_barn() {
        // Arrange
        int antallBarnOpprinnelig = 2;
        final boolean antallBarnTpsGjelderBekreftet = false;

        LocalDate fødselsdatoFraTps = LocalDate.now().minusDays(1);
        LocalDate fødselsDatoFraSøknad = fødselsdatoFraTps.minusDays(10);

        // Behandling
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(fødselsDatoFraSøknad)
            .medAntallBarn(2);
        scenario.medBekreftetHendelse()
            .leggTilBarn(fødselsdatoFraTps)
            .leggTilBarn(fødselsdatoFraTps)
            .leggTilBarn(fødselsdatoFraTps)
            .medAntallBarn(antallBarnOpprinnelig);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        final Behandling behandling = scenario.lagre(repositoryProvider);
        // Dto
        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(fødselsDatoFraSøknad, null),
            new UidentifisertBarnDto(fødselsDatoFraSøknad, null)};

        // Dto
        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("begrunnelse",
            true, antallBarnTpsGjelderBekreftet, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act

        new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste, familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslagDeler = this.tekstBuilder.build(historikkinnslag);

        // Assert
        assertFelt(historikkInnslagDeler.get(0), HistorikkEndretFeltType.BRUK_ANTALL_I_SOKNAD, null, true);
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_fødsel() {
        // Arrange
        LocalDate opprinneligFødseldato = LocalDate.now();
        LocalDate avklartFødseldato = opprinneligFødseldato.plusDays(1);
        int opprinneligAntallBarn = 1;
        int avklartAntallBarn = 2;

        // Behandling
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(opprinneligFødseldato)
            .medAntallBarn(opprinneligAntallBarn);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();
        when(tpsFamilieTjeneste.getFødslerRelatertTilBehandling(any(), any())).thenReturn(new ArrayList<>());

        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(avklartFødseldato, null),
            new UidentifisertBarnDto(avklartFødseldato, null)};

        // Dto

        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("begrunnelse", true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        // Act
        new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste,familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslagDeler = this.tekstBuilder.build(historikkinnslag);

        // Assert
        HistorikkinnslagDel del = historikkInnslagDeler.get(0);

        assertFelt(del, HistorikkEndretFeltType.FODSELSDATO, formatterer.format(opprinneligFødseldato), formatterer.format(avklartFødseldato));
        assertFelt(del, HistorikkEndretFeltType.ANTALL_BARN, opprinneligAntallBarn, avklartAntallBarn);
        assertFelt(del, HistorikkEndretFeltType.FODSELSDATO, formatterer.format(opprinneligFødseldato), formatterer.format(avklartFødseldato));

        Optional<HistorikkinnslagFelt> opplysningOpt = del.getOpplysning(HistorikkOpplysningType.ANTALL_BARN);
        assertThat(opplysningOpt).as("opplysningOpt").hasValueSatisfying(opplysning -> {
            assertThat(opplysning.getNavn()).isEqualTo(HistorikkOpplysningType.ANTALL_BARN.getKode());
            assertThat(opplysning.getTilVerdi()).isEqualTo(Integer.toString(avklartAntallBarn));
        });
    }

    @Test
    public void skal_oppdatere_fødsel() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse()
            .medFødselsDato(now.minusDays(3))
            .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        Behandling behandling = scenario.lagre(repositoryProvider);

        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(now.minusDays(6), null)};

        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("Begrunnelse",
            true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        when(tpsFamilieTjeneste.getFødslerRelatertTilBehandling(any(), any())).thenReturn(new ArrayList<>());

        // Act
        new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste,familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        final FamilieHendelseEntitet hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(1);
        Optional<LocalDate> fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(now.minusDays(6)));
    }

    @Test
    public void skal_oppdatere_fødsel_13m_gir_oppdater_grunnlag() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad().medSøknadsdato(now);
        scenario.medSøknadHendelse().medFødselsDato(now.minusDays(3)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        Behandling behandling = scenario.lagre(repositoryProvider);

        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(now.minusMonths(13), null)};

        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("Begrunnelse",
            true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        when(tpsFamilieTjeneste.getFødslerRelatertTilBehandling(any(), any())).thenReturn(new ArrayList<>());

        // Act
        OppdateringResultat resultat = new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste,familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        assertThat(resultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.OPPDATER);
        final FamilieHendelseEntitet hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(1);
        Optional<LocalDate> fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(now.minusMonths(13)));
    }

    @Test
    public void skal_trigge_totrinnskontroll_dersom_steget_utføres_to_ganger() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse()
            .medFødselsDato(now.minusDays(3))
            .medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        Behandling behandling = scenario.lagre(repositoryProvider);

        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(now.minusDays(6), null)};

        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("Begrunnelse",
            true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode()).get();

        when(tpsFamilieTjeneste.getFødslerRelatertTilBehandling(any(), any())).thenReturn(new ArrayList<>());

        // Act
        OppdateringResultat resultat = new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste, familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();

        // Simuler at aksjonspunkt resettes
        AksjonspunktTestSupport.fjernToTrinnsBehandlingKreves(aksjonspunkt);

        // Act
        OppdateringResultat oppdateringResultat = new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste, familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_oppdatere_antall_barn_basert_på_saksbehandlers_oppgitte_antall() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse()
            .medFødselsDato(now.minusDays(3))
            .medAntallBarn(2);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        Behandling behandling = scenario.lagre(repositoryProvider);

        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(now.minusDays(3), null)};
        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("Begrunnelse",
            true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        when(tpsFamilieTjeneste.getFødslerRelatertTilBehandling(any(), any())).thenReturn(new ArrayList<>());

        // Act
        new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste, familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        final FamilieHendelseEntitet hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(1);
        Optional<LocalDate> fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(now.minusDays(3)));
    }

    @Test
    public void skal_oppdatere_antall_barn_basert_på_tps_dersom_flagg_satt() {
        // Arrange
        LocalDate fødselsdatoFraSøknad = now;
        LocalDate fødselsdatoFraTps = now.minusDays(1);

        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse()
            .medAntallBarn(2)
            .medFødselsDato(fødselsdatoFraSøknad);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        scenario.medBekreftetHendelse()
            .leggTilBarn(fødselsdatoFraTps)
            .leggTilBarn(fødselsdatoFraTps)
            .leggTilBarn(fødselsdatoFraTps);
        Behandling behandling = scenario.lagre(repositoryProvider);
        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("Begrunnelse",
            true, true, new ArrayList<UidentifisertBarnDto>());
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        // Act
        new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste, familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        final FamilieHendelseEntitet hendelse = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        assertThat(hendelse).isNotNull();
        assertThat(hendelse.getAntallBarn()).isEqualTo(3);
        Optional<LocalDate> fodselsdatoOpt = hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        assertThat(fodselsdatoOpt).as("fodselsdatoOpt")
            .hasValueSatisfying(fodselsdato -> assertThat(fodselsdato).as("fodselsdato").isEqualTo(fødselsdatoFraTps));
    }

    @Test
    public void skal_hive_exception_når_dokumentasjon_foreligger_og_fødselsdato_er_tom() {
        // Arrange
        LocalDate fødselsdatoFraSøknad = now;

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medFødselsDato(fødselsdatoFraSøknad);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        Behandling behandling = scenario.lagre(repositoryProvider);

        UidentifisertBarnDto[] uidentifiserteBarn = {new UidentifisertBarnDto(null, null)};

        SjekkManglendeFodselDto dto = new SjekkManglendeFodselDto("Begrunnelse",
            true, false, List.of(uidentifiserteBarn));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        try {
            new SjekkManglendeFødselOppdaterer(lagMockHistory(), skjæringstidspunktTjeneste, familieHendelseTjeneste)
                .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
            fail("expected exception to be thrown");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(TomtResultatException.class);
        }
    }

    private void assertFelt(HistorikkinnslagDel historikkinnslagDel, HistorikkEndretFeltType historikkEndretFeltType, Object fraVerdi, Object tilVerdi) {
        Optional<HistorikkinnslagFelt> feltOpt = historikkinnslagDel.getEndretFelt(historikkEndretFeltType);
        assertThat(feltOpt).hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).as(historikkEndretFeltType + ".navn").isEqualTo(historikkEndretFeltType.getKode());
            assertThat(felt.getFraVerdi()).as(historikkEndretFeltType + ".fraVerdi").isEqualTo(fraVerdi != null ? fraVerdi.toString() : null);
            assertThat(felt.getTilVerdi()).as(historikkEndretFeltType + ".tilVerdi").isEqualTo(tilVerdi.toString());
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
