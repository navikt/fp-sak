package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;

public class MedlemDtoTjenesteTest {

    private final InntektArbeidYtelseTjeneste iayTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    String orgnr = KUNSTIG_ORG;

    @Test
    public void skal_lage_medlem_dto() {
        String navn = "Lisa gikk til skolen";
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var stp = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(stp);
        AktørId søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(søkerAktørId)
                                .navn(navn))
                .build();

        scenario.medRegisterOpplysninger(søker);
        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        scenario.medMedlemskap()
                .medErEosBorger(true)
                .medBosattVurdering(true)
                .medOppholdsrettVurdering(true)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medLovligOppholdVurdering(true);

        Behandling behandling = scenario.lagMocked();
        final BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        InntektspostBuilder builder = InntektspostBuilder.ny();

        InntektspostBuilder inntektspost = builder
                .medBeløp(BigDecimal.TEN)
                .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now())
                .medInntektspostType(InntektspostType.UDEFINERT);

        lagreOpptjening(søkerAktørId, inntektspost, arbeidsgiver);

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any()))
                .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());

        ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger(null, navn, LocalDate.of(2018, 1, 1)));

        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, arbeidsgiverTjeneste, skjæringstidspunktTjeneste, iayTjeneste,
                medlemTjenesteMock, personopplysningTjenesteMock, mock(PersonopplysningDtoTjeneste.class));

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> {
            assertThat(medlemDto.getFom()).isEqualTo(stp);
            assertThat(medlemDto.getMedlemskapPerioder()).hasSize(1);
            assertThat(medlemDto.getInntekt()).hasSize(1);
            InntektDto inntektDto = medlemDto.getInntekt().get(0);
            assertThat(inntektDto.getUtbetaler()).isEqualTo("Lisa ...(01.01.2018)");
            assertThat(inntektDto.getNavn()).isEqualTo(navn);
        });
    }

    @Test
    public void skal_sette_fom_til_endring_i_personopplysningers_gjeldende_fra() {
        String navn = "Lisa gikk til skolen";
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        AktørId søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.person(AktørId.dummy());

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(søkerAktørId)
                                .navn(navn))
                .build();

        scenario.medRegisterOpplysninger(søker);
        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        scenario.medMedlemskap()
                .medErEosBorger(true)
                .medBosattVurdering(true)
                .medOppholdsrettVurdering(true)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medLovligOppholdVurdering(true);

        Behandling behandling = scenario.lagMocked();
        final BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        InntektspostBuilder builder = InntektspostBuilder.ny();

        InntektspostBuilder inntektspost = builder
                .medBeløp(BigDecimal.TEN)
                .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now())
                .medInntektspostType(InntektspostType.UDEFINERT);

        lagreOpptjening(søkerAktørId, inntektspost, arbeidsgiver);

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        LocalDate endringFraDato = LocalDate.now().minusDays(5);
        var endringsresultatPersonopplysningerForMedlemskap = EndringsresultatPersonopplysningerForMedlemskap.builder()
                .leggTilEndring(EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt.Adresse,
                        DatoIntervallEntitet.fraOgMed(endringFraDato), "", "2")
                .build();
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any())).thenReturn(endringsresultatPersonopplysningerForMedlemskap);

        ArbeidsgiverTjeneste arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger(null, navn, LocalDate.of(2018, 1, 1)));

        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, arbeidsgiverTjeneste, skjæringstidspunktTjeneste, iayTjeneste,
                medlemTjenesteMock, personopplysningTjenesteMock, mock(PersonopplysningDtoTjeneste.class));

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt.get().getFom()).isEqualTo(endringFraDato);
    }

    @Test
    public void skal_lage_inntekt_for_ektefelle() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        String navn = "Lisa gikk til skolen";
        String annenPart = "Tripp, tripp, tripp, det sa";
        AktørId aktørIdSøker = AktørId.dummy();
        AktørId aktørIdAnnenPart = AktørId.dummy();

        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        PersonInformasjon personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(aktørIdSøker)
                                .navn(navn))
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(aktørIdAnnenPart)
                                .navn(annenPart))
                .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        scenario.medMedlemskap()
                .medErEosBorger(true)
                .medBosattVurdering(true)
                .medOppholdsrettVurdering(true)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medLovligOppholdVurdering(true);

        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        InntektspostBuilder inntektspost = InntektspostBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medPeriode(LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1))
                .medInntektspostType(InntektspostType.UDEFINERT);

        lagreOpptjening(aktørIdAnnenPart, inntektspost, Arbeidsgiver.virksomhet(orgnr));
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersoninfoBasis person = new PersoninfoBasis.Builder()
                .medNavn(annenPart)
                .medAktørId(AktørId.dummy())
                .medPersonIdent(new PersonIdent("12312411252"))
                .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
                .medFødselsdato(LocalDate.now())
                .build();

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        PersonIdentTjeneste tpsTjeneste = mock(PersonIdentTjeneste.class);
        when(tpsTjeneste.hentBrukerForAktør(AktørId.dummy())).thenReturn(Optional.ofNullable(person));
        ArbeidsgiverTjeneste tjeneste = new ArbeidsgiverTjeneste(tpsTjeneste, mock(VirksomhetTjeneste.class));

        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any()))
                .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());
        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, tjeneste, skjæringstidspunktTjeneste, iayTjeneste,
                medlemTjenesteMock,
                personopplysningTjenesteMock, mock(PersonopplysningDtoTjeneste.class));

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> {
            assertThat(medlemDto.getMedlemskapPerioder()).hasSize(1);
            assertThat(medlemDto.getInntekt()).hasSize(0);
        });
    }

    private void lagreOpptjening(AktørId aktørId, InntektspostBuilder inntektspost, Arbeidsgiver arbeidsgiver) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
                VersjonType.REGISTER);

        final YrkesaktivitetBuilder oppdatere = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        Yrkesaktivitet yrkesaktivitet = oppdatere
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .build();

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = builder
                .leggTilYrkesaktivitet(oppdatere);

        InntektBuilder inntekt = InntektBuilder.oppdatere(Optional.empty())
                .leggTilInntektspost(inntektspost)
                .medArbeidsgiver(yrkesaktivitet.getArbeidsgiver())
                .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId)
                .leggTilInntekt(inntekt);

        inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntekt);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);

        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
                .medData(inntektArbeidYtelseAggregatBuilder).build();

        when(iayTjeneste.finnGrunnlag(any())).thenReturn(Optional.of(inntektArbeidYtelseGrunnlag));
    }
}
