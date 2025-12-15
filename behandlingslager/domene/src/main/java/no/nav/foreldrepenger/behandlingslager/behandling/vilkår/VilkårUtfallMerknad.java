package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VilkårUtfallMerknad implements Kodeverdi {

    VM_1001("1001", "Søknad er sendt før 26. svangerskapsuke er passert og barnet er ikke født"),
    VM_1002("1002", "Søker er medmor (forelder2) og har søkt om engangsstønad til mor"),
    VM_1003("1003", "Søker er far og har søkt om engangsstønad til mor"),
    VM_1004("1004", "Barn over 15 år ved dato for omsorgsovertakelse"),
    VM_1005("1005", "Adopsjon av ektefellens barn"),
    VM_1006("1006", "Mann adopterer ikke alene"),

    VM_1007("1007", "Søknadsfristvilkåret"), //TODO: Vurder å bruke denne som merknad isf 5007
    VM_1019("1019", "Terminbekreftelse utstedt før 22. svangerskapsuke"),

    VM_1020("1020", "Bruker er registrert som ikke medlem"),
    VM_1023("1023", "Bruker ikke er registrert som norsk eller nordisk statsborger i TPS OG bruker ikke er registrert som borger av EU/EØS OG det ikke er avklart at bruker har lovlig opphold i Norge"),
    VM_1024("1024", "Bruker ikke er registrert som norsk eller nordisk statsborger i TPS OG bruker er registrert som borger av EU/EØS OG det ikke er avklart at bruker har oppholdsrett"),
    VM_1025("1025", "Bruker avklart som ikke bosatt."),

    VM_1026("1026", "Fødselsdato ikke oppgitt eller registrert"),
    VM_1027("1027", "ingen barn dokumentert på far/medmor"),
    VM_1028("1028", "mor fyller ikke vilkåret for sykdom"),

    VM_1035("1035", "Ikke tilstrekkelig opptjening"),

    VM_1041("1041", "for lavt brutto beregningsgrunnlag"),

    @Deprecated
    VM_5007("5007", "søknadsfristvilkåret"),
    @Deprecated
    VM_1021("1021", "Bruker er ikke registrert i TPS som bosatt i Norge"), // UTGÅTT, finnes i DB
    @Deprecated
    VM_1051("1051", "Stebarnsadopsjon ikke flere dager igjen"), // UTGÅTT, finnes i DB. Har vært brukt manuelt
    @Deprecated
    VM_7006("7006", "Venter på opptjeningsopplysninger"), // UTGÅTT, finnes i DB

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),

    ;

    private static final Map<String, VilkårUtfallMerknad> KODER = new LinkedHashMap<>();

    private final String navn;
    @JsonValue
    private final String kode;

    VilkårUtfallMerknad(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static VilkårUtfallMerknad fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårUtfallMerknad: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VilkårUtfallMerknad, String> {
        @Override
        public String convertToDatabaseColumn(VilkårUtfallMerknad attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårUtfallMerknad convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
