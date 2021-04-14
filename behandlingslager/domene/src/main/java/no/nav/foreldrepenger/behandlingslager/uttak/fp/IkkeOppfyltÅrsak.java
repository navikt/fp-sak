package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakType.UTSETTELSE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakType.UTTAK;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak.MyIkkeOppfyltPeriodeResultatÅrsakSerializer;
import no.nav.vedtak.konfig.Tid;

@JsonDeserialize(using=PeriodeResultatÅrsakDeserializer.class)
@JsonSerialize(using=MyIkkeOppfyltPeriodeResultatÅrsakSerializer.class)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum IkkeOppfyltÅrsak implements PeriodeResultatÅrsak {

    UKJENT("-", "Ikke definert", null, null),
    IKKE_STØNADSDAGER_IGJEN("4002", "§14-9: Ikke stønadsdager igjen på stønadskonto", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK)),
    MOR_HAR_IKKE_OMSORG("4003", "§14-10 fjerde ledd: Mor har ikke omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    HULL_MELLOM_FORELDRENES_PERIODER("4005", "§14-10 sjuende ledd: Ikke sammenhengende perioder", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    _4006("4006", "§14-10 sjuende ledd: Ikke sammenhengende perioder", "", Set.of(UTTAK), LocalDate.of(2001,1,1)),
    DEN_ANDRE_PART_SYK_SKADET_IKKE_OPPFYLT("4007", "§14-12 tredje ledd: Den andre part syk/skadet ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE, FORELDREPENGER)),
    DEN_ANDRE_PART_INNLEGGELSE_IKKE_OPPFYLT("4008", "§14-12 tredje ledd: Den andre part innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE, FORELDREPENGER)),
    FAR_HAR_IKKE_OMSORG("4012", "§14-10 fjerde ledd: Far/medmor har ikke omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    MOR_SØKER_FELLESPERIODE_FØR_12_UKER_FØR_TERMIN_FØDSEL("4013", "§14-10 første ledd: Mor søker uttak før 12 uker før termin/fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), Set.of(FELLESPERIODE, FORELDREPENGER)),
    _4018("4018", "§14-10 andre ledd: Søkt uttak/utsettelse før omsorgsovertakelse", "", Set.of(UTTAK, UTSETTELSE), LocalDate.of(2001,1,1)),
    SØKNADSFRIST("4020", "§22-13 tredje ledd: Brudd på søknadsfrist", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"22-13\"}}}", Set.of(UTTAK, UTSETTELSE)),
    BARN_OVER_3_ÅR("4022", "§14-10 tredje ledd: Barnet er over 3 år", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK, UTSETTELSE)),
    ARBEIDER_I_UTTAKSPERIODEN_MER_ENN_0_PROSENT("4023", "§14-10 femte ledd: Arbeider i uttaksperioden mer enn 0%", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    AVSLAG_GRADERING_ARBEIDER_100_PROSENT_ELLER_MER("4025", "§14-16 første ledd: Avslag gradering - arbeid 100% eller mer", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}", Set.of(UTTAK)),
    UTSETTELSE_FØR_TERMIN_FØDSEL("4030", "§14-9: Avslag utsettelse før termin/fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_INNENFOR_DE_FØRSTE_6_UKENE("4031", "§14-9: Ferie/arbeid innenfor de første 6 ukene", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTSETTELSE)),
    FERIE_SELVSTENDIG_NÆRINGSDRIVENDSE_FRILANSER("4032", "§14-11 første ledd bokstav a: Ferie - selvstendig næringsdrivende/frilanser", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    IKKE_LOVBESTEMT_FERIE("4033", "§14-11 første ledd bokstav a: Ikke lovbestemt ferie", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    INGEN_STØNADSDAGER_IGJEN("4034", "§14-11, jf §14-9: Avslag utsettelse - ingen stønadsdager igjen", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-9\"}}}", Set.of(UTSETTELSE)),
    BARE_FAR_RETT_MOR_FYLLES_IKKE_AKTIVITETSKRAVET("4035", "§14-11 første ledd bokstav b, jf. §14-14: Bare far har rett, mor fyller ikke aktivitetskravet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE)),
    IKKE_HELTIDSARBEID("4037", "§14-11 første ledd bokstav b: Ikke heltidsarbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    SØKERS_SYKDOM_SKADE_IKKE_OPPFYLT("4038", "§14-11 første ledd bokstav c: Søkers sykdom/skade ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    SØKERS_INNLEGGELSE_IKKE_OPPFYLT("4039", "§14-11 første ledd bokstav c: Søkers innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    BARNETS_INNLEGGELSE_IKKE_OPPFYLT("4040", "§14-11 første ledd bokstav d: Barnets innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_FERIE_PÅ_BEVEGELIG_HELLIGDAG("4041", "§14-11 første ledd bokstav a: Avslag utsettelse ferie på bevegelig helligdag", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    AKTIVITETSKRAVET_ARBEID_IKKE_OPPFYLT("4050", "§14-13 første ledd bokstav a: Aktivitetskravet arbeid ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_IKKE_OPPFYLT("4051", "§14-13 første ledd bokstav b: Aktivitetskravet offentlig godkjent utdanning ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_I_KOMBINASJON_MED_ARBEID_IKKE_OPPFYLT("4052", "§14-13 første ledd bokstav c: Aktivitetskravet offentlig godkjent utdanning i kombinasjon med arbeid ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_MORS_SYKDOM_IKKE_OPPFYLT("4053", "§14-13 første ledd bokstav d: Aktivitetskravet mors sykdom/skade ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_MORS_INNLEGGELSE_IKKE_OPPFYLT("4054", "§14-13 første ledd bokstav e: Aktivitetskravet mors innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_INTRODUKSJONSPROGRAM_IKKE_OPPFYLT("4055", "§14-13 første ledd bokstav f: Aktivitetskravet mors deltakelse på introduksjonsprogram ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_KVALIFISERINGSPROGRAM_IKKE_OPPFYLT("4056", "§14-13 første ledd bokstav g: Aktivitetskravet mors deltakelse på kvalifiseringsprogram ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    MORS_MOTTAK_AV_UFØRETRYGD_IKKE_OPPFYLT("4057", "§14-14 tredje ledd: Unntak for aktivitetskravet, mors mottak av uføretrygd ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FORELDREPENGER)),
    STEBARNSADOPSJON_IKKE_NOK_DAGER("4058", "§14-5 tredje ledd: Unntak for Aktivitetskravet, stebarnsadopsjon - ikke nok dager", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    FLERBARNSFØDSEL_IKKE_NOK_DAGER("4059", "§14-13 sjette ledd, jf. §14-9 fjerde ledd: Unntak for Aktivitetskravet, flerbarnsfødsel - ikke nok dager", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13, 14-9\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    SAMTIDIG_UTTAK_IKKE_GYLDIG_KOMBINASJON("4060", "§14-10 sjette ledd: Samtidig uttak - ikke gyldig kombinasjon", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    UTSETTELSE_FERIE_IKKE_DOKUMENTERT("4061", "§14-11 første ledd bokstav a, jf §21-3: Utsettelse ferie ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_ARBEID_IKKE_DOKUMENTERT("4062", "§14-11 første ledd bokstav b, jf §21-3: Utsettelse arbeid ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_SØKERS_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT("4063", "§14-11 første ledd bokstav c og tredje ledd, jf §21-3: Utsettelse søkers sykdom/skade ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT("4064", "§14-11 første ledd bokstav c og tredje ledd, jf §21-3: Utsettelse søkers innleggelse ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_BARNETS_INNLEGGELSE_IKKE_DOKUMENTERT("4065", "§14-11 første ledd bokstav d, jf §21-3: Utsettelse barnets innleggelse - ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE)),
    AKTIVITETSKRAVET_ARBEID_IKKE_DOKUMENTERT("4066", "§14-13 første ledd bokstav a, jf §21-3: Aktivitetskrav - arbeid ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_UTDANNING_IKKE_DOKUMENTERT("4067", "§14-13 første ledd bokstav b, jf §21-3: Aktivitetskrav – utdanning ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_ARBEID_I_KOMB_UTDANNING_IKKE_DOKUMENTERT("4068", "§14-13 første ledd bokstav c, jf §21-3: Aktivitetskrav – arbeid i komb utdanning ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT("4069", "§14-13 første ledd bokstav d og femte ledd, jf §21-3: Aktivitetskrav – sykdom/skade ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_INNLEGGELSE_IKKE_DOKUMENTERT("4070", "§14-13 første ledd bokstav e og femte ledd, jf §21-3: Aktivitetskrav – innleggelse ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE)),
    SØKER_ER_DØD("4071", "§14-10: Bruker er død", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK, UTSETTELSE)),
    BARNET_ER_DØD("4072", "§14-9 sjuende ledd: Barnet er dødt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK, UTSETTELSE)),
    MOR_IKKE_RETT_TIL_FORELDREPENGER("4073", "§14-12 første ledd: Ikke rett til kvote fordi mor ikke har rett til foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE, FORELDREPENGER)),
    SYKDOM_SKADE_INNLEGGELSE_IKKE_DOKUMENTERT("4074", "§14-12 tredje ledd, jf §21-3: Avslag overføring kvote pga. sykdom/skade/innleggelse ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12,21-3\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    FAR_IKKE_RETT_PÅ_FELLESPERIODE_FORDI_MOR_IKKE_RETT("4075", "§14-9 første ledd: Ikke rett til fellesperiode fordi mor ikke har rett til foreldrepenger", "", Set.of(UTTAK), Set.of(FELLESPERIODE)),
    ANNEN_FORELDER_HAR_RETT("4076", "§14-9 femte ledd: Avslag overføring - annen forelder har rett til foreldrepenger", "", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    FRATREKK_PLEIEPENGER("4077", "§14-10 a: Innvilget prematuruker, med fratrekk pleiepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10 a\"}}}", Set.of(UTSETTELSE)),
    AVSLAG_GRADERING_PÅ_GRUNN_AV_FOR_SEN_SØKNAD("4080", "§14-16: Ikke gradering pga. for sen søknad", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}", Set.of(UTTAK), LocalDate.of(2001,1,1)),
    AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID("4081", "§14-11 første ledd bokstav a: Avslag utsettelse pga ferie tilbake i tid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID("4082", "§14-11 første ledd bokstav b: Avslag utsettelse pga arbeid tilbake i tid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE)),
    DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK("4084", "§14-10 sjette ledd: Annen part har overlappende uttak, det er ikke søkt/innvilget samtidig uttak", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK, UTSETTELSE)),
    IKKE_SAMTYKKE_MELLOM_PARTENE("4085", "§14-10 sjette ledd: Det er ikke samtykke mellom partene", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE("4086", "§14-10 sjette ledd og §14-11: Annen part har overlappende uttaksperioder som er innvilget utsettelse", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10,14-11\"}}}", Set.of(UTTAK, UTSETTELSE)),
    OPPHØR_MEDLEMSKAP("4087", "§14-2: Opphør medlemskap", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-2\"}}}", Set.of(UTTAK)),
    AKTIVITETSKRAVET_INTROPROGRAM_IKKE_DOKUMENTERT("4088", "§14-13 første ledd bokstav f, jf §21-3: Aktivitetskrav – introprogram ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_KVP_IKKE_DOKUMENTERT("4089", "§14-13 første ledd bokstav g, jf §21-3: Aktivitetskrav – KVP ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    HULL_MELLOM_SØKNADSPERIODE_ETTER_SISTE_UTTAK("4090", "§14-10 sjuende ledd: Ikke sammenhengende perioder", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), LocalDate.of(2001,1,1)),
    HULL_MELLOM_SØKNADSPERIOD_ETTER_SISTE_UTSETTELSE("4091", "§14-10 sjuende ledd: Ikke sammenhengende perioder", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), LocalDate.of(2001,1,1)),
    HAR_IKKE_ALENEOMSORG_FOR_BARNET("4092", "§14-12: Avslag overføring - har ikke aleneomsorg for barnet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    AVSLAG_GRADERING_SØKER_ER_IKKE_I_ARBEID("4093", "§14-16: Avslag gradering - søker er ikke i arbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}", Set.of(UTTAK)),
    AVSLAG_GRADERINGSAVTALE_MANGLER_IKKE_DOKUMENTERT("4094", "§14-16 femte ledd, jf §21-3: Avslag graderingsavtale mangler - ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16,21-3\"}}}", Set.of(UTTAK), LocalDate.of(2001,1,1)),
    MOR_TAR_IKKE_ALLE_UKENE("4095", "§14-10 første ledd: Mor tar ikke alle 3 ukene før termin", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER_FØR_FØDSEL)),
    FØDSELSVILKÅRET_IKKE_OPPFYLT("4096", "§14-5: Fødselsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK)),
    ADOPSJONSVILKÅRET_IKKE_OPPFYLT("4097", "§14-5: Adopsjonsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK)),
    FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT("4098", "§14-5: Foreldreansvarsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK)),
    OPPTJENINGSVILKÅRET_IKKE_OPPFYLT("4099", "§14-6: Opptjeningsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-6\"}}}", Set.of(UTTAK)),
    UTTAK_FØR_OMSORGSOVERTAKELSE("4100", "§14-10 andre ledd: Uttak før omsorgsovertakelse", "", Set.of(UTTAK)),
    ;

    private static final Map<String, IkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "IKKE_OPPFYLT_AARSAK";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;
    @JsonIgnore
    private String lovHjemmel;

    @JsonIgnore
    private LocalDate gyldigFom;
    @JsonIgnore
    private LocalDate gyldigTom;

    private Set<UttakType> uttakTyper;

    private Set<StønadskontoType> valgbarForKonto;

    IkkeOppfyltÅrsak(String kode,
                     String navn,
                     String lovHjemmel,
                     Set<UttakType> uttakTyper,
                     Set<StønadskontoType> valgbarForKonto,
                     LocalDate gyldigFom,
                     LocalDate gyldigTom) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
        this.gyldigFom = gyldigFom == null ? LocalDate.of(2000, 1, 1) : gyldigFom;
        this.gyldigTom = gyldigTom == null ? Tid.TIDENES_ENDE : gyldigTom;
        this.uttakTyper = uttakTyper == null ? Set.of(UTTAK) : uttakTyper;
        this.valgbarForKonto = valgbarForKonto == null ?
            Set.of(FELLESPERIODE, MØDREKVOTE, FEDREKVOTE, FORELDREPENGER, FORELDREPENGER_FØR_FØDSEL)
            : valgbarForKonto;
    }

    IkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel, Set<UttakType> uttakTyper, Set<StønadskontoType> valgbarForKonto) {
        this(kode, navn, lovHjemmel, uttakTyper, valgbarForKonto, null, null);
    }

    IkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel, Set<UttakType> uttakTyper) {
        this(kode, navn, lovHjemmel, uttakTyper, null, null, null);
    }

    IkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel, Set<UttakType> uttakTyper, LocalDate gyldigTom) {
        this(kode, navn, lovHjemmel, uttakTyper, null, null, gyldigTom);
    }

    @Override
    public Set<UttakType> getUttakTyper() {
        return uttakTyper;
    }

    @Override
    public Set<StønadskontoType> getValgbarForKonto() {
        return valgbarForKonto;
    }

    @JsonProperty("gyldigFom")
    @Override
    public LocalDate getGyldigFraOgMed() {
        return gyldigFom;
    }

    @JsonProperty("gyldigTom")
    @Override
    public LocalDate getGyldigTilOgMed() {
        return gyldigTom;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static IkkeOppfyltÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        var kode = TempAvledeKode.getVerdi(IkkeOppfyltÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent IkkeOppfyltÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, IkkeOppfyltÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    /** Returnerer p.t. Raw json. */
    @Override
    public String getLovHjemmelData() {
        return lovHjemmel;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<IkkeOppfyltÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(IkkeOppfyltÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public IkkeOppfyltÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker() {
        return new HashSet<>(Arrays.asList(
            MOR_HAR_IKKE_OMSORG,
            FAR_HAR_IKKE_OMSORG,
            BARNET_ER_DØD,
            SØKER_ER_DØD,
            OPPHØR_MEDLEMSKAP,
            FØDSELSVILKÅRET_IKKE_OPPFYLT,
            ADOPSJONSVILKÅRET_IKKE_OPPFYLT,
            FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT,
            OPPTJENINGSVILKÅRET_IKKE_OPPFYLT));
    }

    public static Set<PeriodeResultatÅrsak> årsakerTilAvslagPgaAnnenpart() {
        return new HashSet<>(Arrays.asList(
            DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK,
            DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE));
    }

    public static class MyIkkeOppfyltPeriodeResultatÅrsakSerializer extends PeriodeResultatÅrsakSerializer<IkkeOppfyltÅrsak> {
        public MyIkkeOppfyltPeriodeResultatÅrsakSerializer() {
            super(IkkeOppfyltÅrsak.class);
        }
    }
}
