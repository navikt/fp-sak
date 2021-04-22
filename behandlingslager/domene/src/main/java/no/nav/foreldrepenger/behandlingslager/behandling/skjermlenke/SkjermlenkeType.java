package no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
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
    FAKTA_OM_BEREGNING("FAKTA_OM_BEREGNING", "Fakta om beregning"),
    FAKTA_OM_FOEDSEL("FAKTA_OM_FOEDSEL", "Fakta om fødsel"),
    FAKTA_OM_FORDELING("FAKTA_OM_FORDELING", "Fakta om fordeling"),
    FAKTA_OM_MEDLEMSKAP("FAKTA_OM_MEDLEMSKAP", "Fakta om medlemskap"),
    FAKTA_OM_OMSORG_OG_FORELDREANSVAR("FAKTA_OM_OMSORG_OG_FORELDREANSVAR", "Fakta om omsorg og foreldreansvar"),
    FAKTA_OM_OPPTJENING("FAKTA_OM_OPPTJENING", "Fakta om opptjening"),
    FAKTA_OM_SIMULERING("FAKTA_OM_SIMULERING", "Simulering"),
    FAKTA_OM_UTTAK("FAKTA_OM_UTTAK", "Fakta om uttak"),
    FAKTA_OM_AKTIVITETSKRAV("FAKTA_OM_AKTIVITETSKRAV", "Fakta om aktivitetskrav"),
    FAKTA_OM_VERGE("FAKTA_OM_VERGE", "Fakta om verge/fullmektig"),
    FORMKRAV_KLAGE_KA("FORMKRAV_KLAGE_KA", "Formkrav klage KA"),
    FORMKRAV_KLAGE_NFP("FORMKRAV_KLAGE_NFP", "Formkrav klage NFP"),
    KLAGE_BEH_NFP("KLAGE_BEH_NFP", "Klageresultat Vedtaksinstans"),
    KLAGE_BEH_NK("KLAGE_BEH_NK", "Klageresultat Klageinstans"),
    KONTROLL_AV_SAKSOPPLYSNINGER("KONTROLL_AV_SAKSOPPLYSNINGER", "Kontroll av saksopplysninger"),
    OPPLYSNINGSPLIKT("OPPLYSNINGSPLIKT", "Opplysningsplikt"),
    PUNKT_FOR_ADOPSJON("PUNKT_FOR_ADOPSJON", "Adopsjon"),
    PUNKT_FOR_FOEDSEL("PUNKT_FOR_FOEDSEL", "Fødsel"),
    PUNKT_FOR_FORELDREANSVAR("PUNKT_FOR_FORELDREANSVAR", "Foreldreansvar"),
    PUNKT_FOR_MEDLEMSKAP("PUNKT_FOR_MEDLEMSKAP", "Medlemskap"),
    PUNKT_FOR_MEDLEMSKAP_LØPENDE("PUNKT_FOR_MEDLEMSKAP_LØPENDE", "Punkt for medlemskap løpende"),
    PUNKT_FOR_OMSORG("PUNKT_FOR_OMSORG", "Fakta om omsorg"),
    PUNKT_FOR_OPPTJENING("PUNKT_FOR_OPPTJENING", "Opptjening"),
    PUNKT_FOR_SVANGERSKAPSPENGER("PUNKT_FOR_SVANGERSKAPSPENGER", "Punkt for svangerskapspenger"),
    PUNKT_FOR_SVP_INNGANG("PUNKT_FOR_SVP_INNGANG", "Punkt for svangerskapspenger inngang"),
    SOEKNADSFRIST("SOEKNADSFRIST", "Søknadsfrist"),
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse"),
    UDEFINERT("-", "Ikke definert"),
    UTLAND("UTLAND", "Endret utland"),
    UTTAK("UTTAK", "Uttak"),
    VEDTAK("VEDTAK", "Vedtak"),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler"),
    ;

    private static final Map<String, SkjermlenkeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SKJERMLENKE_TYPE"; //$NON-NLS-1$



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

    private SkjermlenkeType(String kode) {
        this.kode = kode;
    }

    private SkjermlenkeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static SkjermlenkeType fraKode(@JsonProperty("kode") String kode) {
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

    @JsonProperty
    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    /**
     * Returnerer skjermlenketype for eit aksjonspunkt. Inneheld logikk for spesialbehandling av aksjonspunkt som ikkje ligg på aksjonspunktdefinisjonen.
     * @deprecated Brukes kun i totrinnskontroll og foreslå vedtak, bør også fjernes derfra og heller lagres på Aksjonspunktet (ikke definisjonen)
     */
    @Deprecated
    public static SkjermlenkeType finnSkjermlenkeType(AksjonspunktDefinisjon aksjonspunktDefinisjon, Behandling behandling) {
        if (AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE.equals(aksjonspunktDefinisjon) ||
            AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE.equals(aksjonspunktDefinisjon)) {
            return getSkjermlenkeTypeForMottattStotte(behandling.getVilkårTypeForRelasjonTilBarnet().orElse(null));
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
