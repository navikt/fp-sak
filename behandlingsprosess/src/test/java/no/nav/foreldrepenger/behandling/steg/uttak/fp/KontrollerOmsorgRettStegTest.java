package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.fakta.OmsorgRettUttakTjeneste;

@CdiDbAwareTest
class KontrollerOmsorgRettStegTest {

    private static final AktørId FAR_AKTØR_ID = AktørId.dummy();

    private Behandling behandling;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private RyddOmsorgRettTjeneste ryddOmsorgRettTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
    private KontrollerOmsorgRettSteg steg;

    @Inject
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    private OmsorgRettUttakTjeneste tjeneste;

    private static ScenarioFarSøkerForeldrepenger opprettBehandlingForFarSomSøker() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());

        var personInformasjon = scenario
                .opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .mann(FAR_AKTØR_ID, SivilstandType.SAMBOER).statsborgerskap(Landkoder.NOR)
                .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        var rettighet = OppgittRettighetEntitet.beggeRett();
        scenario.medOppgittRettighet(rettighet);
        var now = LocalDate.now();
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(now.plusWeeks(8), now.plusWeeks(12))
                .build()), true));
        return scenario;
    }

    @BeforeEach
    public void oppsett() {
        var scenario = opprettBehandlingForFarSomSøker();
        behandling = scenario.lagre(repositoryProvider);
        steg = new KontrollerOmsorgRettSteg(uttakInputTjeneste, tjeneste, ryddOmsorgRettTjeneste);
    }

    @Test
    void skal_utledede_aksjonspunkt_basert_på_fakta_om_foreldrepenger_til_mor() {
        // oppsett
        byggBehandingMedMorSøkerTypeOgHarAleneOmsorgOgAnnenforelderHarIkkeRett();

        var input = uttakInputTjeneste.lagInput(behandling);
        var resultat = tjeneste.utledAksjonspunkter(input);

        // Assert
        assertThat(resultat).containsExactlyInAnyOrder(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);

    }

    private Behandling byggBehandingMedMorSøkerTypeOgHarAleneOmsorgOgAnnenforelderHarIkkeRett() {
        var AKTØR_ID_MOR = AktørId.dummy();
        var AKTØR_ID_FAR = AktørId.dummy();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        var rettighet = OppgittRettighetEntitet.aleneomsorg();

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        var bostedsadresse = Adresseinfo.builder(AdresseType.BOSTEDSADRESSE)
            .medAdresselinje1("Portveien 2").medPostnummer("7000").medLand(Landkoder.NOR)
            .build();

        var annenPrt = builderForRegisteropplysninger
                .medPersonas()
                .mann(AKTØR_ID_FAR, SivilstandType.GIFT)
                .bostedsadresse(new AdressePeriode(null, bostedsadresse))
                .relasjonTil(AKTØR_ID_MOR, RelasjonsRolleType.EKTE, true)
                .build();
        scenario.medRegisterOpplysninger(annenPrt);

        var søker = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(AKTØR_ID_MOR, SivilstandType.GIFT)
                .bostedsadresse(new AdressePeriode(null, bostedsadresse))
                .statsborgerskap(Landkoder.NOR)
                .relasjonTil(AKTØR_ID_FAR, RelasjonsRolleType.EKTE, true)
                .build();

        scenario.medRegisterOpplysninger(søker);

        scenario.medSøknadAnnenPart()
                .medAktørId(AKTØR_ID_FAR);

        scenario.medSøknad();
        scenario.medOppgittRettighet(rettighet);
        var hendelseBuilder = scenario.medSøknadHendelse();
        var termindato = LocalDate.now().plusDays(35);
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                .medNavnPå("asdf")
                .medUtstedtDato(LocalDate.now())
                .medTermindato(termindato));

        scenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(
                OppgittPeriodeBuilder.ny()
                        .medPeriode(termindato, termindato.plusWeeks(6))
                        .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                        .build()),
                true));

        behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }
}
