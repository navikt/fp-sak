package no.nav.foreldrepenger.domene.abakus.mapping;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.JournalpostId;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.abakus.iaygrunnlag.arbeid.v1.ArbeidDto;
import no.nav.abakus.iaygrunnlag.arbeid.v1.PermisjonDto;
import no.nav.abakus.iaygrunnlag.arbeid.v1.YrkesaktivitetDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.InntekterDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingsPostDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.GraderingDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.NaturalytelseDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.UtsettelsePeriodeDto;
import no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType;
import no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori;
import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektYtelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType;
import no.nav.abakus.iaygrunnlag.kodeverk.Landkode;
import no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.abakus.iaygrunnlag.kodeverk.UtsettelseÅrsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittAnnenAktivitetDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittEgenNæringDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittFrilansDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittFrilansoppdragDto;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatOverstyrtDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseAggregatRegisterDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.AnvisningDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.FordelingDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.YtelseDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.YtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.ytelse.v1.YtelserDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class IAYDtoMapperRoundtripTest {

    private final UUID uuid = UUID.randomUUID();
    private final LocalDate fom = LocalDate.now();
    private final LocalDate tom = LocalDate.now();
    private final AktørIdPersonident aktørIdent = new AktørIdPersonident("0000000000000");
    private final AktørId aktørId = new AktørId(aktørIdent.getIdent());
    private final Organisasjon org = new Organisasjon(KUNSTIG_ORG);
    private final ArbeidType arbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
    private final Periode periode = new Periode(fom, tom);
    private final YtelseType ytelseType = YtelseType.FORELDREPENGER;
    private final LocalDateTime tidspunkt = LocalDateTime.now();
    private final OffsetDateTime offTidspunkt = OffsetDateTime.now();
    private final JournalpostId journalpostId = new JournalpostId("ImajournalpostId");
    private final ArbeidsforholdRefDto arbeidsforholdId = new ArbeidsforholdRefDto(InternArbeidsforholdRef.nyRef().getReferanse(),
            "aaregRef");

    private final IAYFraDtoMapper fraDtoMapper = new IAYFraDtoMapper(aktørId);

    @Test
    void roundtrip_mapping_dto_til_grunnlag() {
        // Arrange
        var dto = lagIAYGrunnlag();

        // Act
        var fpsakGrunnlag = fraDtoMapper.mapTilGrunnlagInklusivRegisterdata(dto, true);

        // Assert
        assertThat(fpsakGrunnlag.getEksternReferanse()).hasToString(dto.getGrunnlagReferanse());

        // Assert oppgitt opptjening
        var fpsakOO = fpsakGrunnlag.getGjeldendeOppgittOpptjening().orElseThrow();
        var dtoOO = dto.getOppgittOpptjening();
        assertThat(fpsakOO.getEksternReferanse()).hasToString(dtoOO.getEksternReferanse().getReferanse());

        assertThat(fpsakOO.getEgenNæring().getFirst().getOrgnr()).isEqualTo(dtoOO.getEgenNæring().getFirst().getVirksomhet().getIdent());
        assertThat(fpsakOO.getEgenNæring().getFirst().getBegrunnelse()).isEqualTo(dtoOO.getEgenNæring().getFirst().getBegrunnelse());
        assertThat(fpsakOO.getEgenNæring().getFirst().getBruttoInntekt()).isEqualTo(dtoOO.getEgenNæring().getFirst().getBruttoInntekt());
        assertThat(fpsakOO.getEgenNæring().getFirst().getEndringDato()).isEqualTo(dtoOO.getEgenNæring().getFirst().getEndringDato());
        assertThat(fpsakOO.getEgenNæring().getFirst().getNyIArbeidslivet()).isEqualTo(dtoOO.getEgenNæring().getFirst().isNyIArbeidslivet());
        assertThat(fpsakOO.getEgenNæring().getFirst().getVarigEndring()).isEqualTo(dtoOO.getEgenNæring().getFirst().isVarigEndring());
        assertThat(fpsakOO.getEgenNæring().getFirst().getNyoppstartet()).isEqualTo(dtoOO.getEgenNæring().getFirst().isNyoppstartet());
        assertThat(fpsakOO.getEgenNæring().getFirst().getRegnskapsførerNavn()).isEqualTo(dtoOO.getEgenNæring().getFirst().getRegnskapsførerNavn());
        assertThat(fpsakOO.getEgenNæring().getFirst().getRegnskapsførerTlf()).isEqualTo(dtoOO.getEgenNæring().getFirst().getRegnskapsførerTlf());

        assertThat(fpsakOO.getAnnenAktivitet().getFirst().getArbeidType().getKode()).isEqualTo(dtoOO.getAnnenAktivitet().getFirst().getArbeidTypeDto().getKode());
        assertThat(fpsakOO.getAnnenAktivitet().getFirst().getPeriode().getFomDato()).isEqualTo(dtoOO.getAnnenAktivitet().getFirst().getPeriode().getFom());
        assertThat(fpsakOO.getAnnenAktivitet().getFirst().getPeriode().getTomDato()).isEqualTo(dtoOO.getAnnenAktivitet().getFirst().getPeriode().getTom());

        assertThat(fpsakOO.getOppgittArbeidsforhold().getFirst().getArbeidType().getKode()).isEqualTo(dtoOO.getArbeidsforhold().getFirst().getArbeidTypeDto().getKode());
        assertThat(fpsakOO.getOppgittArbeidsforhold().getFirst().getPeriode().getFomDato()).isEqualTo(dtoOO.getArbeidsforhold().getFirst().getPeriode().getFom());
        assertThat(fpsakOO.getOppgittArbeidsforhold().getFirst().getPeriode().getTomDato()).isEqualTo(dtoOO.getArbeidsforhold().getFirst().getPeriode().getTom());

        assertThat(fpsakOO.getFrilans().get().getErNyoppstartet()).isEqualTo(dtoOO.getFrilans().isErNyoppstartet());
        assertThat(fpsakOO.getFrilans().get().getHarInntektFraFosterhjem()).isEqualTo(dtoOO.getFrilans().isHarInntektFraFosterhjem());
        assertThat(fpsakOO.getFrilans().get().getHarNærRelasjon()).isEqualTo(dtoOO.getFrilans().isHarNærRelasjon());


        // Assert inntektsmeldinger
        var fpsakIM = fpsakGrunnlag.getInntektsmeldinger().get().getAlleInntektsmeldinger();
        var dtoIM = dto.getInntektsmeldinger().getInntektsmeldinger();
        assertThat(fpsakIM.getFirst().getArbeidsgiver().getIdentifikator()).isEqualTo(dtoIM.getFirst().getArbeidsgiver().getIdent());
        assertThat(fpsakIM.getFirst().getArbeidsforholdRef().getReferanse()).isEqualTo(dtoIM.getFirst().getArbeidsforholdRef().getAbakusReferanse());
        assertThat(fpsakIM.getFirst().getKanalreferanse()).isEqualTo(dtoIM.getFirst().getKanalreferanse());
        assertThat(fpsakIM.getFirst().getInnsendingstidspunkt()).isEqualTo(dtoIM.getFirst().getInnsendingstidspunkt().toLocalDateTime());
        assertThat(fpsakIM.getFirst().getRefusjonBeløpPerMnd().getVerdi()).isEqualTo(dtoIM.getFirst().getRefusjonsBeløpPerMnd());
        assertThat(fpsakIM.getFirst().getInntektBeløp().getVerdi()).isEqualTo(dtoIM.getFirst().getInntektBeløp());

        // Assert arbeid
        var fpsakYA = fpsakGrunnlag.getAktørArbeidFraRegister(aktørId).get().hentAlleYrkesaktiviteter().stream().toList();
        var dtoYA = dto.getRegister().getArbeid().getFirst().getYrkesaktiviteter();

        assertThat(fpsakYA.getFirst().getArbeidsforholdRef().getReferanse()).isEqualTo(dtoYA.getFirst().getArbeidsforholdId().getAbakusReferanse());
        assertThat(fpsakYA.getFirst().getArbeidsgiver().getIdentifikator()).isEqualTo(dtoYA.getFirst().getArbeidsgiver().get().getIdent());
        assertThat(fpsakYA.getFirst().getArbeidType().getKode()).isEqualTo(dtoYA.getFirst().getType().getKode());

        // Assert inntekt
        var fpsakInntekt = fpsakGrunnlag.getAktørInntektFraRegister(aktørId).get().getInntekt().stream().toList();
        var dtoInntekt = dto.getRegister().getInntekt().getFirst().getUtbetalinger();
        assertThat(fpsakInntekt.getFirst().getArbeidsgiver().getIdentifikator()).isEqualTo(dtoInntekt.getFirst().getUtbetaler().getIdent());
        assertThat(fpsakInntekt.getFirst().getInntektsKilde().getKode()).isEqualTo(dtoInntekt.getFirst().getKilde().getKode());
        var x = fpsakInntekt.getFirst().getAlleInntektsposter().stream().findFirst().orElseThrow();
        assertThat(fpsakInntekt.getFirst().getAlleInntektsposter().stream().findFirst().orElseThrow().getInntektYtelseType().getKode()).isEqualTo(InntektYtelseType.FORELDREPENGER.getKode());

        // Assert ytelse
        var fpsakYtelse = fpsakGrunnlag.getAktørYtelseFraRegister(aktørId).get().getAlleYtelser().stream().toList();
        var dtoYtelse = dto.getRegister().getYtelse().getFirst().getYtelser();
        assertThat(fpsakYtelse.getFirst().getVedtattTidspunkt()).isEqualTo(dtoYtelse.getFirst().getVedtattTidspunkt());
        assertThat(fpsakYtelse.getFirst().getPeriode().getFomDato()).isEqualTo(dtoYtelse.getFirst().getPeriode().getFom());
        assertThat(fpsakYtelse.getFirst().getPeriode().getTomDato()).isEqualTo(dtoYtelse.getFirst().getPeriode().getTom());
        assertThat(fpsakYtelse.getFirst().getSaksnummer().getVerdi()).isEqualTo(dtoYtelse.getFirst().getSaksnummer());
        assertThat(fpsakYtelse.getFirst().getKilde().getKode()).isEqualTo(dtoYtelse.getFirst().getFagsystemDto().getKode());
    }

    private InntektArbeidYtelseGrunnlagDto lagIAYGrunnlag() {
        var grunnlag = new InntektArbeidYtelseGrunnlagDto(aktørIdent, offTidspunkt, uuid, uuid, YtelseType.FORELDREPENGER);

        grunnlag.medRegister(
                new InntektArbeidYtelseAggregatRegisterDto(tidspunkt, uuid)
                        .medArbeid(List.of(
                                new ArbeidDto(aktørIdent)
                                        .medYrkesaktiviteter(List.of(
                                                new YrkesaktivitetDto(arbeidType)
                                                        .medArbeidsgiver(org)
                                                        .medPermisjoner(List.of(
                                                                new PermisjonDto(periode, PermisjonsbeskrivelseType.PERMISJON).medProsentsats(50)))
                                                        .medArbeidsforholdId(arbeidsforholdId)
                                                        .medNavnArbeidsgiverUtland("utlandskNavnAS")
                                                        .medAktivitetsAvtaler(List.of(
                                                                new AktivitetsAvtaleDto(periode)
                                                                        .medSistLønnsendring(fom)
                                                                        .medBeskrivelse("Beskrivelse")
                                                                        .medStillingsprosent(50)))))))
                        .medInntekt(List.of(
                                new InntekterDto(aktørIdent)
                                        .medUtbetalinger(List.of(
                                                new UtbetalingDto("INNTEKT_BEREGNING")
                                                        .medArbeidsgiver(org)
                                                        .medPoster(List.of(
                                                                new UtbetalingsPostDto(periode, InntektspostType.fraKode("LØNN"))
                                                                        .medInntektYtelseType(InntektYtelseType.FORELDREPENGER)
                                                                        .medBeløp(100)
                                                                        .medSkattAvgiftType(SkatteOgAvgiftsregelType.NETTOLØNN)))))))
                        .medYtelse(List.of(
                                new YtelserDto(aktørIdent)
                                        .medYtelser(List.of(
                                                new YtelseDto(Fagsystem.FPSAK, ytelseType, periode, YtelseStatus.LØPENDE)
                                                        .medSaksnummer("1234")
                                                        .medGrunnlag(
                                                                new YtelseGrunnlagDto()
                                                                        .medArbeidskategoriDto(Arbeidskategori.ARBEIDSTAKER)
                                                                        .medOpprinneligIdentDato(fom)
                                                                        .medDekningsgradProsent(100)
                                                                        .medInntektsgrunnlagProsent(100)
                                                                        .medGraderingProsent(100)
                                                                        .medFordeling(List.of(new FordelingDto(org, InntektPeriodeType.DAGLIG, 100, false))))
                                                        .medAnvisninger(List.of(
                                                                new AnvisningDto(periode)
                                                                        .medBeløp(100)
                                                                        .medDagsats(100)
                                                                        .medUtbetalingsgrad(100))))))))
                .medOverstyrt(
                        new InntektArbeidYtelseAggregatOverstyrtDto(tidspunkt, uuid)
                                .medArbeid(List.of(
                                        new ArbeidDto(aktørIdent)
                                                .medYrkesaktiviteter(List.of(
                                                        new YrkesaktivitetDto(arbeidType)
                                                                .medArbeidsgiver(org)
                                                                .medPermisjoner(List.of(new PermisjonDto(periode, PermisjonsbeskrivelseType.PERMISJON)
                                                                        .medProsentsats(50)))
                                                                .medArbeidsforholdId(arbeidsforholdId)
                                                                .medAktivitetsAvtaler(List.of(
                                                                        new AktivitetsAvtaleDto(periode)
                                                                                .medSistLønnsendring(tom)
                                                                                .medBeskrivelse("beskrivelse")
                                                                                .medStillingsprosent(30))))))))
                .medInntektsmeldinger(
                        new InntektsmeldingerDto()
                                .medInntektsmeldinger(List.of(
                                        new InntektsmeldingDto(org, journalpostId, tidspunkt, fom)
                                                .medArbeidsforholdRef(arbeidsforholdId)
                                                .medInnsendingsårsak(InntektsmeldingInnsendingsårsakType.NY)
                                                .medInntektBeløp(99999)
                                                .medKanalreferanse("BBC")
                                                .medKildesystem("TheSource")
                                                .medRefusjonOpphører(fom)
                                                .medRefusjonsBeløpPerMnd(100)
                                                .medStartDatoPermisjon(fom)
                                                .medNærRelasjon(false)
                                                .medEndringerRefusjon(List.of(new RefusjonDto(fom, 100)))
                                                .medGraderinger(List.of(new GraderingDto(periode, 50)))
                                                .medNaturalytelser(
                                                        List.of(new NaturalytelseDto(periode, NaturalytelseType.ELEKTRISK_KOMMUNIKASJON, 100)))
                                                .medUtsettelsePerioder(List.of(new UtsettelsePeriodeDto(periode, UtsettelseÅrsakType.FERIE))))))
                .medOppgittOpptjening(
                        new OppgittOpptjeningDto(uuid, tidspunkt)
                                .medArbeidsforhold(List.of(
                                        new OppgittArbeidsforholdDto(periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                                                .medErUtenlandskInntekt(true)
                                                .medOppgittVirksomhetNavn("GammelDansk", Landkode.DNK)))
                                .medEgenNæring(List.of(
                                        new OppgittEgenNæringDto(periode)
                                                .medBegrunnelse("MinBegrunnelse")
                                                .medBruttoInntekt(10000)
                                                .medEndringDato(fom)
                                                .medNyIArbeidslivet(false)
                                                .medNyoppstartet(false)
                                                .medNærRelasjon(false)
                                                .medOppgittVirksomhetNavn("DuGamleDuFria", Landkode.SWE)
                                                .medRegnskapsførerNavn("Regnskapsfører")
                                                .medRegnskapsførerTlf("TELEFON")
                                                .medVarigEndring(true)
                                                .medVirksomhet(org)
                                                .medVirksomhetType(VirksomhetType.ANNEN)))
                                .medAnnenAktivitet(List.of(new OppgittAnnenAktivitetDto(periode, arbeidType)))
                                .medFrilans(new OppgittFrilansDto(List.of(
                                        new OppgittFrilansoppdragDto(periode, "MittOppdrag")))
                                                .medErNyoppstartet(false)
                                                .medHarInntektFraFosterhjem(false)
                                                .medHarNærRelasjon(false)));

        return grunnlag;

    }

}
