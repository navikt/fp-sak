package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(JpaExtension.class)
class BeregnTilrettleggingsperioderTjenesteTest {

    private static final String ARBEIDSGIVER_ORGNR = KUNSTIG_ORG;
    public static final InternArbeidsforholdRef ARB_1 = InternArbeidsforholdRef.namedRef("arb1");
    public static final InternArbeidsforholdRef ARB_2 = InternArbeidsforholdRef.namedRef("arb2");

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private FamilieHendelseRepository familieHendelseRepository;
    private BeregnTilrettleggingsperioderTjeneste tjeneste;
    private IAYRepositoryProvider iayRepositoryProvider;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        iayRepositoryProvider = new IAYRepositoryProvider(entityManager);
        familieHendelseRepository = iayRepositoryProvider.getFamilieHendelseRepository();
        svangerskapspengerRepository = new SvangerskapspengerRepository(entityManager);
        tjeneste = new BeregnTilrettleggingsperioderTjeneste(svangerskapspengerRepository, iayTjeneste, familieHendelseRepository);
    }

    @Test
    void skal_beregne_perioder() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(iayRepositoryProvider);
        var termindato = LocalDate.now().plusDays(22);
        var jordmorsdato = LocalDate.now().minusDays(10);
        var delvisTilrettelegging = LocalDate.now();
        var terminMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);
        lagreSøknad(behandling, termindato, LocalDate.now());

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List
                        .of(opprettTilrettelegging(jordmorsdato, delvisTilrettelegging, BigDecimal.valueOf(40), InternArbeidsforholdRef.nullRef())))
                .build();

        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);

        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        lagAktørArbeid(inntektArbeidYtelseAggregatBuilder, behandlingReferanse.aktørId(), LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(1));

        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        var tilretteleggingMedUtbelingsgrad = tjeneste.beregnPerioder(behandlingReferanse);

        assertThat(tilretteleggingMedUtbelingsgrad).hasSize(1);
        var periode1 = tilretteleggingMedUtbelingsgrad.get(0).getPeriodeMedUtbetalingsgrad().get(0);
        var periode2 = tilretteleggingMedUtbelingsgrad.get(0).getPeriodeMedUtbetalingsgrad().get(1);

        assertThat(periode1.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1)));
        assertThat(periode2.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminMinus3UkerOg1Dag));
        assertThat(periode1.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(periode2.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void skal_beregne_perioder_riktig_ved_to_arbeidsforhold_i_samme_bedrift() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(iayRepositoryProvider);
        var termindato = LocalDate.now().plusDays(22);
        var jordmorsdato = LocalDate.now().minusDays(10);
        var delvisTilrettelegging = LocalDate.now();
        var terminMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);
        lagreSøknad(behandling, termindato, LocalDate.now());

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(opprettTilrettelegging(jordmorsdato, delvisTilrettelegging, BigDecimal.valueOf(50), ARB_1),
                        opprettTilrettelegging(jordmorsdato, delvisTilrettelegging, BigDecimal.valueOf(40), ARB_2)))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);

        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        lagAktørArbeidMed2ArbeidsforholdIsammeVirksomhet(inntektArbeidYtelseAggregatBuilder, behandlingReferanse.aktørId(),
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        var tilretteleggingMedUtbelingsgrad = tjeneste.beregnPerioder(behandlingReferanse);

        assertThat(tilretteleggingMedUtbelingsgrad).hasSize(2);
        var arb1 = tilretteleggingMedUtbelingsgrad.stream()
                .filter(t -> t.getTilretteleggingArbeidsforhold().getInternArbeidsforholdRef().gjelderFor(ARB_1)).findFirst().get();
        var arb2 = tilretteleggingMedUtbelingsgrad.stream()
                .filter(t -> t.getTilretteleggingArbeidsforhold().getInternArbeidsforholdRef().gjelderFor(ARB_2)).findFirst().get();

        var arb1Periode1 = arb1.getPeriodeMedUtbetalingsgrad().get(0);
        var arb1Periode2 = arb1.getPeriodeMedUtbetalingsgrad().get(1);
        var arb2Periode1 = arb2.getPeriodeMedUtbetalingsgrad().get(0);
        var arb2Periode2 = arb2.getPeriodeMedUtbetalingsgrad().get(1);

        assertThat(arb1Periode1.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1)));
        assertThat(arb1Periode2.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminMinus3UkerOg1Dag));
        assertThat(arb1Periode1.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(arb1Periode2.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(33.33));

        assertThat(arb2Periode1.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1)));
        assertThat(arb2Periode2.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminMinus3UkerOg1Dag));
        assertThat(arb2Periode1.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(arb2Periode2.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void skal_beregne_perioder_riktig_ved_to_arbeidsforhold_i_samme_bedrift_der_det_ene_er_inaktivt_og_man_søker_uten_arbeidsforholdId() {
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(iayRepositoryProvider);
        var termindato = LocalDate.now().plusDays(22);
        var jordmorsdato = LocalDate.now().minusDays(10);
        var delvisTilrettelegging = LocalDate.now();
        var terminMinus3UkerOg1Dag = termindato.minusWeeks(3).minusDays(1);
        lagreSøknad(behandling, termindato, LocalDate.now());

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List
                        .of(opprettTilrettelegging(jordmorsdato, delvisTilrettelegging, BigDecimal.valueOf(50), InternArbeidsforholdRef.nullRef())))
                .build();

        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);

        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        lagAktørArbeidMed2ArbeidsforholdIsammeVirksomhet(inntektArbeidYtelseAggregatBuilder, behandlingReferanse.aktørId(),
                LocalDate.now().minusYears(2), LocalDate.now().minusYears(1), LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));

        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        var tilretteleggingMedUtbelingsgrad = tjeneste.beregnPerioder(behandlingReferanse);

        assertThat(tilretteleggingMedUtbelingsgrad).hasSize(1);
        var arb1 = tilretteleggingMedUtbelingsgrad.stream()
                .filter(t -> t.getTilretteleggingArbeidsforhold().getArbeidsgiver().get().equals(Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR)))
                .findFirst().get();

        var arb1Periode1 = arb1.getPeriodeMedUtbetalingsgrad().get(0);
        var arb1Periode2 = arb1.getPeriodeMedUtbetalingsgrad().get(1);

        assertThat(arb1Periode1.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1)));
        assertThat(arb1Periode2.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminMinus3UkerOg1Dag));
        assertThat(arb1Periode1.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(arb1Periode2.getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(37.50));
    }

    private SvpTilretteleggingEntitet opprettTilrettelegging(LocalDate jordmorsdato, LocalDate delvis, BigDecimal stillingsprosent,
            InternArbeidsforholdRef ref) {
        return new SvpTilretteleggingEntitet.Builder()
                .medKopiertFraTidligereBehandling(false)
                .medMottattTidspunkt(LocalDate.now().atStartOfDay())
                .medTilretteleggingFom(new TilretteleggingFOM.Builder()
                        .medFomDato(delvis)
                        .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                        .medStillingsprosent(stillingsprosent).build())
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR))
                .medInternArbeidsforholdRef(ref)
                .build();
    }

    private void lagreSøknad(Behandling behandling, LocalDate termindato, LocalDate søknadsdato) {
        byggFamilieHendelse(behandling, termindato);
        var søknad = new SøknadEntitet.Builder()
                .medElektroniskRegistrert(true)
                .medSøknadsdato(søknadsdato)
                .medMottattDato(søknadsdato)
                .medErEndringssøknad(false)
                .build();
        iayRepositoryProvider.getSøknadRepository().lagreOgFlush(behandling, søknad);
    }

    private FamilieHendelseEntitet byggFamilieHendelse(Behandling behandling, LocalDate termindato) {
        var søknadHendelse = familieHendelseRepository
                .opprettBuilderForSøknad(behandling.getId())
                .medAntallBarn(1);
        søknadHendelse.medTerminbekreftelse(søknadHendelse.getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(termindato.minusDays(21)));
        familieHendelseRepository.lagreSøknadHendelse(behandling.getId(), søknadHendelse);
        return familieHendelseRepository.hentAggregat(behandling.getId()).getSøknadVersjon();
    }

    private void lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId,
            LocalDate fom, LocalDate tom) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørArbeidBuilder(aktørId);

        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(ARBEIDSGIVER_ORGNR);
        var arbeidsgiver = Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR);

        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), true);

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        var prosent = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), false);
        prosent.medProsentsats(BigDecimal.valueOf(80));

        yrkesaktivitetBuilder
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(prosent)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private void lagAktørArbeidMed2ArbeidsforholdIsammeVirksomhet(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder,
            AktørId aktørId,
            LocalDate førsteFom, LocalDate førsteTom, LocalDate andreFom, LocalDate andreTom) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                .getAktørArbeidBuilder(aktørId);

        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(ARBEIDSGIVER_ORGNR);
        var arbeidsgiver = Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR);

        var yrkesaktivitetBuilder1 = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder1 = yrkesaktivitetBuilder1
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(førsteFom, førsteTom), true);

        var aktivitetsAvtale1 = aktivitetsAvtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(førsteFom, førsteTom));

        var prosent1 = yrkesaktivitetBuilder1
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(førsteFom, Tid.TIDENES_ENDE), false);
        prosent1.medProsentsats(BigDecimal.valueOf(75));

        yrkesaktivitetBuilder1
                .leggTilAktivitetsAvtale(aktivitetsAvtale1)
                .leggTilAktivitetsAvtale(prosent1)
                .medArbeidsforholdId(ARB_1)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver);

        var yrkesaktivitetBuilder2 = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder2 = yrkesaktivitetBuilder2
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(andreFom, andreTom), true);

        var aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(andreFom, andreTom));

        var prosent2 = yrkesaktivitetBuilder2
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(andreFom, Tid.TIDENES_ENDE), false);
        prosent2.medProsentsats(BigDecimal.valueOf(80));

        yrkesaktivitetBuilder2
                .leggTilAktivitetsAvtale(aktivitetsAvtale2)
                .leggTilAktivitetsAvtale(prosent2)
                .medArbeidsforholdId(ARB_2)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder2);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }
}
