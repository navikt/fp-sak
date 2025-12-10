package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Avslagsårsak implements Kodeverdi {

    SØKT_FOR_TIDLIG("1001", "Søkt for tidlig"),
    SØKER_ER_MEDMOR("1002", "Søker er medmor"),
    SØKER_ER_FAR("1003", "Søker er far"),
    BARN_OVER_15_ÅR("1004", "Barn over 15 år"),
    EKTEFELLES_SAMBOERS_BARN("1005", "Ektefelles/samboers barn"),
    MANN_ADOPTERER_IKKE_ALENE("1006", "Mann adopterer ikke alene"),
    SØKT_FOR_SENT("1007", "Søkt for sent"),
    SØKER_ER_IKKE_BARNETS_FAR_O("1008", "Søker er ikke barnets far"),
    MOR_IKKE_DØD("1009", "Mor ikke død"),
    MOR_IKKE_DØD_VED_FØDSEL_OMSORG("1010", "Mor ikke død ved fødsel/omsorg"),
    ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR("1011", "Engangsstønad er allerede utbetalt til mor"),
    FAR_HAR_IKKE_OMSORG_FOR_BARNET("1012", "Far har ikke omsorg for barnet"),
    SØKER_HAR_IKKE_FORELDREANSVAR("1014", "Søker har ikke foreldreansvar"),
    SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET("1015", "Søker har hatt vanlig samvær med barnet"),
    SØKER_ER_IKKE_BARNETS_FAR_F("1016", "Søker er ikke barnets far"),
    OMSORGSOVERTAKELSE_ETTER_56_UKER("1017", "Omsorgsovertakelse etter 56 uker"),
    IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA("1018", "Ikke foreldreansvar alene etter barnelova"),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon"),
    SØKER_ER_IKKE_MEDLEM("1020", "Søker er ikke medlem"),
    SØKER_ER_UTVANDRET("1021", "Søker er utflyttet"),
    SØKER_HAR_IKKE_LOVLIG_OPPHOLD("1023", "Søker har ikke lovlig opphold"),
    SØKER_HAR_IKKE_OPPHOLDSRETT("1024", "Søker har ikke oppholdsrett (EØS)"),
    SØKER_ER_IKKE_BOSATT("1025", "Søker er ikke bosatt"),
    FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT("1026", "Fødselsdato ikke oppgitt eller registrert"),
    INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR("1027", "Ingen barn dokumentert på far/medmor"),
    MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM("1028", "Mor fyller ikke vilkåret for sykdom"),
    BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET("1029", "Bruker er ikke registrert som far/medmor til barnet"),
    ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR("1031", "Engangsstønad er allerede utbetalt til mor"),
    FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR("1032", "Foreldrepenger er allerede utbetalt til mor"),
    ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR("1033", "Engangsstønad er allerede utbetalt til far/medmor "),
    FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR("1034", "Foreldrepenger er allerede utbetalt til far/medmor"),
    IKKE_TILSTREKKELIG_OPPTJENING("1035", "Ikke tilstrekkelig opptjening"),
    FOR_LAVT_BEREGNINGSGRUNNLAG("1041", "For lavt brutto beregningsgrunnlag"),
    STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN("1051", "Stebarnsadopsjon ikke flere dager igjen"),
    SØKER_INNFLYTTET_FOR_SENT("1052", "Innflyttet mindre enn 12 måneder før termin/omsorgsovertakelse"),
    SØKER_IKKE_GRAVID_KVINNE("1060", "§ 14-4 første ledd: Søker er ikke gravid kvinne"),
    SØKER_ER_IKKE_I_ARBEID("1061", "§ 14-4 tredje ledd: Søker er ikke i arbeid/har ikke tap av pensjonsgivende inntekt"),
    SØKER_HAR_MOTTATT_SYKEPENGER("1062", "§ 14-4 første ledd: Søker har mottatt sykepenger"),
    ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER("1063", "§ 14-4 første ledd: Arbeidstaker har ikke dokumentert risikofaktorer"),
    ARBEIDSTAKER_KAN_OMPLASSERES("1064", "§ 14-4 første ledd: Arbeidstaker kan omplasseres til annet høvelig arbeid"),
    SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER("1065", "§ 14-4 andre ledd: Næringsdrivende/frilanser har ikke dokumentert risikofaktorer"),
    SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE("1066", "§ 14-4 andre ledd: Næringsdrivende/frilanser har mulighet til å tilrettelegge sitt virke"),
    INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN("1099", "Ingen beregningsregler tilgjengelig i løsningen"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, Avslagsårsak> KODER = new LinkedHashMap<>();

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

    Avslagsårsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Avslagsårsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Avslagsårsak: " + kode);
        }
        return ad;
    }

    public static Optional<Avslagsårsak> fraDefinertKode(String kode) {
        return Optional.ofNullable(fraKode(kode)).filter(a -> !UDEFINERT.equals(a));
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    /**
     * Get vilkår dette avslaget kan opptre i.
     */
    public Set<VilkårType> getVilkårTyper() {
        return VilkårType.getVilkårTyper(this);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Avslagsårsak, String> {
        @Override
        public String convertToDatabaseColumn(Avslagsårsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Avslagsårsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
