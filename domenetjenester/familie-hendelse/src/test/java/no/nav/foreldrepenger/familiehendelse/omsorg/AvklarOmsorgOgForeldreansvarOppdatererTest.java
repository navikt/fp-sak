package no.nav.foreldrepenger.familiehendelse.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
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
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.AvklarOmsorgOgForeldreansvarOppdaterer;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class AvklarOmsorgOgForeldreansvarOppdatererTest extends EntityManagerAwareTest {

    private final ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private OmsorghendelseTjeneste omsorghendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        omsorghendelseTjeneste = new OmsorghendelseTjeneste(familieHendelseTjeneste);
    }

    @Test
    public void skal_oppdatere_vilkår_for_omsorg() {
        // Arrange
        AktørId forelderId = AktørId.dummy();

        scenario.medSøknadHendelse()
            .medAntallBarn(2)
            .leggTilBarn(LocalDate.now().minusYears(1)).leggTilBarn(LocalDate.now().minusYears(1))
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .navn("Forelder"))
            .build();

        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling behandling = scenario.lagre(repositoryProvider);

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setOmsorgsovertakelseDato(LocalDate.now());
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        avklarOmsorgOgForeldreansvar(behandling, dto);
        vilkårBuilder.buildFor(behandling);

        // Assert
        final FamilieHendelseEntitet gjellendeVersjon = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId())
            .getGjeldendeVersjon();
        final Optional<AdopsjonEntitet> adopsjon = gjellendeVersjon.getAdopsjon();
        assertThat(gjellendeVersjon.getAntallBarn()).isEqualTo(2);
        assertThat(adopsjon).hasValueSatisfying(value -> {
            assertThat(value.getOmsorgsovertakelseDato()).as("omsorgsovertakelsesDato").isEqualTo(LocalDate.now());
            assertThat(value.getOmsorgovertakelseVilkår()).as("omsorgsovertakelsesVilkår").isEqualTo(OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET);
        });
    }

    private OppdateringResultat avklarOmsorgOgForeldreansvar(Behandling behandling, AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto) {
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        OppdateringResultat resultat = new AvklarOmsorgOgForeldreansvarOppdaterer(repositoryProvider, skjæringstidspunktTjeneste, omsorghendelseTjeneste, lagMockHistory())
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, null, dto));
        byggVilkårResultat(vilkårBuilder, resultat);
        return resultat;
    }

    private void byggVilkårResultat(VilkårResultat.Builder vilkårBuilder, OppdateringResultat delresultat) {
        delresultat.getVilkårResultatSomSkalLeggesTil().forEach(v -> vilkårBuilder.leggTilVilkårResultat(
            v.getVilkårType(),
            v.getVilkårUtfallType(),
            v.getVilkårUtfallMerknad(),
            new Properties(),
            v.getAvslagsårsak(),
            true,
            false,
            null, null));
        delresultat.getVilkårTyperSomSkalFjernes().forEach(vilkårBuilder::fjernVilkår); // TODO: Vilkår burde ryddes på ein annen måte enn dette
        if (delresultat.getVilkårResultatType() != null) {
            vilkårBuilder.medVilkårResultatType(delresultat.getVilkårResultatType());
        }
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
        LocalDate dødsdato = LocalDate.now();
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

        AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setOmsorgsovertakelseDato(LocalDate.now());
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        var resultat = avklarOmsorgOgForeldreansvar(behandling, dto);

        // Assert
        assertThat(resultat.getEkstraAksjonspunktResultat().stream().filter(ear -> AksjonspunktStatus.AVBRUTT.equals(ear.getElement2())))
            .anySatisfy(ear -> assertThat(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET).isEqualTo(ear.getElement1().getAksjonspunktDefinisjon()));

        assertThat(resultat.getEkstraAksjonspunktResultat().stream().filter(ear -> AksjonspunktStatus.AVBRUTT.equals(ear.getElement2())))
            .allMatch(apr -> !Objects.equals(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, apr.getElement1().getAksjonspunktDefinisjon()));

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

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

}
