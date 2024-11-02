package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum HistorikkEndretFeltVerdiType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ADOPTERER_ALENE("ADOPTERER_ALENE", "adopterer alene"),
    ADOPTERER_IKKE_ALENE("ADOPTERER_IKKE_ALENE", "adopterer ikke alene"),
    ALENEOMSORG("ALENEOMSORG", "Søker har aleneomsorg for barnet"),
    BOSATT_I_NORGE("BOSATT_I_NORGE", "Søker er bosatt i Norge"),
    EKTEFELLES_BARN("EKTEFELLES_BARN", "ektefelles barn"),
    FORELDREANSVAR_2_TITTEL("FORELDREANSVAR_2_TITTEL", "Foreldreansvarsvilkåret §14-17 andre ledd"),
    FORELDREANSVAR_4_TITTEL("FORELDREANSVAR_4_TITTEL", "Foreldreansvarsvilkåret §14-17 fjerde ledd"),
    FORTSETT_BEHANDLING("FORTSETT_BEHANDLING", "Fortsett behandling"),
    HAR_GYLDIG_GRUNN("HAR_GYLDIG_GRUNN", "Gyldig grunn for sen fremsetting av søknaden"),
    HAR_IKKE_GYLDIG_GRUNN("HAR_IKKE_GYLDIG_GRUNN", "Ingen gyldig grunn for sen fremsetting av søknaden"),
    HENLEGG_BEHANDLING("HENLEGG_BEHANDLING", "Henlegg behandling"),
    IKKE_ALENEOMSORG("IKKE_ALENEOMSORG", "Søker har ikke aleneomsorg for barnet"),
    IKKE_BOSATT_I_NORGE("IKKE_BOSATT_I_NORGE", "Søker er ikke bosatt i Norge"),
    IKKE_EKTEFELLES_BARN("IKKE_EKTEFELLES_BARN", "ikke ektefelles barn"),
    IKKE_LOVLIG_OPPHOLD("IKKE_LOVLIG_OPPHOLD", "Ikke lovlig opphold"),
    IKKE_NY_I_ARBEIDSLIVET("IKKE_NY_I_ARBEIDSLIVET", "Endre til ikke ny i arbeidslivet"),
    IKKE_NYOPPSTARTET("IKKE_NYOPPSTARTET", "Endre til ikke nyoppstartet"),
    IKKE_OMSORG_FOR_BARNET("IKKE_OMSORG_FOR_BARNET", "Søker har ikke omsorg for barnet"),
    IKKE_OPPFYLT("IKKE_OPPFYLT", "ikke oppfylt"),
    IKKE_OPPHOLDSRETT("IKKE_OPPHOLDSRETT", "Ikke oppholdsrett"),
    IKKE_RELEVANT_PERIODE("IKKE_RELEVANT_PERIODE", "Ikke relevant periode"),
    IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD("IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD", "Endre til ikke tidsbegrenset arbeidsforhold"),
    INGEN_VARIG_ENDRING_NAERING("INGEN_VARIG_ENDRING_NAERING", "Ingen varig endring i næring"),
    LOVLIG_OPPHOLD("LOVLIG_OPPHOLD", "Lovlig opphold"),
    NY_I_ARBEIDSLIVET("NY_I_ARBEIDSLIVET", "Endre til ny i arbeidslivet"),
    NYOPPSTARTET("NYOPPSTARTET", "Endre til nyoppstartet"),
    OMSORG_FOR_BARNET("OMSORG_FOR_BARNET", "Søker har omsorg for barnet"),
    OMSORGSVILKARET_TITTEL("OMSORGSVILKARET_TITTEL", "Omsorgsvilkår §14-17 tredje ledd"),
    OPPFYLT("OPPFYLT", "oppfylt"),
    OPPHOLDSRETT("OPPHOLDSRETT", "Oppholdsrett"),
    PERIODE_MEDLEM("PERIODE_MEDLEM", "Periode med medlemskap"),
    PERIODE_UNNTAK("PERIODE_UNNTAK", "Periode med unntak fra medlemskap"),
    TIDSBEGRENSET_ARBEIDSFORHOLD("TIDSBEGRENSET_ARBEIDSFORHOLD", "Endre til tidsbegrenset arbeidsforhold"),
    VARIG_ENDRET_NAERING("VARIG_ENDRET_NAERING", "Varig endret næring"),
    VILKAR_IKKE_OPPFYLT("VILKAR_IKKE_OPPFYLT", "Vilkåret er ikke oppfylt"),
    VILKAR_OPPFYLT("VILKAR_OPPFYLT", "Vilkåret er oppfylt"),
    FASTSETT_RESULTAT_GRADERING_AVKLARES("FASTSETT_RESULTAT_GRADERING_AVKLARES", "Tilpass søknadsperiode og andel arbeid til inntektsmeldingen"),
    FASTSETT_RESULTAT_UTSETTELSE_AVKLARES("FASTSETT_RESULTAT_UTSETTELSE_AVKLARES", "Tilpass søknadsperiode til inntektsmeldingen"),
    FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE("FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE", "Perioden kan ikke avklares"),
    FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE("FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE", "Sykdommen/skaden er ikke dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE("FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE", "Innleggelsen er ikke dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT_IKKE("FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT_IKKE", "Øvelse eller tjeneste i heimevernet er ikke dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT_IKKE("FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT_IKKE", "Tiltak i regi av Nav er ikke dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT("FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT", "Sykdommen/skaden er dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT("FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT", "Innleggelsen er dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT("FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT", "Øvelse eller tjeneste i heimevernet er dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT("FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT", "Tiltak i regi av Nav er dokumentert"),
    FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN("FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN", "Endre søknadsperioden"),
    DOKUMENTERT("DOKUMENTERT", "dokumentert"),
    IKKE_DOKUMENTERT("IKKE_DOKUMENTERT", "ikke dokumentert"),
    GRADERING_OPPFYLT("GRADERING_OPPFYLT", "Oppfylt"),
    GRADERING_IKKE_OPPFYLT("GRADERING_IKKE_OPPFYLT", "Ikke oppfylt"),
    ANNEN_FORELDER_HAR_IKKE_RETT("ANNEN_FORELDER_HAR_IKKE_RETT", "Annen forelder har ikke rett"),
    ANNEN_FORELDER_HAR_RETT("ANNEN_FORELDER_HAR_RETT", "Annen forelder har rett"),
    GRADERING_PÅ_ANDEL_UTEN_BG_IKKE_SATT_PÅ_VENT("GRADERING_PÅ_ANDEL_UTEN_BG_IKKE_SATT_PÅ_VENT", "Ikke feil i inntektsgrunnlag, fortsett behandlingen"),
    NASJONAL("NASJONAL", "Nasjonal"),
    EØS_BOSATT_NORGE("EØS_BOSATT_NORGE", "EØS bosatt Norge"),
    BOSATT_UTLAND("BOSATT_UTLAND", "Bosatt utland"),
    SAMMENSATT_KONTROLL("SAMMENSATT_KONTROLL", "Sammensatt kontroll"),
    DØD_DØDFØDSEL("DØD_DØDFØDSEL", "Død/dødfødsel"),
    SELVSTENDIG_NÆRING("SELVSTENDIG_NÆRING", "Selvstendig næringsdrivende"),
    PRAKSIS_UTSETTELSE("PRAKSIS_UTSETTELSE", "Feil praksis utsettelse"),
    UTFØR_TILBAKETREKK("UTFØR_TILBAKETREKK", "Utfør tilbaketrekk"),
    HINDRE_TILBAKETREKK("HINDRE_TILBAKETREKK", "Hindre tilbaketrekk"),
    BENYTT("BENYTT", "Benytt"),
    IKKE_BENYTT("IKKE_BENYTT", "Ikke benytt"),
    INGEN_INNVIRKNING("INGEN_INNVIRKNING", "Ingen innvirkning"),
    INNVIRKNING("INNVIRKNING", "Innvirkning"),
    ;

    private static final Map<String, HistorikkEndretFeltVerdiType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_ENDRET_FELT_VERDI_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    private final String navn;
    @JsonValue
    private final String kode;

    HistorikkEndretFeltVerdiType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, HistorikkEndretFeltVerdiType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HistorikkEndretFeltVerdiType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkEndretFeltVerdiType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkEndretFeltVerdiType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static HistorikkEndretFeltVerdiType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent HistorikkEndretFeltVerdiType: " + kode);
            }
            return ad;
        }

    }
}
