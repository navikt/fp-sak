package no.nav.foreldrepenger.domene.abakus.mapping;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.Gradering;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.UtsettelsePeriode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.JournalpostId;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.GraderingDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.NaturalytelseDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.UtsettelsePeriodeDto;

public class MapInntektsmeldinger {
    private static final Comparator<RefusjonDto> COMP_ENDRINGER_REFUSJON = Comparator
        .comparing((RefusjonDto re) -> re.getFom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<GraderingDto> COMP_GRADERING = Comparator
        .comparing((GraderingDto dto) -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<NaturalytelseDto> COMP_NATURALYTELSE = Comparator
        .comparing((NaturalytelseDto dto) -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getType() == null ? null : dto.getType().getKode(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<UtsettelsePeriodeDto> COMP_UTSETTELSE = Comparator
        .comparing((UtsettelsePeriodeDto dto) -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getUtsettelseÅrsakDto() == null ? null : dto.getUtsettelseÅrsakDto().getKode(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<InntektsmeldingDto> COMP_INNTEKTSMELDING = Comparator
        .comparing((InntektsmeldingDto im) -> im.getArbeidsgiver().getIdent())
        .thenComparing(im -> im.getInnsendingstidspunkt())
        .thenComparing(im -> im.getArbeidsforholdRef() == null ? null : im.getArbeidsforholdRef().getAbakusReferanse(),
            Comparator.nullsLast(Comparator.naturalOrder()));

    private MapInntektsmeldinger() {
        // skjul public constructor
    }

    static class MapTilDto {
        private static final Logger log = LoggerFactory.getLogger(MapTilDto.class);

        MapTilDto() {
        }

        public InntektsmeldingerDto map(ArbeidsforholdInformasjon arbeidsforholdInformasjon,
                                        InntektsmeldingAggregat inntektsmeldingAggregat) {

            return map(arbeidsforholdInformasjon, inntektsmeldingAggregat, false);
        }

        public InntektsmeldingerDto map(ArbeidsforholdInformasjon arbeidsforholdInformasjon,
                                        InntektsmeldingAggregat inntektsmeldingAggregat, boolean validerArbeidsforholdId) {

            if (arbeidsforholdInformasjon == null && inntektsmeldingAggregat == null) {
                return null;
            } else if ((!validerArbeidsforholdId || arbeidsforholdInformasjon != null) && inntektsmeldingAggregat != null) {
                var dto = new InntektsmeldingerDto();
                var inntektsmeldinger = inntektsmeldingAggregat.getAlleInntektsmeldinger().stream()
                    .map(im -> this.mapInntektsmelding(arbeidsforholdInformasjon, im, validerArbeidsforholdId)).sorted(COMP_INNTEKTSMELDING).collect(Collectors.toList());
                dto.medInntektsmeldinger(inntektsmeldinger);

                return dto;
            } else {
                throw new IllegalStateException(
                    "Utvikler-feil: Både arbeidsforholdInformasjon og inntektsmeldingAggregat må samtidig eksistere, men har arbeidsforholdInformasjon:"
                        + arbeidsforholdInformasjon + ", inntektsmeldingAggregat=" + inntektsmeldingAggregat);
            }
        }

        private InntektsmeldingDto mapInntektsmelding(ArbeidsforholdInformasjon arbeidsforholdInformasjon, Inntektsmelding im, boolean validerArbeidsforholdId) {
            var arbeidsgiver = mapAktør(im.getArbeidsgiver());
            var journalpostId = new JournalpostId(im.getJournalpostId().getVerdi());
            var innsendingstidspunkt = im.getInnsendingstidspunkt();
            EksternArbeidsforholdRef eksternRef;
            try {
                eksternRef = validerArbeidsforholdId ? arbeidsforholdInformasjon.finnEksternRaw(im.getArbeidsgiver(), im.getArbeidsforholdRef()) : null;
            } catch (IllegalStateException e) {
                if (e.getMessage().startsWith("Mangler eksternReferanse for internReferanse:")) {
                    // Sukk, må håndtere at det ligger dritt her også ..
                    log.warn("Mangler eksternReferanse for internReferanse={}, forkaster internReferanse. Antar feilmapping", im.getArbeidsforholdRef());
                    eksternRef = null;
                } else {
                    throw e;
                }
            }

            var arbeidsforholdsDto = mapArbeidsforholdsId(im.getArbeidsgiver(), im.getArbeidsforholdRef(), eksternRef, validerArbeidsforholdId);
            var innsendingsårsak = KodeverkMapper.mapInntektsmeldingInnsendingsårsak(im.getInntektsmeldingInnsendingsårsak());
            var mottattDato = im.getMottattDato();

            var inntektsmeldingDto = new InntektsmeldingDto(arbeidsgiver, journalpostId, innsendingstidspunkt, mottattDato)
                .medArbeidsforholdRef(arbeidsforholdsDto)
                .medInnsendingsårsak(innsendingsårsak)
                .medInntektBeløp(im.getInntektBeløp().getVerdi())
                .medKanalreferanse(im.getKanalreferanse())
                .medKildesystem(im.getKildesystem())
                .medRefusjonOpphører(im.getRefusjonOpphører())
                .medRefusjonsBeløpPerMnd(im.getRefusjonBeløpPerMnd() == null ? null : im.getRefusjonBeløpPerMnd().getVerdi())
                .medStartDatoPermisjon(im.getStartDatoPermisjon().orElse(null))
                .medNærRelasjon(im.getErNærRelasjon());

            inntektsmeldingDto.medEndringerRefusjon(im.getEndringerRefusjon().stream().map(this::mapEndringRefusjon).sorted(COMP_ENDRINGER_REFUSJON).collect(Collectors.toList()));

            inntektsmeldingDto.medGraderinger(im.getGraderinger().stream().map(this::mapGradering).sorted(COMP_GRADERING).collect(Collectors.toList()));

            inntektsmeldingDto.medNaturalytelser(im.getNaturalYtelser().stream().map(this::mapNaturalytelse).sorted(COMP_NATURALYTELSE).collect(Collectors.toList()));

            inntektsmeldingDto.medUtsettelsePerioder(im.getUtsettelsePerioder().stream().map(this::mapUtsettelsePeriode).sorted(COMP_UTSETTELSE).collect(Collectors.toList()));

            return inntektsmeldingDto;
        }

        private RefusjonDto mapEndringRefusjon(Refusjon refusjon) {
            return new RefusjonDto(refusjon.getFom(), refusjon.getRefusjonsbeløp().getVerdi());
        }

        private GraderingDto mapGradering(Gradering gradering) {
            var periode = gradering.getPeriode();
            var arbeidstidProsent = gradering.getArbeidstidProsent();
            return new GraderingDto(new Periode(periode.getFomDato(), periode.getTomDato()), arbeidstidProsent.getVerdi());
        }

        private NaturalytelseDto mapNaturalytelse(NaturalYtelse naturalYtelse) {
            var periode = naturalYtelse.getPeriode();
            var type = KodeverkMapper.mapNaturalYtelseTilDto(naturalYtelse.getType());
            var beløpPerMnd = naturalYtelse.getBeloepPerMnd().getVerdi();
            return new NaturalytelseDto(new Periode(periode.getFomDato(), periode.getTomDato()), type, beløpPerMnd);
        }

        private UtsettelsePeriodeDto mapUtsettelsePeriode(UtsettelsePeriode utsettelsePeriode) {
            var periode = utsettelsePeriode.getPeriode();
            var utsettelseÅrsak = KodeverkMapper.mapUtsettelseÅrsakTilDto(utsettelsePeriode.getÅrsak());
            return new UtsettelsePeriodeDto(new Periode(periode.getFomDato(), periode.getTomDato()), utsettelseÅrsak);
        }

        private Aktør mapAktør(Arbeidsgiver arbeidsgiver) {
            return arbeidsgiver.erAktørId()
                ? new AktørIdPersonident(arbeidsgiver.getAktørId().getId())
                : new Organisasjon(arbeidsgiver.getOrgnr());
        }

        private ArbeidsforholdRefDto mapArbeidsforholdsId(@SuppressWarnings("unused") Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internRef,
                                                          EksternArbeidsforholdRef eksternRef, boolean validerArbeidsforholdId) {
            if ((internRef == null || internRef.getReferanse() == null) && (eksternRef == null || eksternRef.getReferanse() == null)) {
                return null;
            } else if (internRef != null && eksternRef != null && internRef.getReferanse() != null && eksternRef.getReferanse() != null) {
                return new ArbeidsforholdRefDto(internRef.getReferanse(), eksternRef.getReferanse(),
                    no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem.AAREGISTERET);
            } else if (!validerArbeidsforholdId && eksternRef != null && eksternRef.getReferanse() != null) {
                return new ArbeidsforholdRefDto(null, eksternRef.getReferanse(),
                    no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem.AAREGISTERET);
            } else if (internRef != null && internRef.getReferanse() != null && eksternRef == null) {
                return null;
            } else {
                throw new IllegalStateException(
                    "Både internArbeidsforholdRef og eksternArbeidsforholdRef må være satt (eller begge ikke satt), har nå internRef=" + internRef
                        + ", eksternRef=" + eksternRef);
            }
        }

        public InntektsmeldingerDto map(Collection<InntektsmeldingBuilder> inntektsmeldingBuildere) {
            if (inntektsmeldingBuildere == null || inntektsmeldingBuildere.isEmpty()) {
                return null;
            }
            return new InntektsmeldingerDto()
                .medInntektsmeldinger(inntektsmeldingBuildere.stream().map(this::map).collect(Collectors.toList()));
        }

        private InntektsmeldingDto map(InntektsmeldingBuilder builder) {
            final var im = builder.build(true);
            var arbeidsgiver = mapAktør(im.getArbeidsgiver());
            var journalpostId = new JournalpostId(im.getJournalpostId().getVerdi());
            var innsendingstidspunkt = im.getInnsendingstidspunkt();
            var eksternRef = builder.getEksternArbeidsforholdRef().orElse(null);
            var arbeidsforholdsDto = mapArbeidsforholdsId(im.getArbeidsgiver(), im.getArbeidsforholdRef(), eksternRef, false);
            var innsendingsårsak = KodeverkMapper.mapInntektsmeldingInnsendingsårsak(im.getInntektsmeldingInnsendingsårsak());
            var mottattDato = im.getMottattDato();

            var inntektsmeldingDto = new InntektsmeldingDto(arbeidsgiver, journalpostId, innsendingstidspunkt, mottattDato)
                .medArbeidsforholdRef(arbeidsforholdsDto)
                .medInnsendingsårsak(innsendingsårsak)
                .medInntektBeløp(im.getInntektBeløp().getVerdi())
                .medKanalreferanse(im.getKanalreferanse())
                .medKildesystem(im.getKildesystem())
                .medRefusjonOpphører(im.getRefusjonOpphører())
                .medRefusjonsBeløpPerMnd(im.getRefusjonBeløpPerMnd() == null ? null : im.getRefusjonBeløpPerMnd().getVerdi())
                .medStartDatoPermisjon(im.getStartDatoPermisjon().orElse(null))
                .medNærRelasjon(im.getErNærRelasjon());

            inntektsmeldingDto.medEndringerRefusjon(im.getEndringerRefusjon().stream().map(this::mapEndringRefusjon).sorted(COMP_ENDRINGER_REFUSJON).collect(Collectors.toList()));

            inntektsmeldingDto.medGraderinger(im.getGraderinger().stream().map(this::mapGradering).sorted(COMP_GRADERING).collect(Collectors.toList()));

            inntektsmeldingDto.medNaturalytelser(im.getNaturalYtelser().stream().map(this::mapNaturalytelse).sorted(COMP_NATURALYTELSE).collect(Collectors.toList()));

            inntektsmeldingDto.medUtsettelsePerioder(im.getUtsettelsePerioder().stream().map(this::mapUtsettelsePeriode).sorted(COMP_UTSETTELSE).collect(Collectors.toList()));

            return inntektsmeldingDto;
        }
    }

    public static class MapFraDto {

        public InntektsmeldingAggregat map(ArbeidsforholdInformasjonBuilder arbeidsforholdInformasjon, InntektsmeldingerDto dto) {
            if (dto == null) {
                return null;
            }
            var inntektsmeldinger = dto.getInntektsmeldinger().stream().map(im -> mapInntektsmelding(arbeidsforholdInformasjon, im)).collect(Collectors.toList());
            return new InntektsmeldingAggregat(inntektsmeldinger);
        }

        private Inntektsmelding mapInntektsmelding(ArbeidsforholdInformasjonBuilder arbeidsforholdInformasjon, InntektsmeldingDto dto) {
            var arbeidsgiver = mapArbeidsgiver(dto.getArbeidsgiver());

            var arbeidsforholdRef = dto.getArbeidsforholdRef();
            InternArbeidsforholdRef internRef = arbeidsforholdRef == null || arbeidsforholdRef.getAbakusReferanse() == null ? InternArbeidsforholdRef.nullRef()
                : InternArbeidsforholdRef.ref(arbeidsforholdRef.getAbakusReferanse());
            EksternArbeidsforholdRef eksternRef = arbeidsforholdRef == null || arbeidsforholdRef.getEksternReferanse() == null ? null
                : EksternArbeidsforholdRef.ref(arbeidsforholdRef.getEksternReferanse());

            if ((internRef.getReferanse() != null && eksternRef == null) || (internRef.getReferanse() == null && eksternRef != null)) {
                throw new IllegalStateException(
                    "Både internRef og eksternRef må enten være satt eller begge null, fikk intern=" + internRef + ", ekstern=" + eksternRef);
            } else if (!InternArbeidsforholdRef.nullRef().equals(internRef)) {
                arbeidsforholdInformasjon.leggTilNyReferanse(new ArbeidsforholdReferanse(arbeidsgiver, internRef, eksternRef));
            }

            var journalpostId = dto.getJournalpostId().getId();
            var innsendingstidspunkt = dto.getInnsendingstidspunkt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            var innsendingsårsak = KodeverkMapper.mapInntektsmeldingInnsendingsårsakFraDto(dto.getInnsendingsårsak());

            var builder = InntektsmeldingBuilder.builder()
                .medJournalpostId(journalpostId)
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(innsendingstidspunkt)
                .medBeløp(dto.getInntektBeløp())
                .medArbeidsforholdId(eksternRef)
                .medArbeidsforholdId(internRef)
                .medStartDatoPermisjon(dto.getStartDatoPermisjon())
                .medRefusjon(dto.getRefusjonsBeløpPerMnd(), dto.getRefusjonOpphører())
                .medKanalreferanse(dto.getKanalreferanse())
                .medInntektsmeldingaarsak(innsendingsårsak)
                .medNærRelasjon(dto.isNærRelasjon() == null ? false : dto.isNærRelasjon())
                .medKildesystem(dto.getKildesystem())
                .medMottattDato(dto.getMottattDato());

            dto.getEndringerRefusjon().stream()
                .map(eir -> new Refusjon(eir.getRefusjonsbeløpMnd(), eir.getFom()))
                .forEach(builder::leggTil);

            dto.getGraderinger().stream()
                .map(gr -> {
                    var periode = gr.getPeriode();
                    return new Gradering(periode.getFom(), periode.getTom(), gr.getArbeidstidProsent());
                })
                .forEach(builder::leggTil);

            dto.getNaturalytelser().stream()
                .map(ny -> {
                    var periode = ny.getPeriode();
                    var naturalYtelseType = KodeverkMapper.mapNaturalYtelseFraDto(ny.getType());
                    return new NaturalYtelse(periode.getFom(), periode.getTom(), ny.getBeløpPerMnd(), naturalYtelseType);
                })
                .forEach(builder::leggTil);

            dto.getUtsettelsePerioder().stream()
                .map(up -> {
                    var periode = up.getPeriode();
                    var utsettelseÅrsak = KodeverkMapper.mapUtsettelseÅrsakFraDto(up.getUtsettelseÅrsakDto());
                    return UtsettelsePeriode.utsettelse(periode.getFom(), periode.getTom(), utsettelseÅrsak);
                })
                .forEach(builder::leggTil);

            return builder.build();
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiverDto) {
            if (arbeidsgiverDto == null) {
                return null;
            }
            String identifikator = arbeidsgiverDto.getIdent();
            if (arbeidsgiverDto.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(identifikator));
            }
            if (arbeidsgiverDto.getErPerson()) {
                return Arbeidsgiver.person(new AktørId(identifikator));
            }
            throw new IllegalArgumentException();
        }
    }

}
