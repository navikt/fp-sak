package no.nav.foreldrepenger.familiehendelse.omsorg;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.AvklarOmsorgOgForeldreansvarOppdaterer;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataBarnDto;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataForeldreDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class AvklarOmsorgOgForeldreansvarOppdatererTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();
    @Rule
    public final UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    private FamilieHendelseTjeneste familieHendelseTjeneste = new FamilieHendelseTjeneste(null, null, repositoryProvider);
    private final LocalDate nå = LocalDate.now();
    private VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();

    private OmsorghendelseTjeneste omsorghendelseTjeneste = new OmsorghendelseTjeneste(familieHendelseTjeneste);

    @Test
    public void skal_oppdatere_vilkår_for_omsorg() {
        // Arrange
        AktørId forelderId = AktørId.dummy();

        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .navn("Forelder"))
            .build();

        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(2);
        dto.setOmsorgsovertakelseDato(nå);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        avklarOmsorgOgForeldreansvar(behandling, dto);
        vilkårBuilder.buildFor(behandling);

        // Assert
        final FamilieHendelseEntitet gjellendeVersjon = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId())
            .getGjeldendeVersjon();
        final Optional<AdopsjonEntitet> adopsjon = gjellendeVersjon.getAdopsjon();
        assertThat(gjellendeVersjon.getAntallBarn()).isEqualTo(2);
        assertThat(adopsjon).hasValueSatisfying(value -> {
            assertThat(value.getOmsorgsovertakelseDato()).as("omsorgsovertakelsesDato").isEqualTo(nå);
            assertThat(value.getOmsorgovertakelseVilkår()).as("omsorgsovertakelsesVilkår").isEqualTo(OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET);
        });
    }

    @Test
    public void skal_legge_til_nytt_barn_dersom_id_er_tom_i_dto() {
        // Arrange
        // Behandlingsgrunnlag UTEN eksisterende bekreftet barn
        AktørId forelderId = AktørId.dummy();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.medSøknad();

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .navn("Forelder"))
            .build();

        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);

        AvklartDataBarnDto barn1 = new AvklartDataBarnDto();
        barn1.setFodselsdato(nå);

        AvklartDataForeldreDto forelder1 = new AvklartDataForeldreDto();
        forelder1.setAktorId(forelderId);

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(1);
        dto.setOmsorgsovertakelseDato(nå);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);
        dto.setForeldre(singletonList(forelder1));
        dto.setBarn(singletonList(barn1));

        // Act
        avklarOmsorgOgForeldreansvar(behandling, dto);

        // Assert
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        final FamilieHendelseEntitet gjeldendeVersjon = familieHendelseGrunnlag.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
    }

    @Test
    public void skal_oppdatere_eksisterende_barn_dersom_id_er_oppgitt_i_dto() {
        // Arrange
        AktørId forelderId = AktørId.dummy();
        LocalDate fødselsdato = nå;
        LocalDate oppdatertFødselsdato = fødselsdato.plusDays(1);

        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .navn("Forelder"))
            .build();

        scenario.medSøknad();
        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);
        final no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet personopplysning = getSøkerPersonopplysning(behandling.getId());
        AvklartDataBarnDto barn1 = new AvklartDataBarnDto();
        barn1.setFodselsdato(oppdatertFødselsdato);

        AvklartDataForeldreDto forelder1 = new AvklartDataForeldreDto();
        forelder1.setAktorId(personopplysning.getPersonopplysninger().stream()
            .filter(e -> e.getAktørId().equals(forelderId))
            .findFirst().get().getAktørId());

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(1);
        dto.setOmsorgsovertakelseDato(nå);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);
        dto.setForeldre(Collections.singletonList(forelder1));
        dto.setBarn(Collections.singletonList(barn1));

        // Act
        avklarOmsorgOgForeldreansvar(behandling, dto);

        // Assert
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        final FamilieHendelseEntitet gjeldendeVersjon = familieHendelseGrunnlag.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
        assertThat(gjeldendeVersjon.getAntallBarn()).isEqualTo(1);
        assertThat(gjeldendeVersjon.getBarna().stream().map(UidentifisertBarn::getFødselsdato)
            .collect(Collectors.toList())).contains(oppdatertFødselsdato);
    }

    private OppdateringResultat avklarOmsorgOgForeldreansvar(Behandling behandling, AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto) {
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        return new AvklarOmsorgOgForeldreansvarOppdaterer(repositoryProvider, skjæringstidspunktTjeneste, omsorghendelseTjeneste, lagMockHistory())
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, null, vilkårBuilder, dto));
    }

    @Test
    public void skal_fjerne_eksisterende_barn_dersom_antall_barn_reduseres() {
        // Arrange
        AktørId forelderId = AktørId.dummy();
        LocalDate fødselsdato1 = nå;

        // Behandlingsgrunnlag MED eksisterende 2 bekreftede barn
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .navn("Forelder"))
            .build();

        scenario.medSøknad();
        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);
        Behandling behandling = scenario.lagre(repositoryProvider);
        final no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet personopplysning = getSøkerPersonopplysning(behandling.getId());
        // Kun 1 barn fra DTO
        AvklartDataBarnDto barnDto1 = new AvklartDataBarnDto();
        barnDto1.setFodselsdato(nå);

        AvklartDataForeldreDto forelder1 = new AvklartDataForeldreDto();
        forelder1.setAktorId(personopplysning.getPersonopplysninger().stream()
            .filter(e -> e.getAktørId().equals(forelderId))
            .findFirst().get().getAktørId());

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(1);
        dto.setOmsorgsovertakelseDato(nå);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);
        dto.setForeldre(singletonList(forelder1));
        dto.setBarn(singletonList(barnDto1));

        // Act
        avklarOmsorgOgForeldreansvar(behandling, dto);

        // Assert
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        final FamilieHendelseEntitet gjeldendeVersjon = familieHendelseGrunnlag.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
        assertThat(gjeldendeVersjon.getAntallBarn()).isEqualTo(1);
        assertThat(gjeldendeVersjon.getBarna().stream().map(UidentifisertBarn::getFødselsdato)
            .collect(Collectors.toList())).containsOnly(fødselsdato1);
    }

    private no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet getSøkerPersonopplysning(Long behandlingId) {
        PersonopplysningGrunnlagEntitet grunnlag = getPersonopplysninger(behandlingId);
        return grunnlag.getGjeldendeVersjon();
    }

    private PersonopplysningGrunnlagEntitet getPersonopplysninger(Long behandlingId) {
        return repositoryProvider.getPersonopplysningRepository()
            .hentPersonopplysninger(behandlingId);
    }

    @Test
    public void skal_sette_andre_aksjonspunkter_knyttet_til_omsorgsvilkåret_som_utført() {
        // Arrange
        AktørId forelderId = AktørId.dummy();
        LocalDate dødsdato = nå;
        LocalDate oppdatertDødsdato = dødsdato.plusDays(1);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .dødsdato(dødsdato)
                    .navn("Navn"))
            .build();

        scenario.medRegisterOpplysninger(forelder);
        Behandling behandling = scenario.lagre(repositoryProvider);

        AvklartDataForeldreDto forelderDto = new AvklartDataForeldreDto();
        forelderDto.setAktorId(forelderId);
        forelderDto.setDødsdato(oppdatertDødsdato);

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(1);
        dto.setOmsorgsovertakelseDato(nå);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);
        dto.setForeldre(singletonList(forelderDto));

        var resultat = avklarOmsorgOgForeldreansvar(behandling, dto);

        // Assert
        assertThat(resultat.getEkstraAksjonspunktResultat().stream().filter(ear -> AksjonspunktStatus.AVBRUTT.equals(ear.getElement2())))
            .anySatisfy(ear -> assertThat(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET).isEqualTo(ear.getElement1()));

        assertThat(resultat.getEkstraAksjonspunktResultat().stream().filter(ear -> AksjonspunktStatus.AVBRUTT.equals(ear.getElement2())))
            .allMatch(apr -> !Objects.equals(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, apr.getElement1()));

    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_omsorgsovertakelsesdato() {
        // Arrange
        LocalDate omsorgsovertakelsesdatoOppgitt = LocalDate.of(2019, 3, 4);
        LocalDate omsorgsovertakelsesdatoBekreftet = omsorgsovertakelsesdatoOppgitt.plusDays(1);

        // Behandling
        scenario.medSøknad()
            .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsesdatoOppgitt));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagre(repositoryProvider);

        Behandling behandling = scenario.getBehandling();

        // Dto
        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(1);
        dto.setOmsorgsovertakelseDato(omsorgsovertakelsesdatoBekreftet);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        avklarOmsorgOgForeldreansvar(behandling, dto);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslagDeler = this.tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslagDeler).hasSize(1);
        List<HistorikkinnslagFelt> feltList = historikkInnslagDeler.get(0).getEndredeFelt();
        HistorikkinnslagFelt felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.OMSORGSOVERTAKELSESDATO.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isEqualTo("04.03.2019");
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("05.03.2019");
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_ved_omsorgsovertakelse_antall_barn() {
        // Arrange
        int antallBarnFraSøknad = 2;
        int antallBarnBekreftet = 1;

        // Behandling
        scenario.medSøknad()
            .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()))
            .medAntallBarn(antallBarnFraSøknad);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Dto
        AvklartDataBarnDto barn = new AvklartDataBarnDto();
        barn.setFodselsdato(LocalDate.now());

        AvklartDataForeldreDto forelder1 = new AvklartDataForeldreDto();

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setAntallBarn(antallBarnBekreftet);
        dto.setOmsorgsovertakelseDato(LocalDate.now());
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);
        dto.setForeldre(Collections.singletonList(forelder1));
        dto.setBarn(Collections.singletonList(barn));

        avklarOmsorgOgForeldreansvar(behandling, dto);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslagDeler = this.tekstBuilder.build(historikkinnslag);

        // Assert
        assertHistorikkinnslag(historikkInnslagDeler, HistorikkEndretFeltType.ANTALL_BARN, Integer.toString(antallBarnFraSøknad),
            Integer.toString(antallBarnBekreftet));
    }

    private void assertHistorikkinnslag(List<HistorikkinnslagDel> historikkInnslagDeler, HistorikkEndretFeltType endretFeltType, String fraVerdi,
                                        String tilVerdi) {
        assertThat(historikkInnslagDeler).hasSize(1);
        HistorikkinnslagDel del = historikkInnslagDeler.get(0);
        Optional<HistorikkinnslagFelt> feltOpt = del.getEndretFelt(endretFeltType);
        assertThat(feltOpt).as("endretFelt").hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).as("navn").isEqualTo(endretFeltType.getKode());
            assertThat(felt.getFraVerdi()).as("fraVerdi").isEqualTo(fraVerdi);
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(tilVerdi);
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

}
