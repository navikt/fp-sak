package no.nav.foreldrepenger.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.*;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdInformasjon;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdOverstyringDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdReferanseDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.*;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class MapArbeidsforholdInformasjon {

    private static final Comparator<ArbeidsforholdOverstyringDto> COMP_ARBEIDSFORHOLD_OVERSTYRING = Comparator.comparing(
            (ArbeidsforholdOverstyringDto ov) -> ov.getArbeidsgiver().getIdent())
        .thenComparing(ov -> ov.getArbeidsforholdRef() == null ? null : ov.getArbeidsforholdRef().getAbakusReferanse(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<ArbeidsforholdReferanseDto> COMP_ARBEIDSFORHOLD_REFERANSE = Comparator.comparing(
            (ArbeidsforholdReferanseDto ref) -> ref.getArbeidsgiver().getIdent())
        .thenComparing(ref -> ref.getArbeidsforholdReferanse() == null ? null : ref.getArbeidsforholdReferanse().getAbakusReferanse(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<Periode> COMP_PERIODE = Comparator.comparing(Periode::getFom, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(Periode::getTom, Comparator.nullsLast(Comparator.naturalOrder()));

    private MapArbeidsforholdInformasjon() {
        // hidden
    }

    static class MapFraDto {
        private InntektArbeidYtelseGrunnlagBuilder grunnlagBuilder;

        public MapFraDto(InntektArbeidYtelseGrunnlagBuilder builder) {
            this.grunnlagBuilder = builder;
        }

        private ArbeidsforholdOverstyringBuilder mapArbeidsforholdOverstyring(ArbeidsforholdOverstyringDto ov,
                                                                              ArbeidsforholdInformasjonBuilder builder) {
            var arbeidsgiverRef = mapArbeidsforholdRef(ov.getArbeidsforholdRef());
            var nyArbeidsgiverRef = mapArbeidsforholdRef(ov.getNyArbeidsforholdRef());
            var arbeidsgiver = mapArbeidsgiver(ov.getArbeidsgiver());

            var overstyringBuilder = builder.getOverstyringBuilderFor(arbeidsgiver, arbeidsgiverRef);

            overstyringBuilder.medBeskrivelse(ov.getBegrunnelse())
                .medNyArbeidsforholdRef(nyArbeidsgiverRef)
                .medHandling(KodeverkMapper.mapArbeidsforholdHandlingTypeFraDto(ov.getHandling()))
                .medAngittArbeidsgiverNavn(ov.getAngittArbeidsgiverNavn())
                .medAngittStillingsprosent(ov.getStillingsprosent() == null ? null : new Stillingsprosent(ov.getStillingsprosent()));

            ov.getBekreftetPermisjon().ifPresent(bp -> {
                var bekreftetPermisjon = new BekreftetPermisjon(bp.getPeriode().getFom(), bp.getPeriode().getTom(),
                    KodeverkMapper.getBekreftetPermisjonStatus(bp.getBekreftetPermisjonStatus()));
                overstyringBuilder.medBekreftetPermisjon(bekreftetPermisjon);
            });

            // overstyrte perioder
            ov.getArbeidsforholdOverstyrtePerioder().forEach(p -> overstyringBuilder.leggTilOverstyrtPeriode(p.getFom(), p.getTom()));

            return overstyringBuilder;
        }

        private InternArbeidsforholdRef mapArbeidsforholdRef(ArbeidsforholdRefDto arbeidsforholdId) {
            if (arbeidsforholdId == null) {
                return InternArbeidsforholdRef.nullRef();
            }
            return InternArbeidsforholdRef.ref(arbeidsforholdId.getAbakusReferanse());
        }

        private ArbeidsforholdReferanse mapArbeidsforholdReferanse(ArbeidsforholdReferanseDto r) {
            var internRef = InternArbeidsforholdRef.ref(r.getArbeidsforholdReferanse().getAbakusReferanse());
            var eksternRef = EksternArbeidsforholdRef.ref(r.getArbeidsforholdReferanse().getEksternReferanse());
            var arbeidsgiver = mapArbeidsgiver(r.getArbeidsgiver());
            if (!internRef.gjelderForSpesifiktArbeidsforhold() && !eksternRef.gjelderForSpesifiktArbeidsforhold()) {
                return null;
            }
            return new ArbeidsforholdReferanse(arbeidsgiver, internRef, eksternRef);
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
            if (arbeidsgiver.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(arbeidsgiver.getIdent()));
            }
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }

        ArbeidsforholdInformasjonBuilder map(ArbeidsforholdInformasjon dto) {
            var eksisterende = grunnlagBuilder.getArbeidsforholdInformasjon();
            var builder = ArbeidsforholdInformasjonBuilder.builder(eksisterende);
            if (dto != null) {
                dto.getOverstyringer().stream().map(ov -> mapArbeidsforholdOverstyring(ov, builder)).forEach(builder::leggTil);
                dto.getReferanser().stream().map(this::mapArbeidsforholdReferanse).forEach(builder::leggTilNyReferanse);
            }
            return builder;
        }
    }

    static class MapTilDto {

        private static final Logger LOG = LoggerFactory.getLogger(MapTilDto.class);

        private List<Periode> map(List<ArbeidsforholdOverstyrtePerioder> perioder) {
            return perioder == null ? null : perioder.stream()
                .map(ArbeidsforholdOverstyrtePerioder::getOverstyrtePeriode)
                .map(this::mapPeriode)
                .sorted(COMP_PERIODE)
                .toList();
        }

        private Aktør mapAktør(Arbeidsgiver arbeidsgiver) {
            return arbeidsgiver.erAktørId() ? new AktørIdPersonident(arbeidsgiver.getAktørId().getId()) : new Organisasjon(arbeidsgiver.getOrgnr());
        }

        private Optional<ArbeidsforholdReferanseDto> mapArbeidsforholdReferanse(ArbeidsforholdReferanse ref) {
            var arbeidsgiver = mapAktør(ref.getArbeidsgiver());
            var internReferanse = ref.getInternReferanse() != null ? ref.getInternReferanse().getReferanse() : null;
            var eksternReferanse = ref.getEksternReferanse() != null ? ref.getEksternReferanse().getReferanse() : null;

            if (internReferanse != null && eksternReferanse != null) {
                return Optional.of(new ArbeidsforholdReferanseDto(arbeidsgiver, new ArbeidsforholdRefDto(internReferanse, eksternReferanse)));
            }
            if (internReferanse == null && eksternReferanse != null) {
                LOG.warn("Mangler internReferanse for eksternReferanse: {}, arbeidsgiver={}", eksternReferanse, arbeidsgiver);
                return Optional.empty();
            }
            if (internReferanse == null) {
                return Optional.empty();
            }
            throw new IllegalStateException("Mangler eksternReferanse for internReferanse:" + internReferanse + ", arbeidsgiver=" + arbeidsgiver);
        }

        private ArbeidsforholdRefDto mapArbeidsforholdsId(no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon arbeidsforholdInformasjon,
                                                          Arbeidsgiver arbeidsgiver,
                                                          InternArbeidsforholdRef ref,
                                                          UUID grunnlagRef,
                                                          boolean aktiv) {
            if (ref != null && ref.getReferanse() != null) {
                EksternArbeidsforholdRef eksternReferanse;
                try {
                    eksternReferanse = arbeidsforholdInformasjon == null ? null : arbeidsforholdInformasjon.finnEksternRaw(arbeidsgiver, ref);
                } catch (IllegalStateException e) {
                    if (e.getMessage().startsWith("Mangler eksternReferanse for internReferanse:")) {
                        // Sukk, må håndtere at det ligger dritt her også ..
                        eksternReferanse = null;
                    } else {
                        throw e;
                    }
                }

                if (eksternReferanse == null || eksternReferanse.getReferanse() == null) {
                    LOG.warn("Grunnlag=[ref='{}',aktiv={}] Mangler eksternReferanse for internReferanse={}", grunnlagRef, aktiv, ref);
                    return null;
                }
                return new ArbeidsforholdRefDto(ref.getReferanse(), eksternReferanse.getReferanse());
            }
            return null;
        }

        private no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.BekreftetPermisjon mapBekreftetPermisjon(Optional<BekreftetPermisjon> entitet) {
            if (entitet.isEmpty()) {
                return null;
            }
            var bekreftetPermisjon = entitet.get();
            if (bekreftetPermisjon.getPeriode() == null) {
                return null;
            }
            var periode = mapPeriode(bekreftetPermisjon.getPeriode());
            var bekreftetPermisjonStatus = KodeverkMapper.mapBekreftetPermisjonStatus(bekreftetPermisjon.getStatus());
            return new no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.BekreftetPermisjon(periode, bekreftetPermisjonStatus);
        }

        private Periode mapPeriode(DatoIntervallEntitet periode) {
            return new Periode(periode.getFomDato(), periode.getTomDato());
        }

        ArbeidsforholdInformasjon map(no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon entitet,
                                      UUID eksternReferanse,
                                      boolean aktiv) {
            if (entitet == null) {
                return null;
            }

            var arbeidsforholdInformasjon = new ArbeidsforholdInformasjon();
            var overstyringer = entitet.getOverstyringer()
                .stream()
                .map(ao -> new ArbeidsforholdOverstyringDto(mapAktør(ao.getArbeidsgiver()),
                    mapArbeidsforholdsId(entitet, ao.getArbeidsgiver(), ao.getArbeidsforholdRef(), eksternReferanse, aktiv)).medBegrunnelse(
                        ao.getBegrunnelse())
                    .medBekreftetPermisjon(mapBekreftetPermisjon(ao.getBekreftetPermisjon()))
                    .medHandling(KodeverkMapper.mapArbeidsforholdHandlingTypeTilDto(ao.getHandling()))
                    .medNavn(ao.getArbeidsgiverNavn())
                    .medStillingsprosent(ao.getStillingsprosent() == null ? null : ao.getStillingsprosent().getVerdi())
                    .medNyArbeidsforholdRef(
                        ao.getNyArbeidsforholdRef() == null ? null : mapArbeidsforholdsId(entitet, ao.getArbeidsgiver(), ao.getNyArbeidsforholdRef(),
                            eksternReferanse, aktiv))
                    .medArbeidsforholdOverstyrtePerioder(map(ao.getArbeidsforholdOverstyrtePerioder())))
                .sorted(COMP_ARBEIDSFORHOLD_OVERSTYRING)
                .toList();

            var referanser = entitet.getArbeidsforholdReferanser()
                .stream()
                .map(this::mapArbeidsforholdReferanse)
                .flatMap(Optional::stream)
                .sorted(COMP_ARBEIDSFORHOLD_REFERANSE)
                .toList();

            return arbeidsforholdInformasjon.medOverstyringer(overstyringer).medReferanser(referanser);
        }

    }
}
