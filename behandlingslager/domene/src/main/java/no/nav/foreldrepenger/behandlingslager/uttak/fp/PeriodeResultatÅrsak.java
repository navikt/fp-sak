package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import static java.util.Set.of;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.LovEndring.FRITT_UTTAK;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.LovEndring.KREVER_SAMMENHENGENDE_UTTAK;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType.MØDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakType.UTSETTELSE;
import static no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakType.UTTAK;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.ÅrsakskodeMedLovreferanse;

public enum PeriodeResultatÅrsak implements Kodeverdi, ÅrsakskodeMedLovreferanse {

    UKJENT("-", "Ikke definert", null, null),

    // Regel oppfylt, resultat = innvilget
    FELLESPERIODE_ELLER_FORELDREPENGER("2002", "§14-9: Innvilget fellesperiode/foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", of(UTTAK), of(FELLESPERIODE, FORELDREPENGER)),
    KVOTE_ELLER_OVERFØRT_KVOTE("2003", "§14-12: Innvilget uttak av kvote", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", of(UTTAK), of(MØDREKVOTE, FEDREKVOTE)),
    FORELDREPENGER_KUN_FAR_HAR_RETT("2004", "§14-14, jf. §14-13 : Innvilget foreldrepenger, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-13\"}}}", of(UTTAK), of(FORELDREPENGER)),
    FORELDREPENGER_ALENEOMSORG("2005", "§14-15: Innvilget foreldrepenger ved aleneomsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-15\"}}}", of(UTTAK), of(FORELDREPENGER)),
    INNVILGET_FORELDREPENGER_FØR_FØDSEL("2006", "§14-10: Innvilget foreldrepenger før fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", of(UTTAK), of(FORELDREPENGER_FØR_FØDSEL)),
    FORELDREPENGER_KUN_MOR_HAR_RETT("2007", "§14-10: Innvilget foreldrepenger, kun mor har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", of(UTTAK), of(FORELDREPENGER)),
    UTSETTELSE_GYLDIG_PGA_FERIE("2010", "§14-11 første ledd bokstav a: Gyldig utsettelse pga. ferie", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID("2011", "§14-11 første ledd bokstav b: Gyldig utsettelse pga. 100% arbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_INNLEGGELSE("2012", "§14-11 første ledd bokstav c: Gyldig utsettelse pga. innleggelse", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT("2013", "§14-11 første ledd bokstav d: Gyldig utsettelse pga. barn innlagt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_SYKDOM("2014", "§14-11 første ledd bokstav c: Gyldig utsettelse pga. sykdom", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_FERIE_KUN_FAR_HAR_RETT("2015", "§14-11 første ledd bokstav a, jf. §14-14, jf. §14-13: Utsettelse pga. ferie, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_ARBEID_KUN_FAR_HAR_RETT("2016", "§14-11 første ledd bokstav b, jf. §14-14, jf. §14-13: Utsettelse pga. 100% arbeid, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_SYKDOM_KUN_FAR_HAR_RETT("2017", "§14-11 første ledd bokstav c, jf. §14-14, jf. §14-13: Utsettelse pga. sykdom, skade, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_INNLEGGELSE_KUN_FAR_HAR_RETT("2018", "§14-11 første ledd bokstav c, jf. §14-14, jf. §14-13: Utsettelse pga. egen innleggelse på helseinstiusjon, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT_KUN_FAR_HAR_RETT("2019", "§14-11 første ledd bokstav d, jf. §14-14, jf. §14-13: Utsettelse pga. barnets innleggelse på helseinstitusjon, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", of(UTSETTELSE), null, of(KREVER_SAMMENHENGENDE_UTTAK)),
    OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER("2020", "§14-9 første ledd: Overføring oppfylt, annen part har ikke rett til foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", of(UTTAK), of(MØDREKVOTE, FEDREKVOTE)),
    OVERFØRING_ANNEN_PART_SYKDOM_SKADE("2021", "§14-12: Overføring oppfylt, annen part er helt avhengig av hjelp til å ta seg av barnet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", of(UTTAK), of(MØDREKVOTE, FEDREKVOTE)),
    OVERFØRING_ANNEN_PART_INNLAGT("2022", "§14-12: Overføring oppfylt, annen part er innlagt i helseinstitusjon", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", of(UTTAK), of(MØDREKVOTE, FEDREKVOTE)),
    OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET("2023", "§14-15 første ledd: Overføring oppfylt, søker har aleneomsorg for barnet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-15\"}}}", of(UTTAK), of(MØDREKVOTE, FEDREKVOTE)),
    //TODO fritt uttak. Se til at riktig navn på ny årsak. Avvent om trengs koder for BFHR utsettelse pga mor syk/innlagt - bør vel søke uttak eller FRI med dokumentasjon
    UTSETTELSE_GYLDIG("2024", "§14-11: Gyldig utsettelse", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), null, of(FRITT_UTTAK)),
    UTSETTELSE_GYLDIG_SEKS_UKER_INNLEGGELSE("2025", "§14-11: Gyldig utsettelse første 6 uker pga. innleggelse", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), of(MØDREKVOTE, FORELDREPENGER), of(FRITT_UTTAK)),
    UTSETTELSE_GYLDIG_SEKS_UKER_FRI_BARN_INNLAGT("2026", "§14-11: Gyldig utsettelse første 6 uker pga. barn innlagt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), of(MØDREKVOTE, FORELDREPENGER), of(FRITT_UTTAK)),
    UTSETTELSE_GYLDIG_SEKS_UKER_FRI_SYKDOM("2027", "§14-11: Gyldig utsettelse første 6 uker pga. sykdom", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", of(UTSETTELSE), of(MØDREKVOTE, FORELDREPENGER), of(FRITT_UTTAK)),
    UTSETTELSE_GYLDIG_BFR_AKT_KRAV_OPPFYLT("2028", "§14-14, jf. 14-13: Bare far rett, aktivitetskravet oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-13\"}}}", of(UTSETTELSE), of(FORELDREPENGER), of(FRITT_UTTAK)),
    GRADERING_FELLESPERIODE_ELLER_FORELDREPENGER("2030", "§14-9, jf. §14-16: Gradering av fellesperiode/foreldrepenger", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9,14-16\"}}}", of(UTTAK), of(FELLESPERIODE, FORELDREPENGER)),
    GRADERING_KVOTE_ELLER_OVERFØRT_KVOTE("2031", "§14-12, jf. §14-16: Gradering av kvote/overført kvote", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12,14-16\"}}}", of(UTTAK), of(MØDREKVOTE, FEDREKVOTE)),
    GRADERING_ALENEOMSORG("2032", "§14-15, jf. §14-16: Gradering foreldrepenger ved aleneomsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-15,14-16\"}}}", of(UTTAK), of(FORELDREPENGER)),
    GRADERING_FORELDREPENGER_KUN_FAR_HAR_RETT("2033", "§14-14, jf. §14-13, jf. §14-16: Gradering foreldrepenger, kun far har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-13,14-16\"}}}", of(UTTAK), of(FORELDREPENGER)),
    GRADERING_FORELDREPENGER_KUN_MOR_HAR_RETT("2034", "§14-10, jf. §14-16: Gradering foreldrepenger, kun mor har rett", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10,14-16\"}}}", of(UTTAK), of(FORELDREPENGER)),
    GRADERING_KUN_FAR_HAR_RETT_MOR_UFØR("2035", "§14-14 tredje ledd, jf. §14-16: Gradering foreldrepenger, kun far har rett og mor er ufør", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-16\"}}}", of(UTTAK), of(FORELDREPENGER)),
    FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR("2036", "§14-14 tredje ledd: Innvilget foreldrepenger, kun far har rett og mor er ufør", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14\"}}}", of(UTTAK), of(FORELDREPENGER)),
    FORELDREPENGER_FELLESPERIODE_TIL_FAR("2037", "§14-9, jf. §14-13: Innvilget fellesperiode til far", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", of(UTTAK), of(FELLESPERIODE)),
    FORELDREPENGER_REDUSERT_GRAD_PGA_SAMTIDIG_UTTAK("2038", "§14-10 sjette ledd: Samtidig uttak", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", of(UTTAK)),

    // Regel ikke oppfylt, resultat = avslått
    IKKE_STØNADSDAGER_IGJEN("4002", "§14-9: Ikke stønadsdager igjen på stønadskonto", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK)),
    MOR_HAR_IKKE_OMSORG("4003", "§14-10 fjerde ledd: Mor har ikke omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    HULL_MELLOM_FORELDRENES_PERIODER("4005", "§14-10 sjuende ledd: Ikke sammenhengende perioder", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    DEN_ANDRE_PART_SYK_SKADET_IKKE_OPPFYLT("4007", "§14-12 tredje ledd: Den andre part syk/skadet ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE, FORELDREPENGER)),
    DEN_ANDRE_PART_INNLEGGELSE_IKKE_OPPFYLT("4008", "§14-12 tredje ledd: Den andre part innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE, FORELDREPENGER)),
    FAR_HAR_IKKE_OMSORG("4012", "§14-10 fjerde ledd: Far/medmor har ikke omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    MOR_SØKER_FELLESPERIODE_FØR_12_UKER_FØR_TERMIN_FØDSEL("4013", "§14-10 første ledd: Mor søker uttak før 12 uker før termin/fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), Set.of(FELLESPERIODE, FORELDREPENGER)),
    SØKNADSFRIST("4020", "§22-13 tredje ledd: Brudd på søknadsfrist", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"22-13\"}}}", Set.of(UTTAK, UTSETTELSE)),
    BARN_OVER_3_ÅR("4022", "§14-10 tredje ledd: Barnet er over 3 år", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK, UTSETTELSE)),
    ARBEIDER_I_UTTAKSPERIODEN_MER_ENN_0_PROSENT("4023", "§14-10 femte ledd: Arbeider i uttaksperioden mer enn 0%", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    AVSLAG_GRADERING_ARBEIDER_100_PROSENT_ELLER_MER("4025", "§14-16 første ledd: Avslag gradering - arbeid 100% eller mer", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}", Set.of(UTTAK)),
    UTSETTELSE_FØR_TERMIN_FØDSEL("4030", "§14-9: Avslag utsettelse før termin/fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTSETTELSE)),
    UTSETTELSE_INNENFOR_DE_FØRSTE_6_UKENE("4031", "§14-9: Ferie/arbeid innenfor de første 6 ukene", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTSETTELSE)),
    FERIE_SELVSTENDIG_NÆRINGSDRIVENDSE_FRILANSER("4032", "§14-11 første ledd bokstav a: Ferie - selvstendig næringsdrivende/frilanser", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    IKKE_LOVBESTEMT_FERIE("4033", "§14-11 første ledd bokstav a: Ikke lovbestemt ferie", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    INGEN_STØNADSDAGER_IGJEN("4034", "§14-11, jf §14-9: Avslag utsettelse - ingen stønadsdager igjen", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-9\"}}}", Set.of(UTSETTELSE)),
    BARE_FAR_RETT_MOR_FYLLES_IKKE_AKTIVITETSKRAVET("4035", "§14-11 første ledd bokstav b, jf. §14-14: Bare far har rett, mor fyller ikke aktivitetskravet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    IKKE_HELTIDSARBEID("4037", "§14-11 første ledd bokstav b: Ikke heltidsarbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    SØKERS_SYKDOM_SKADE_IKKE_OPPFYLT("4038", "§14-11 første ledd bokstav c: Søkers sykdom/skade ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    SØKERS_INNLEGGELSE_IKKE_OPPFYLT("4039", "§14-11 første ledd bokstav c: Søkers innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    BARNETS_INNLEGGELSE_IKKE_OPPFYLT("4040", "§14-11 første ledd bokstav d: Barnets innleggelse ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_FERIE_PÅ_BEVEGELIG_HELLIGDAG("4041", "§14-11 første ledd bokstav a: Avslag utsettelse ferie på bevegelig helligdag", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
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
    UTSETTELSE_FERIE_IKKE_DOKUMENTERT("4061", "§14-11 første ledd bokstav a, jf §21-3: Utsettelse ferie ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_ARBEID_IKKE_DOKUMENTERT("4062", "§14-11 første ledd bokstav b, jf §21-3: Utsettelse arbeid ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_SØKERS_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT("4063", "§14-11 første ledd bokstav c og tredje ledd, jf §21-3: Utsettelse søkers sykdom/skade ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT("4064", "§14-11 første ledd bokstav c og tredje ledd, jf §21-3: Utsettelse søkers innleggelse ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    UTSETTELSE_BARNETS_INNLEGGELSE_IKKE_DOKUMENTERT("4065", "§14-11 første ledd bokstav d, jf §21-3: Utsettelse barnets innleggelse - ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
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
    AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID("4081", "§14-11 første ledd bokstav a: Avslag utsettelse pga ferie tilbake i tid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID("4082", "§14-11 første ledd bokstav b: Avslag utsettelse pga arbeid tilbake i tid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), null, Set.of(KREVER_SAMMENHENGENDE_UTTAK)),
    DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK("4084", "§14-10 sjette ledd: Annen part har overlappende uttak, det er ikke søkt/innvilget samtidig uttak", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK, UTSETTELSE)),
    IKKE_SAMTYKKE_MELLOM_PARTENE("4085", "§14-10 sjette ledd: Det er ikke samtykke mellom partene", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK)),
    DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE("4086", "§14-10 sjette ledd og §14-11: Annen part har overlappende uttaksperioder som er innvilget utsettelse", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10,14-11\"}}}", Set.of(UTTAK, UTSETTELSE)),
    OPPHØR_MEDLEMSKAP("4087", "§14-2: Opphør medlemskap", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-2\"}}}", Set.of(UTTAK)),
    AKTIVITETSKRAVET_INTROPROGRAM_IKKE_DOKUMENTERT("4088", "§14-13 første ledd bokstav f, jf §21-3: Aktivitetskrav – introprogram ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    AKTIVITETSKRAVET_KVP_IKKE_DOKUMENTERT("4089", "§14-13 første ledd bokstav g, jf §21-3: Aktivitetskrav – KVP ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}", Set.of(UTTAK, UTSETTELSE), Set.of(FELLESPERIODE, FORELDREPENGER)),
    HAR_IKKE_ALENEOMSORG_FOR_BARNET("4092", "§14-12: Avslag overføring - har ikke aleneomsorg for barnet", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FEDREKVOTE)),
    AVSLAG_GRADERING_SØKER_ER_IKKE_I_ARBEID("4093", "§14-16: Avslag gradering - søker er ikke i arbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}", Set.of(UTTAK)),
    MOR_TAR_IKKE_ALLE_UKENE("4095", "§14-10 første ledd: Mor tar ikke alle 3 ukene før termin", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER_FØR_FØDSEL)),
    FØDSELSVILKÅRET_IKKE_OPPFYLT("4096", "§14-5: Fødselsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK)),
    ADOPSJONSVILKÅRET_IKKE_OPPFYLT("4097", "§14-5: Adopsjonsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK)),
    FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT("4098", "§14-5: Foreldreansvarsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}", Set.of(UTTAK)),
    OPPTJENINGSVILKÅRET_IKKE_OPPFYLT("4099", "§14-6: Opptjeningsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-6\"}}}", Set.of(UTTAK)),
    UTTAK_FØR_OMSORGSOVERTAKELSE("4100", "§14-10 andre ledd: Uttak før omsorgsovertakelse", "", Set.of(UTTAK)),
    BARE_FAR_RETT_IKKE_SØKT("4102", "§14-14, jf 14-13: Bare far har rett, mangler søknad uttak/aktivitetskrav", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14,14-13\"}}}", Set.of(UTTAK), Set.of(FORELDREPENGER), Set.of(FRITT_UTTAK)),
    MOR_FØRSTE_SEKS_UKER_IKKE_SØKT("4103", "§14-9 sjette ledd: Mangler søknad for første 6 uker etter fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    STØNADSPERIODE_NYTT_BARN("4104", "§14-10 tredje ledd: Stønadsperiode for nytt barn", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", Set.of(UTTAK, UTSETTELSE)),
    FAR_SØKT_FØR_FØDSEL("4105", "§14-9 sjette ledd: Far/medmor søker uttak før fødsel/omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}", Set.of(UTTAK)),
    SØKERS_SYKDOM_SKADE_SEKS_UKER_IKKE_OPPFYLT("4110", "§14-11: Søkers sykdom/skade første 6 uker ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    SØKERS_INNLEGGELSE_SEKS_UKER_IKKE_OPPFYLT("4111", "§14-11: Søkers innleggelse første 6 uker ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    BARNETS_INNLEGGELSE_SEKS_UKER_IKKE_OPPFYLT("4112", "§14-11: Barnets innleggelse første 6 uker ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}", Set.of(UTSETTELSE), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    SØKERS_SYKDOM_ELLER_SKADE_SEKS_UKER_IKKE_DOKUMENTERT("4115", "§14-11, jf §21-3: Søkers sykdom/skade første 6 uker ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    SØKERS_INNLEGGELSE_SEKS_UKER_IKKE_DOKUMENTERT("4116", "§14-11, jf §21-3: Søkers innleggelse første 6 uker ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    BARNETS_INNLEGGELSE_SEKS_UKER_IKKE_DOKUMENTERT("4117", "§14-11, jf §21-3: Barnets innleggelse første 6 uker ikke dokumentert", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}", Set.of(UTSETTELSE), Set.of(MØDREKVOTE, FORELDREPENGER), Set.of(FRITT_UTTAK)),
    ;

    private static final Map<String, PeriodeResultatÅrsak> KODER = new LinkedHashMap<>();
    private static final String UDEFINERT_KODE = "-";

    public static final String KODEVERK = "PERIODE_RESULTAT_AARSAK";


    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    private String lovHjemmel;

    private UtfallType utfallType;

    private Set<UttakType> uttakTyper;

    private Set<StønadskontoType> valgbarForKonto;

    private Set<LovEndring> gyldigForLovendringer;

    PeriodeResultatÅrsak(String kode,
                         String navn,
                         String lovHjemmel,
                         Set<UttakType> uttakTyper,
                         Set<StønadskontoType> valgbarForKonto, Set<LovEndring> gyldigForLovendringer) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
        this.utfallType = UDEFINERT_KODE.equals(kode) ? null : (Long.parseLong(kode) < 4000L ? UtfallType.INNVILGET : UtfallType.AVSLÅTT);
        this.uttakTyper = uttakTyper == null ? Set.of(UTTAK) : uttakTyper;
        this.valgbarForKonto = valgbarForKonto == null ?
            Set.of(FELLESPERIODE, MØDREKVOTE, FEDREKVOTE, FORELDREPENGER, FORELDREPENGER_FØR_FØDSEL)
            : valgbarForKonto;
        this.gyldigForLovendringer = gyldigForLovendringer == null ? Set.of(LovEndring.values()) : gyldigForLovendringer;
    }

    PeriodeResultatÅrsak(String kode,
                         String navn,
                         String lovHjemmel,
                         Set<UttakType> uttakTyper,
                         Set<StønadskontoType> valgbarForKonto) {
        this(kode, navn, lovHjemmel, uttakTyper, valgbarForKonto, null);
    }

    PeriodeResultatÅrsak(String kode,
                         String navn,
                         String lovHjemmel,
                         Set<UttakType> uttakTyper) {
        this(kode, navn, lovHjemmel, uttakTyper, null, null);
    }

    public Set<UttakType> getUttakTyper() {
        return uttakTyper;
    }

    public Set<StønadskontoType> getValgbarForKonto() {
        return valgbarForKonto;
    }

    public Set<LovEndring> getGyldigForLovendringer() {
        return gyldigForLovendringer;
    }

    public UtfallType getUtfallType() {
        return utfallType;
    }

    public static PeriodeResultatÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeResultatÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, PeriodeResultatÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

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
    public static class KodeverdiConverter implements AttributeConverter<PeriodeResultatÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(PeriodeResultatÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public PeriodeResultatÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker() {
        return Set.of(MOR_HAR_IKKE_OMSORG, FAR_HAR_IKKE_OMSORG,
            BARNET_ER_DØD, SØKER_ER_DØD,
            OPPHØR_MEDLEMSKAP,
            FØDSELSVILKÅRET_IKKE_OPPFYLT,
            ADOPSJONSVILKÅRET_IKKE_OPPFYLT,
            FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT,
            OPPTJENINGSVILKÅRET_IKKE_OPPFYLT,
            STØNADSPERIODE_NYTT_BARN);
    }

    public static Set<PeriodeResultatÅrsak> årsakerTilAvslagPgaAnnenpart() {
        return Set.of(
            DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK,
            DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE);
    }

    public enum LovEndring {
        KREVER_SAMMENHENGENDE_UTTAK,
        FRITT_UTTAK
    }

    public enum UtfallType {
        INNVILGET,
        AVSLÅTT
    }
}
