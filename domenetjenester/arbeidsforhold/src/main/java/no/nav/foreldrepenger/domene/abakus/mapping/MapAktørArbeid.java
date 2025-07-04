package no.nav.foreldrepenger.domene.abakus.mapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.PersonIdent;
import no.nav.abakus.iaygrunnlag.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.abakus.iaygrunnlag.arbeid.v1.ArbeidDto;
import no.nav.abakus.iaygrunnlag.arbeid.v1.PermisjonDto;
import no.nav.abakus.iaygrunnlag.arbeid.v1.YrkesaktivitetDto;
import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapAktørArbeid {

    private static final Comparator<YrkesaktivitetDto> COMP_YRKESAKTIVITET = Comparator
            .comparing((YrkesaktivitetDto dto) -> dto.getArbeidsgiver().map(Aktør::getIdent).orElse(null),
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getArbeidsforholdId() == null ? null : dto.getArbeidsforholdId().getAbakusReferanse(),
                    Comparator.nullsFirst(Comparator.naturalOrder()));

    private static final Comparator<AktivitetsAvtaleDto> COMP_AKTIVITETSAVTALE = Comparator
            .comparing((AktivitetsAvtaleDto dto) -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<PermisjonDto> COMP_PERMISJON = Comparator
            .comparing((PermisjonDto dto) -> dto.getPeriode().getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getPeriode().getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private MapAktørArbeid() {
        // skjul public constructor
    }

    public static class MapFraDto {

        private InntektArbeidYtelseAggregatBuilder registerData;

        MapFraDto(InntektArbeidYtelseAggregatBuilder registerData) {
            this.registerData = registerData;
        }

        List<AktørArbeidBuilder> map(Collection<ArbeidDto> dtos) {
            if (dtos == null || dtos.isEmpty()) {
                return Collections.emptyList();
            }
            return dtos.stream().map(this::mapAktørArbeid).toList();
        }

        private AktørArbeidBuilder mapAktørArbeid(ArbeidDto dto) {
            var builder = registerData.getAktørArbeidBuilder(tilAktørId(dto.getPerson()));
            dto.getYrkesaktiviteter().forEach(yrkesaktivitetDto -> builder.leggTilYrkesaktivitet(mapYrkesaktivitet(yrkesaktivitetDto)));
            return builder;
        }

        /**
         * Returnerer person sin aktørId. Denne trenger ikke være samme som søkers
         * aktørid men kan f.eks. være annen part i en sak.
         */
        private AktørId tilAktørId(PersonIdent person) {
            if (!(person instanceof AktørIdPersonident)) {
                throw new IllegalArgumentException("Støtter kun " + AktørIdPersonident.class.getSimpleName() + " her");
            }
            return new AktørId(person.getIdent());
        }

        private YrkesaktivitetBuilder mapYrkesaktivitet(YrkesaktivitetDto dto) {
            var arbeidsgiver = dto.getArbeidsgiver().map(this::mapArbeidsgiver).orElse(null);
            var internArbeidsforholdRef = arbeidsgiver == null ? null : mapArbeidsforholdRef(arbeidsgiver, dto.getArbeidsforholdId());

            var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                    .medArbeidsforholdId(internArbeidsforholdRef)
                    .medArbeidsgiver(arbeidsgiver)
                    .medArbeidsgiverNavn(dto.getNavnArbeidsgiverUtland())
                    .medArbeidType(KodeverkMapper.mapArbeidType(dto.getType()));

            dto.getAktivitetsAvtaler()
                    .forEach(aktivitetsAvtaleDto -> yrkesaktivitetBuilder.leggTilAktivitetsAvtale(mapAktivitetsAvtale(aktivitetsAvtaleDto)));

            dto.getPermisjoner()
                    .forEach(permisjonDto -> yrkesaktivitetBuilder
                            .leggTilPermisjon(mapPermisjon(permisjonDto, yrkesaktivitetBuilder.getPermisjonBuilder())));

            return yrkesaktivitetBuilder;
        }

        private Permisjon mapPermisjon(PermisjonDto dto, PermisjonBuilder permisjonBuilder) {
            return permisjonBuilder
                    .medPeriode(dto.getPeriode().getFom(), dto.getPeriode().getTom())
                    .medPermisjonsbeskrivelseType(KodeverkMapper.mapPermisjonbeskrivelseTypeFraDto(dto.getType()))
                    .medProsentsats(dto.getProsentsats())
                    .build();
        }

        private AktivitetsAvtaleBuilder mapAktivitetsAvtale(AktivitetsAvtaleDto dto) {
            return AktivitetsAvtaleBuilder.ny()
                    .medBeskrivelse(dto.getBeskrivelse())
                    .medPeriode(mapPeriode(dto.getPeriode()))
                    .medProsentsats(dto.getStillingsprosent())
                    .medSisteLønnsendringsdato(dto.getSistLønnsendring());
        }

        private DatoIntervallEntitet mapPeriode(Periode periode) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
        }

        private InternArbeidsforholdRef mapArbeidsforholdRef(Arbeidsgiver arbeidsgiver, ArbeidsforholdRefDto arbeidsforholdId) {
            if (arbeidsforholdId == null) {
                return InternArbeidsforholdRef.nullRef();
            }
            var internRef = InternArbeidsforholdRef.ref(arbeidsforholdId.getAbakusReferanse());
            var eksternRef = EksternArbeidsforholdRef.ref(arbeidsforholdId.getEksternReferanse());
            registerData.medNyInternArbeidsforholdRef(arbeidsgiver, internRef, eksternRef);

            return internRef;
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
            if (arbeidsgiver.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(arbeidsgiver.getIdent()));
            }
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }
    }

    public static class MapTilDto {

        private static final Logger LOG = LoggerFactory.getLogger(MapTilDto.class);

        private ArbeidsforholdInformasjon arbeidsforholdInformasjon;

        public MapTilDto(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
            this.arbeidsforholdInformasjon = arbeidsforholdInformasjon;
        }

        private static BigDecimal minMax(BigDecimal val, BigDecimal min, BigDecimal max) {
            if (val == null) {
                return val;
            }
            if (min != null && val.compareTo(min) < 0) {
                return min;
            }
            if (max != null && val.compareTo(max) > 0) {
                return max;
            }
            return val;
        }

        List<ArbeidDto> map(Collection<AktørArbeid> aktørArbeid) {
            if (aktørArbeid == null || aktørArbeid.isEmpty()) {
                return Collections.emptyList();
            }
            return aktørArbeid.stream().map(this::map).toList();
        }

        private ArbeidDto map(AktørArbeid arb) {
            List<YrkesaktivitetDto> yrkesaktiviteter = new ArrayList<>(getYrkesaktiviteter(arb.hentAlleYrkesaktiviteter()));

            var aktiviteter = yrkesaktiviteter.stream().filter(this::erGyldigYrkesaktivitet).sorted(COMP_YRKESAKTIVITET).toList();

            return new ArbeidDto(new AktørIdPersonident(arb.getAktørId().getId()))
                    .medYrkesaktiviteter(aktiviteter);
        }

        private boolean erGyldigYrkesaktivitet(YrkesaktivitetDto yrkesaktivitet) {
            return !yrkesaktivitet.getAktivitetsAvtaler().isEmpty() || !yrkesaktivitet.getPermisjoner().isEmpty();
        }

        private List<YrkesaktivitetDto> getYrkesaktiviteter(Collection<Yrkesaktivitet> aktiviteter) {
            return aktiviteter.stream().map(this::mapYrkesaktivitet).toList();
        }

        private AktivitetsAvtaleDto map(AktivitetsAvtale aa) {
            var fomDato = aa.getPeriodeUtenOverstyring().getFomDato();
            var tomDato = aa.getPeriodeUtenOverstyring().getTomDato();
            return new AktivitetsAvtaleDto(fomDato, tomDato)
                    .medBeskrivelse(aa.getBeskrivelse())
                    .medSistLønnsendring(aa.getSisteLønnsendringsdato())
                    .medStillingsprosent(aa.getProsentsats() == null ? null : aa.getProsentsats().getVerdi());
        }

        private PermisjonDto map(Permisjon p) {
            var permisjonsbeskrivelseType = KodeverkMapper.mapPermisjonbeskrivelseTypeTilDto(p.getPermisjonsbeskrivelseType());
            var maxPermisjonProsentsats = new BigDecimal(100); // enig med Cecilie H. om å transformere dårlige data (eks. 800% permisjon).
                                                                      // Bare første 100% som gir utslag.

            return new PermisjonDto(new Periode(p.getFraOgMed(), p.getTilOgMed()), permisjonsbeskrivelseType)
                    .medProsentsats(
                            minMax(p.getProsentsats() != null ? p.getProsentsats().getVerdi() : null, BigDecimal.ZERO, maxPermisjonProsentsats));
        }

        private YrkesaktivitetDto mapYrkesaktivitet(Yrkesaktivitet a) {
            var aktivitetsAvtaler = a.getAlleAktivitetsAvtaler().stream().map(this::map).sorted(COMP_AKTIVITETSAVTALE).toList();
            var permisjoner = a.getPermisjon().stream().map(this::map).sorted(COMP_PERMISJON).toList();

            var arbeidsforholdId = mapArbeidsforholdsId(a.getArbeidsgiver(), a);

            var arbeidType = KodeverkMapper.mapArbeidTypeTilDto(a.getArbeidType());

            return new YrkesaktivitetDto(arbeidType)
                    .medArbeidsgiver(mapAktør(a.getArbeidsgiver()))
                    .medAktivitetsAvtaler(aktivitetsAvtaler)
                    .medPermisjoner(permisjoner)
                    .medArbeidsforholdId(arbeidsforholdId)
                    .medNavnArbeidsgiverUtland(a.getNavnArbeidsgiverUtland());
        }

        private ArbeidsforholdRefDto mapArbeidsforholdsId(Arbeidsgiver arbeidsgiver, Yrkesaktivitet yrkesaktivitet) {
            var internRef = yrkesaktivitet.getArbeidsforholdRef();
            if (internRef == null || internRef.getReferanse() == null) {
                return null;
            }
            EksternArbeidsforholdRef eksternRef;
            try {
                eksternRef = arbeidsforholdInformasjon == null ? null : arbeidsforholdInformasjon.finnEksternRaw(arbeidsgiver, internRef);
            } catch (IllegalStateException e) {
                if (e.getMessage().startsWith("Mangler eksternReferanse for internReferanse:")) {
                    // Sukk, må håndtere at det ligger dritt her også ..
                    eksternRef = null;
                } else {
                    throw e;
                }
            }

            if (eksternRef == null || eksternRef.getReferanse() == null) {
                LOG.warn("Mangler eksternReferanse for internReferanse={}, forkaster internReferanse. Antar feilmapping", internRef);
                return null;
            }

            return new ArbeidsforholdRefDto(internRef.getReferanse(), eksternRef.getReferanse(),
                    Fagsystem.AAREGISTERET);
        }

        private Aktør mapAktør(Arbeidsgiver arbeidsgiver) {
            if (arbeidsgiver == null) {
                return null; // arbeidType='NÆRING' har null arbeidsgiver
            }
            return arbeidsgiver.erAktørId()
                    ? new AktørIdPersonident(arbeidsgiver.getAktørId().getId())
                    : new Organisasjon(arbeidsgiver.getOrgnr());
        }

    }
}
