package no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum SkjermlenkeType implements Kodeverdi {

    ANKE_MERKNADER("ANKE_MERKNADER", "Anke merknader"),
    ANKE_VURDERING("ANKE_VURDERING", "Anke vurdering"),
    BEREGNING_ENGANGSSTOENAD("BEREGNING_ENGANGSSTOENAD", "Beregning"),
    BEREGNING_FORELDREPENGER("BEREGNING_FORELDREPENGER", "Beregning"),
    BESTEBEREGNING("BESTEBEREGNING", "Besteberegning"),
    FAKTA_FOR_OMSORG("FAKTA_FOR_OMSORG", "Fakta om omsorg"),
    FAKTA_FOR_OPPTJENING("FAKTA_FOR_OPPTJENING", "Fakta om opptjening"),
    FAKTA_OM_ADOPSJON("FAKTA_OM_ADOPSJON", "Fakta om adopsjon"),
    FAKTA_OM_ARBEIDSFORHOLD("FAKTA_OM_ARBEIDSFORHOLD", "Fakta om arbeidsforhold"),
    FAKTA_OM_ARBEIDSFORHOLD_INNTEKTSMELDING("FAKTA_OM_ARBEIDSFORHOLD_INNTEKTSMELDING", "Fakta om arbeid og inntekt"),
    FAKTA_OM_ARBEIDSFORHOLD_PERMISJON("FAKTA_OM_ARBEIDSFORHOLD_PERMISJON", "Fakta om arbeidsforhold med permisjon uten sluttdato"),
    FAKTA_OM_BEREGNING("FAKTA_OM_BEREGNING", "Fakta om beregning"),
    FAKTA_OM_FOEDSEL("FAKTA_OM_FOEDSEL", "Fakta om fødsel"),
    FAKTA_OM_FORDELING("FAKTA_OM_FORDELING", "Fakta om fordeling"),
    FAKTA_OM_MEDLEMSKAP("FAKTA_OM_MEDLEMSKAP", "Fakta om medlemskap"),
    FAKTA_OM_OMSORG_OG_FORELDREANSVAR("FAKTA_OM_OMSORG_OG_FORELDREANSVAR", "Fakta om omsorg og foreldreansvar"),
    FAKTA_OM_OPPTJENING("FAKTA_OM_OPPTJENING", "Fakta om opptjening"),
    FAKTA_OM_SIMULERING("FAKTA_OM_SIMULERING", "Simulering"),
    FAKTA_OM_UTTAK("FAKTA_OM_UTTAK", "Fakta om uttak"),
    FAKTA_OM_AKTIVITETSKRAV("FAKTA_OM_AKTIVITETSKRAV", "Fakta om aktivitetskrav"),
    FAKTA_OMSORG_OG_RETT("FAKTA_OMSORG_OG_RETT", "Fakta om aleneomsorg og annenpart rett"),
    FAKTA_OM_VERGE("FAKTA_OM_VERGE", "Fakta om verge/fullmektig"),
    FORMKRAV_KLAGE_KA("FORMKRAV_KLAGE_KA", "Formkrav klage KA"),
    FORMKRAV_KLAGE_NFP("FORMKRAV_KLAGE_NFP", "Formkrav klage NFP"),
    KLAGE_BEH_NFP("KLAGE_BEH_NFP", "Klageresultat NFP"),
    KLAGE_BEH_NK("KLAGE_BEH_NK", "Klageresultat Klageinstansen"),
    KONTROLL_AV_SAKSOPPLYSNINGER("KONTROLL_AV_SAKSOPPLYSNINGER", "Fakta om saken"),
    OPPLYSNINGSPLIKT("OPPLYSNINGSPLIKT", "Opplysningsplikt"),
    PUNKT_FOR_ADOPSJON("PUNKT_FOR_ADOPSJON", "Adopsjon"),
    PUNKT_FOR_FOEDSEL("PUNKT_FOR_FOEDSEL", "Fødsel"),
    PUNKT_FOR_FORELDREANSVAR("PUNKT_FOR_FORELDREANSVAR", "Foreldreansvar"),
    PUNKT_FOR_MEDLEMSKAP("PUNKT_FOR_MEDLEMSKAP", "Medlemskap"),
    PUNKT_FOR_MEDLEMSKAP_LØPENDE("PUNKT_FOR_MEDLEMSKAP_LØPENDE", "Løpende medlemskap"),
    PUNKT_FOR_OMSORG("PUNKT_FOR_OMSORG", "Omsorg"),
    PUNKT_FOR_OPPTJENING("PUNKT_FOR_OPPTJENING", "Opptjening"),
    PUNKT_FOR_SVANGERSKAPSPENGER("PUNKT_FOR_SVANGERSKAPSPENGER", "Svangerskapspenger"),
    PUNKT_FOR_SVP_INNGANG("PUNKT_FOR_SVP_INNGANG", "Fakta om fødsel og tilrettelegging"),
    SOEKNADSFRIST("SOEKNADSFRIST", "Søknadsfrist"),
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse"),
    UDEFINERT("-", "Ikke definert"),
    UTLAND("UTLAND", "Endret utland"),
    UTTAK("UTTAK", "Uttak"),
    VEDTAK("VEDTAK", "Vedtak"),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler"),
    FAKTA_OM_UTTAK_DOKUMENTASJON("FAKTA_OM_UTTAK_DOKUMENTASJON", "Vurder dokumentasjon"),
    FAKTA_UTTAK("FAKTA_UTTAK", "Fakta om uttak"),
    FAKTA_UTTAK_EØS("FAKTA_UTTAK_EØS", "Fakta om uttak EØS");

    private static final Map<String, SkjermlenkeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SKJERMLENKE_TYPE";



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

    SkjermlenkeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static SkjermlenkeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SkjermlenkeType: " + kode);
        }
        return ad;
    }

    public static Map<String, SkjermlenkeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static boolean totrinnsSkjermlenke(SkjermlenkeType skjermlenkeType) {
        return skjermlenkeType != null && !SkjermlenkeType.UDEFINERT.equals(skjermlenkeType);
    }

    /**
     * Returnerer skjermlenketype for eit aksjonspunkt. Inneheld logikk for spesialbehandling av aksjonspunkt som ikkje ligg på aksjonspunktdefinisjonen.
     * @deprecated Brukes kun i totrinnskontroll og foreslå vedtak, bør også fjernes derfra og heller lagres på Aksjonspunktet (ikke definisjonen)
     */
    @Deprecated
    public static SkjermlenkeType finnSkjermlenkeType(AksjonspunktDefinisjon aksjonspunktDefinisjon, Behandling behandling,
                                                      Behandlingsresultat behandlingsresultat) {
        if (AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE.equals(aksjonspunktDefinisjon)) {
            return getSkjermlenkeTypeForMottattStotte(behandlingsresultat);
        }
        if (AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE.equals(aksjonspunktDefinisjon) ){
            return getSkjermlenkeTypeForOmsorgsovertakelse(behandling);
        }
        return aksjonspunktDefinisjon.getSkjermlenkeType();
    }

    private static SkjermlenkeType getSkjermlenkeTypeForOmsorgsovertakelse(Behandling behandling) {
        var fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)){
            return  SkjermlenkeType.FAKTA_OM_OMSORG_OG_FORELDREANSVAR;
        }
        return SkjermlenkeType.FAKTA_FOR_OMSORG;
    }

    public static SkjermlenkeType getSkjermlenkeTypeForMottattStotte(Behandlingsresultat behandlingsresultat) {
        var vilkårType = Optional.ofNullable(behandlingsresultat)
            .map(Behandlingsresultat::getVilkårResultat)
            .flatMap(VilkårResultat::getVilkårForRelasjonTilBarn).orElse(null);
        return getSkjermlenkeTypeForMottattStotte(vilkårType);
    }

    public static SkjermlenkeType getSkjermlenkeTypeForMottattStotte(VilkårType vilkårType) {
        if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårType) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(vilkårType)) {
            return SkjermlenkeType.PUNKT_FOR_FOEDSEL;
        }
        if (VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.equals(vilkårType) || VilkårType.ADOPSJONSVILKARET_FORELDREPENGER.equals(vilkårType)) {
            return SkjermlenkeType.PUNKT_FOR_ADOPSJON;
        }
        if (VilkårType.OMSORGSVILKÅRET.equals(vilkårType)) {
            return SkjermlenkeType.PUNKT_FOR_OMSORG;
        }
        if (VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD.equals(vilkårType) || VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD.equals(vilkårType)) {
            return SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
        }
        return SkjermlenkeType.UDEFINERT;
    }


    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<SkjermlenkeType, String> {
        @Override
        public String convertToDatabaseColumn(SkjermlenkeType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public SkjermlenkeType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
