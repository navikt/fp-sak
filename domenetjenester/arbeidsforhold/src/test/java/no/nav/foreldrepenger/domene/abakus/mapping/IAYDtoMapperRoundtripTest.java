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
import no.nav.abakus.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType;
import no.nav.abakus.iaygrunnlag.kodeverk.Landkode;
import no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.abakus.iaygrunnlag.kodeverk.TemaUnderkategori;
import no.nav.abakus.iaygrunnlag.kodeverk.UtbetaltYtelseFraOffentligeType;
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
        assertThat(fpsakGrunnlag.getEksternReferanse().toString()).isEqualTo(dto.getGrunnlagReferanse());

        // Assert oppgitt opptjening
        var fpsakOO = fpsakGrunnlag.getOppgittOpptjening().get();
        var dtoOO = dto.getOppgittOpptjening();
        assertThat(fpsakOO.getEksternReferanse().toString()).isEqualTo(dtoOO.getEksternReferanse().getReferanse());

        assertThat(fpsakOO.getEgenNæring().get(0).getOrgnr()).isEqualTo(dtoOO.getEgenNæring().get(0).getVirksomhet().getIdent());
        assertThat(fpsakOO.getEgenNæring().get(0).getBegrunnelse()).isEqualTo(dtoOO.getEgenNæring().get(0).getBegrunnelse());
        assertThat(fpsakOO.getEgenNæring().get(0).getBruttoInntekt()).isEqualTo(dtoOO.getEgenNæring().get(0).getBruttoInntekt());
        assertThat(fpsakOO.getEgenNæring().get(0).getEndringDato()).isEqualTo(dtoOO.getEgenNæring().get(0).getEndringDato());
        assertThat(fpsakOO.getEgenNæring().get(0).getNyIArbeidslivet()).isEqualTo(dtoOO.getEgenNæring().get(0).isNyIArbeidslivet());
        assertThat(fpsakOO.getEgenNæring().get(0).getVarigEndring()).isEqualTo(dtoOO.getEgenNæring().get(0).isVarigEndring());
        assertThat(fpsakOO.getEgenNæring().get(0).getNyoppstartet()).isEqualTo(dtoOO.getEgenNæring().get(0).isNyoppstartet());
        assertThat(fpsakOO.getEgenNæring().get(0).getRegnskapsførerNavn()).isEqualTo(dtoOO.getEgenNæring().get(0).getRegnskapsførerNavn());
        assertThat(fpsakOO.getEgenNæring().get(0).getRegnskapsførerTlf()).isEqualTo(dtoOO.getEgenNæring().get(0).getRegnskapsførerTlf());

        assertThat(fpsakOO.getAnnenAktivitet().get(0).getArbeidType().getKode()).isEqualTo(dtoOO.getAnnenAktivitet().get(0).getArbeidTypeDto().getKode());
        assertThat(fpsakOO.getAnnenAktivitet().get(0).getPeriode().getFomDato()).isEqualTo(dtoOO.getAnnenAktivitet().get(0).getPeriode().getFom());
        assertThat(fpsakOO.getAnnenAktivitet().get(0).getPeriode().getTomDato()).isEqualTo(dtoOO.getAnnenAktivitet().get(0).getPeriode().getTom());

        assertThat(fpsakOO.getOppgittArbeidsforhold().get(0).getArbeidType().getKode()).isEqualTo(dtoOO.getArbeidsforhold().get(0).getArbeidTypeDto().getKode());
        assertThat(fpsakOO.getOppgittArbeidsforhold().get(0).getPeriode().getFomDato()).isEqualTo(dtoOO.getArbeidsforhold().get(0).getPeriode().getFom());
        assertThat(fpsakOO.getOppgittArbeidsforhold().get(0).getPeriode().getTomDato()).isEqualTo(dtoOO.getArbeidsforhold().get(0).getPeriode().getTom());

        assertThat(fpsakOO.getFrilans().get().getErNyoppstartet()).isEqualTo(dtoOO.getFrilans().isErNyoppstartet());
        assertThat(fpsakOO.getFrilans().get().getHarInntektFraFosterhjem()).isEqualTo(dtoOO.getFrilans().isHarInntektFraFosterhjem());
        assertThat(fpsakOO.getFrilans().get().getHarNærRelasjon()).isEqualTo(dtoOO.getFrilans().isHarNærRelasjon());


        // Assert inntektsmeldinger
        var fpsakIM = fpsakGrunnlag.getInntektsmeldinger().get().getAlleInntektsmeldinger();
        var dtoIM = dto.getInntektsmeldinger().getInntektsmeldinger();
        assertThat(fpsakIM.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(dtoIM.get(0).getArbeidsgiver().getIdent());
        assertThat(fpsakIM.get(0).getArbeidsforholdRef().getReferanse()).isEqualTo(dtoIM.get(0).getArbeidsforholdRef().getAbakusReferanse());
        assertThat(fpsakIM.get(0).getKanalreferanse()).isEqualTo(dtoIM.get(0).getKanalreferanse());
        assertThat(fpsakIM.get(0).getInnsendingstidspunkt()).isEqualTo(dtoIM.get(0).getInnsendingstidspunkt().toLocalDateTime());
        assertThat(fpsakIM.get(0).getRefusjonBeløpPerMnd().getVerdi()).isEqualTo(dtoIM.get(0).getRefusjonsBeløpPerMnd());
        assertThat(fpsakIM.get(0).getInntektBeløp().getVerdi()).isEqualTo(dtoIM.get(0).getInntektBeløp());

        // Assert arbeid
        var fpsakYA = fpsakGrunnlag.getAktørArbeidFraRegister(aktørId).get().hentAlleYrkesaktiviteter().stream().toList();
        var dtoYA = dto.getRegister().getArbeid().get(0).getYrkesaktiviteter();

        assertThat(fpsakYA.get(0).getArbeidsforholdRef().getReferanse()).isEqualTo(dtoYA.get(0).getArbeidsforholdId().getAbakusReferanse());
        assertThat(fpsakYA.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(dtoYA.get(0).getArbeidsgiver().get().getIdent());
        assertThat(fpsakYA.get(0).getArbeidType().getKode()).isEqualTo(dtoYA.get(0).getType().getKode());

        // Assert inntekt
        var fpsakInntekt = fpsakGrunnlag.getAktørInntektFraRegister(aktørId).get().getInntekt().stream().toList();
        var dtoInntekt = dto.getRegister().getInntekt().get(0).getUtbetalinger();
        assertThat(fpsakInntekt.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(dtoInntekt.get(0).getUtbetaler().getIdent());
        assertThat(fpsakInntekt.get(0).getInntektsKilde().getKode()).isEqualTo(dtoInntekt.get(0).getKilde().getKode());

        // Assert ytelse
        var fpsakYtelse = fpsakGrunnlag.getAktørYtelseFraRegister(aktørId).get().getAlleYtelser().stream().toList();
        var dtoYtelse = dto.getRegister().getYtelse().get(0).getYtelser();
        assertThat(fpsakYtelse.get(0).getVedtattTidspunkt()).isEqualTo(dtoYtelse.get(0).getVedtattTidspunkt());
        assertThat(fpsakYtelse.get(0).getPeriode().getFomDato()).isEqualTo(dtoYtelse.get(0).getPeriode().getFom());
        assertThat(fpsakYtelse.get(0).getPeriode().getTomDato()).isEqualTo(dtoYtelse.get(0).getPeriode().getTom());
        assertThat(fpsakYtelse.get(0).getSaksnummer().getVerdi()).isEqualTo(dtoYtelse.get(0).getSaksnummer());
        assertThat(fpsakYtelse.get(0).getKilde().getKode()).isEqualTo(dtoYtelse.get(0).getFagsystemDto().getKode());
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
                                                                        .medUtbetaltYtelseType(UtbetaltYtelseFraOffentligeType.FORELDREPENGER)
                                                                        .medBeløp(100)
                                                                        .medSkattAvgiftType(SkatteOgAvgiftsregelType.NETTOLØNN)))))))
                        .medYtelse(List.of(
                                new YtelserDto(aktørIdent)
                                        .medYtelser(List.of(
                                                new YtelseDto(Fagsystem.FPSAK, ytelseType, periode, YtelseStatus.LØPENDE)
                                                        .medSaksnummer("1234")
                                                        .medTemaUnderkategori(TemaUnderkategori.fraKode("FØ"))
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
