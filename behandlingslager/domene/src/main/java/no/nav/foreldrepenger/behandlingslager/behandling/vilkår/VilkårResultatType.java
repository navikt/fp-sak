package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.vedtak.exception.TekniskException;

public enum VilkårResultatType implements Kodeverdi {
     INNVILGET("INNVILGET", "Innvilget"),
     AVSLÅTT("AVSLAATT", "Avslått"),
     IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),

     UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VilkårResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VILKAR_RESULTAT_TYPE";

    private final String navn;

    @JsonValue
    private final String kode;

    VilkårResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, VilkårResultatType> kodeMap() {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static VilkårResultatType utledInngangsvilkårUtfall(Set<VilkårUtfallType> vilkårUtfallene) {
        return utledInngangsvilkårUtfall(vilkårUtfallene, true);
    }

    static VilkårResultatType utledInngangsvilkårUtfall(Set<VilkårUtfallType> vilkårUtfallene, boolean exception) {
        var oppfylt = vilkårUtfallene.contains(VilkårUtfallType.OPPFYLT);
        var ikkeOppfylt = vilkårUtfallene.contains(VilkårUtfallType.IKKE_OPPFYLT);
        var ikkeVurdert = vilkårUtfallene.contains(VilkårUtfallType.IKKE_VURDERT);

        // Enkeltutfallene per vilkår sammenstilles til et samlet vilkårsresultat.
        // Høyest rangerte enkeltutfall ift samlet vilkårsresultat sjekkes først, deretter nest høyeste osv.
        VilkårResultatType vilkårResultatType;
        if (ikkeOppfylt) {
            vilkårResultatType = VilkårResultatType.AVSLÅTT;
        } else if (ikkeVurdert) {
            vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;
        } else if (oppfylt) {
            vilkårResultatType = VilkårResultatType.INNVILGET;
        } else if (!exception) {
            vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;
        } else {
            throw new TekniskException("FP-384251", "Ikke mulig å utlede gyldig vilkårsresultat fra enkeltvilkår");
        }

        return vilkårResultatType;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VilkårResultatType, String> {
        @Override
        public String convertToDatabaseColumn(VilkårResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static VilkårResultatType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent VilkårResultatType: " + kode);
            }
            return ad;
        }
    }

}
