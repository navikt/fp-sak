package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
class AvklarOmSøkerOppholderSegINorgeTest {

    @Inject
    private BehandlingRepositoryProvider provider;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private AvklarOmSøkerOppholderSegINorge tjeneste;

    @BeforeEach
    public void setUp() {
        this.tjeneste = new AvklarOmSøkerOppholderSegINorge(provider, personopplysningTjeneste, iayTjeneste);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_har_fodt() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
                .medFødselsDato(fødselsdato)
                .medAntallBarn(1);
        scenario.medBekreftetHendelse()
                .medFødselsDato(fødselsdato)
                .medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);
        var behandling = lagre(scenario);

        // Act
        var medlemResultat = kallTjeneste(behandling, fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(provider);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_har_fodt_søkt_termin() {
        // Arrange
        var fødselsdato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(fødselsdato)
                .medUtstedtDato(fødselsdato.minusMonths(2))
                .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(fødselsdato)
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(fødselsdato.minusMonths(2)));

        scenario.medBekreftetHendelse()
                .medFødselsDato(fødselsdato.minusDays(12))
                .medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.BEL);
        scenario.medSøknadDato(fødselsdato.minusMonths(2).plusWeeks(1));
        scenario.medSøknad()
                .medMottattDato(fødselsdato.minusMonths(2).plusWeeks(1));
        var behandling = lagre(scenario);

        // Act
        var medlemResultat = kallTjeneste(behandling, fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_har_dato_for_omsorgsovertakelse() {
        // Arrange
        var omsorgsovertakelseDato = LocalDate.now();
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        var farSøkerType = FarSøkerType.OVERTATT_OMSORG;
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelseDato));
        scenario.medSøknad().medFarSøkerType(farSøkerType);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);

        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = kallTjeneste(behandling, omsorgsovertakelseDato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_er_nordisk() {
        // Arrange
        var termindato = LocalDate.now();
        var aktørId1 = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(termindato));
        scenario.medSøknad()
                .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);
        var behandling = lagre(scenario);

        // Act
        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    private Optional<MedlemResultat> kallTjeneste(Behandling behandling, LocalDate dato) {
        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato)
            .medUttaksintervall(new LocalDateInterval(dato.minusWeeks(4), dato.plusWeeks(4)))
            .build();
        return tjeneste.utledVedSTP(ref, stp);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_har_annet_statsborgerskap() {
        // Arrange
        var termindato = LocalDate.now();
        var aktørId1 = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(termindato));
        scenario.medSøknad()
                .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.CAN);
        var behandling = lagre(scenario);

        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_er_gift_med_nordisk() {
        // Arrange
        var termindato = LocalDate.now();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var annenPartAktørId = AktørId.dummy();

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        var gift = builderForRegisteropplysninger
                .medPersonas()
                .mann(annenPartAktørId, SivilstandType.GIFT)
                .statsborgerskap(Landkoder.FIN)
                .relasjonTil(søkerAktørId, RelasjonsRolleType.EKTE)
                .build();
        scenario.medRegisterOpplysninger(gift);

        var søker = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.GIFT)
                .statsborgerskap(Landkoder.ESP)
                .relasjonTil(annenPartAktørId, RelasjonsRolleType.EKTE)
                .build();

        scenario.medRegisterOpplysninger(søker);

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(termindato)
                .medNavnPå("LEGEN MIN"));
        scenario.medSøknad()
                .medMottattDato(LocalDate.now());
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGEN MIN"));
        var behandling = lagre(scenario);

        // Act
        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_er_gift_med_ANNET_statsborgerskap() {
        // Arrange
        var termindato = LocalDate.now();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var annenPartAktørId = AktørId.dummy();

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        var søker = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.GIFT)
                .statsborgerskap(Landkoder.ESP)
                .relasjonTil(annenPartAktørId, RelasjonsRolleType.EKTE)
                .build();

        var gift = builderForRegisteropplysninger
                .medPersonas()
                .mann(annenPartAktørId, SivilstandType.GIFT)
                .statsborgerskap(Landkoder.CAN)
                .relasjonTil(søkerAktørId, RelasjonsRolleType.EKTE)
                .build();

        scenario.medRegisterOpplysninger(gift);
        scenario.medRegisterOpplysninger(søker);

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(termindato)
                .medNavnPå("LEGEN MIN"));
        scenario.medSøknad()
                .medMottattDato(LocalDate.now());
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGEN MIN"));
        var behandling = lagre(scenario);

        // Act
        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_har_hatt_inntekt_i_Norge_de_siste_tre_mnd() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var fom = LocalDate.now().minusWeeks(3L);
        var tom = LocalDate.now().minusWeeks(1L);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        var termindato = LocalDate.now().plusDays(40);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now().minusDays(7))
                .medNavnPå("LEGEN MIN"));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now().minusDays(7))
                .medNavnPå("LEGEN MIN"));
        scenario.medSøknad().medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        var behandling = lagre(scenario);

        leggTilInntekt(behandling, behandling.getAktørId(), fom, tom);

        // Act
        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    private void leggTilInntekt(Behandling behandling, AktørId aktørId, LocalDate fom, LocalDate tom) {
        // Arrange - inntekt
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørInntekt = builder.getAktørInntektBuilder(aktørId);
        aktørInntekt.leggTilInntekt(InntektBuilder.oppdatere(empty())
                .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
                .leggTilInntektspost(InntektspostBuilder.ny()
                        .medBeløp(BigDecimal.TEN)
                        .medInntektspostType(InntektspostType.LØNN)
                        .medPeriode(fom, tom)));
        builder.leggTilAktørInntekt(aktørInntekt);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_medsoker_har_hatt_inntekt_i_Norge_de_siste_tre_mnd() {
        // Arrange
        var aktørId1 = AktørId.dummy();
        var aktørId2 = AktørId.dummy();
        var fom = LocalDate.now().minusWeeks(3L);
        var tom = LocalDate.now().minusWeeks(1L);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        var termindato = LocalDate.now().plusDays(40);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now().minusDays(7))
                .medNavnPå("LEGEN MIN"));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now().minusDays(7))
                .medNavnPå("LEGEN MIN"));
        scenario.medSøknad().medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);

        var behandling = lagre(scenario);

        leggTilInntekt(behandling, aktørId2, fom, tom);

        // Act
        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_termindato_ikke_har_passert_14_dager() {
        // Arrange
        var termindato = LocalDate.now().minusDays(14L);
        var aktørId1 = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(termindato));
        scenario.medSøknad()
                .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        var behandling = lagre(scenario);

        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    void skal_ikke_opprette_vent_om_termindato_har_passert_28_dager() {
        // Arrange
        var termindato = LocalDate.now().minusMonths(2);
        var aktørId1 = AktørId.dummy();
        var fom = LocalDate.now().minusWeeks(60L);
        var tom = LocalDate.now().minusWeeks(58L);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(termindato.minusMonths(2))
                .medNavnPå("LEGEN MIN"));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(termindato.minusMonths(2))
                .medNavnPå("LEGEN MIN"));
        scenario.medSøknad().medMottattDato(termindato.minusMonths(2).plusDays(3));
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);

        var behandling = lagre(scenario);

        leggTilInntekt(behandling, behandling.getAktørId(), fom, tom);

        // Act
        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    void skal_oprette_aksjonspunkt_ved_uavklart_oppholdsrett() {
        // Arrange
        var termindato = LocalDate.now().minusDays(15L);
        var aktørId1 = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(termindato));
        scenario.medSøknad()
                .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        var behandling = lagre(scenario);

        var medlemResultat = kallTjeneste(behandling, termindato);

        // Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    private AktørId leggTilSøker(AbstractTestScenario<?> scenario, AdresseType adresseType, Landkoder adresseLand) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var persona = builderForRegisteropplysninger
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.UOPPGITT)
                .personstatus(PersonstatusType.UDEFINERT)
                .statsborgerskap(adresseLand);

        var adresseBuilder = PersonAdresse.builder().adresselinje1("Portveien 2").land(adresseLand);
        persona.adresse(adresseType, adresseBuilder);
        var søker = persona.build();
        scenario.medRegisterOpplysninger(søker);
        return søkerAktørId;
    }

}
