package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@CdiDbAwareTest
class MedlemsvilkårOversetterTest {

    private MedlemsvilkårOversetter medlemsoversetter;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private YrkesaktivitetBuilder yrkesaktivitetBuilder;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @BeforeEach
    public void oppsett() {
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);
        medlemsoversetter = new MedlemsvilkårOversetter(repositoryProvider, personopplysningTjeneste, iayTjeneste);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap() {
        // Arrange

        var skjæringstidspunkt = LocalDate.now();

        var scenario = oppsett(skjæringstidspunkt);
        var behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, skjæringstidspunkt.minusMonths(5), skjæringstidspunkt.plusMonths(4), true);

        var vurdertMedlemskap = new VurdertMedlemskapBuilder()
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medBosattVurdering(true)
                .medLovligOppholdVurdering(true)
                .medOppholdsrettVurdering(true)
                .build();
        var medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        medlemskapRepository.lagreMedlemskapVurdering(behandling.getId(), vurdertMedlemskap);

        // Act
        var grunnlag = medlemsoversetter.oversettTilRegelModellMedlemskap(BehandlingReferanse.fra(behandling), skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));

        // Assert
        assertThat(grunnlag.brukerAvklartBosatt()).isTrue();
        assertThat(grunnlag.brukerAvklartLovligOppholdINorge()).isTrue();
        assertThat(grunnlag.brukerAvklartOppholdsrett()).isTrue();
        assertThat(grunnlag.brukerAvklartPliktigEllerFrivillig()).isTrue();
        assertThat(grunnlag.brukerNorskNordisk()).isTrue();
        assertThat(grunnlag.brukerBorgerAvEUEOS()).isFalse();
        assertThat(grunnlag.harSøkerArbeidsforholdOgInntekt()).isTrue();
    }

    @Test
    void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap_med_ingen_relevant_arbeid_og_inntekt() {

        // Arrange
        var skjæringstidspunkt = LocalDate.now();
        var scenario = oppsett(skjæringstidspunkt);
        var behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, skjæringstidspunkt.minusMonths(5), skjæringstidspunkt.minusDays(1), true);

        // Act
        var grunnlag = medlemsoversetter.oversettTilRegelModellMedlemskap(BehandlingReferanse.fra(behandling), skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));

        // Assert
        assertThat(grunnlag.harSøkerArbeidsforholdOgInntekt()).isFalse();
    }

    @Test
    void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap_med_relevant_arbeid_og_ingen_pensjonsgivende_inntekt() {

        // Arrange
        var skjæringstidspunkt = LocalDate.now();
        var scenario = oppsett(skjæringstidspunkt);
        var behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, skjæringstidspunkt.minusMonths(5), skjæringstidspunkt.plusDays(10), false);

        // Act
        var grunnlag = medlemsoversetter.oversettTilRegelModellMedlemskap(BehandlingReferanse.fra(behandling), skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));

        // Assert
        assertThat(grunnlag.harSøkerArbeidsforholdOgInntekt()).isFalse();
    }

    private AbstractTestScenario<?> oppsett(LocalDate skjæringstidspunkt) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt);
        scenario.medSøknad()
                .medMottattDato(LocalDate.of(2017, 3, 15));

        var søker = scenario.opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT)
                .personstatus(PersonstatusType.BOSA)
                .statsborgerskap(Landkoder.NOR)
                .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

    private void opprettArbeidOgInntektForBehandling(Behandling behandling, LocalDate fom, LocalDate tom,
            boolean harPensjonsgivendeInntekt) {

        var orgnr = "42";

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørId = behandling.getAktørId();
        lagAktørArbeid(aggregatBuilder, aktørId, orgnr, fom, tom, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Optional.empty());
        for (var dt = fom; dt.isBefore(tom); dt = dt.plusMonths(1)) {
            lagInntekt(aggregatBuilder, aktørId, orgnr, dt, dt.plusMonths(1), harPensjonsgivendeInntekt);
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);
    }

    private AktørArbeid lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
            LocalDate fom, LocalDate tom, ArbeidType arbeidType, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel;
        var arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        if (arbeidsforholdRef.isPresent()) {
            opptjeningsnøkkel = new Opptjeningsnøkkel(arbeidsforholdRef.get(), arbeidsgiver.getIdentifikator(), null);
        } else {
            opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);
        }

        yrkesaktivitetBuilder = aktørArbeidBuilder
                .getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
                .medArbeidType(arbeidType)
                .medArbeidsgiver(arbeidsgiver);

        yrkesaktivitetBuilder.medArbeidsforholdId(arbeidsforholdRef.orElse(null));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return aktørArbeidBuilder.build();
    }

    private void lagInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
            LocalDate fom, LocalDate tom, boolean harPensjonsgivendeInntekt) {
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        Stream<InntektsKilde> inntektsKildeStream;
        if (harPensjonsgivendeInntekt) {
            inntektsKildeStream = Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING, InntektsKilde.INNTEKT_OPPTJENING);
        } else {
            inntektsKildeStream = Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING);
        }

        inntektsKildeStream.forEach(kilde -> {
            var inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
            var inntektspost = InntektspostBuilder.ny()
                    .medBeløp(BigDecimal.valueOf(35000))
                    .medPeriode(fom, tom)
                    .medInntektspostType(InntektspostType.LØNN);
            inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(yrkesaktivitetBuilder.build().getArbeidsgiver());
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        });
    }

}
