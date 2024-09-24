package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlem.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es.Medlemsvilkårutleder;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es.VurderMedlemskapvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.skjæringstidspunkt.es.BotidCore2024;

@CdiDbAwareTest
class VurderMedlemskapvilkårStegTest {
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    public InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void forutgående_medlem_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now(), false);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(LocalDate.of(2024, Month.JANUARY, 1), Period.parse("P18W3D")));
        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }

    @Test
    void forutgående_medlem_ikke_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now(), true);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(LocalDate.of(2024, Month.JANUARY, 1), Period.parse("P18W3D")));
        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.IKKE_OPPFYLT);

    }

    @Test
    void skal_endre_til_forutgående_medlem_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now(), false);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(LocalDate.of(2024, Month.JANUARY, 1), Period.parse("P18W3D")));
        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }

    @Test
    void klassisk_medlem_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now(), false);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var fagsak = behandling.getFagsak();
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(LocalDate.now(), Period.parse("P18W3D")));
        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }

    private ScenarioMorSøkerEngangsstønad lagTestScenarioMedlem(LocalDate termindato, boolean medlperiode) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato.plusDays(40))
                .medNavnPå("navn navnesen")
                .medUtstedtDato(termindato.minusWeeks(2)));
        scenario.medOverstyrtHendelse()
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato.plusDays(40))
                .medNavnPå("navn navnesen")
                .medUtstedtDato(termindato.minusWeeks(2)));
        if (medlperiode) {
            scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_7_A)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(termindato.minusMonths(1), termindato.plusMonths(1))
                .build());
        }

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var søker = builderForRegisteropplysninger.medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .personstatus(PersonstatusType.BOSA)
            .statsborgerskap(Landkoder.NOR)
            .adresse(AdresseType.BOSTEDSADRESSE, PersonAdresse.builder()
                .land(Landkoder.NOR)
                .adresseType(AdresseType.BOSTEDSADRESSE)
                .periode(termindato.minusYears(2), termindato.plusYears(2)))
            .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

}
