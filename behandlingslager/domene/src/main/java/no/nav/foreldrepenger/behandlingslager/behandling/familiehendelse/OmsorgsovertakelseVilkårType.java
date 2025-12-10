package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum OmsorgsovertakelseVilkårType implements Kodeverdi {

    ES_ADOPSJONSVILKÅRET("FP_VK_4", "Adopsjon § 14-17 første ledd",
        Avslagsårsak.EKTEFELLES_SAMBOERS_BARN,
        Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE),
    ES_FORELDREANSVARSVILKÅRET_2_LEDD("FP_VK_8", "Foreldreansvar § 14-17 andre ledd",
        Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR,
        Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET),
    ES_OMSORGSVILKÅRET("FP_VK_5", "Omsorg § 14-17 tredje ledd",
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O,
        Avslagsårsak.MOR_IKKE_DØD,
        Avslagsårsak.MOR_IKKE_DØD_VED_FØDSEL_OMSORG,
        Avslagsårsak.FAR_HAR_IKKE_OMSORG_FOR_BARNET),
    ES_FORELDREANSVARSVILKÅRET_4_LEDD("FP_VK_33", "Foreldreansvar § 14-17 fjerde ledd",
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F,
        Avslagsårsak.OMSORGSOVERTAKELSE_ETTER_56_UKER,
        Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA),
    FP_ADOPSJONSVILKÅRET("FP_VK_16", "Adopsjon § 14-5 første ledd",
        Avslagsårsak.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN),
    FP_FORELDREANSVARSVILKÅRET_2_LEDD("FP_VK_8F", "Foreldreansvar § 14-5 andre ledd",
        Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR,
        Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET),
    FP_STEBARNSADOPSJONSVILKÅRET("FP_VK_16S", "Stebarnsadopsjon § 14-5 tredje ledd",
        Avslagsårsak.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN),

    /* Legger inn udefinert kode. Må gjerne erstattes av noe annet dersom starttilstand er kjent. */
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),

    ;

    private static final Set<Avslagsårsak> FELLES_ÅRSAKER = Set.of(Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR, Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR, Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR);

    private static final Map<String, OmsorgsovertakelseVilkårType> KODER = new LinkedHashMap<>();

    private final String navn;

    private final Set<Avslagsårsak> avslagsårsaker;

    @JsonValue
    private final String kode;

    OmsorgsovertakelseVilkårType(String kode, String navn, Avslagsårsak... avslagsårsaker) {
        this.kode = kode;
        this.navn = navn;
        this.avslagsårsaker = Set.of(avslagsårsaker);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    public List<Avslagsårsak> getAvslagsårsaker() {
        var alleÅrsaker = new ArrayList<>(avslagsårsaker);
        alleÅrsaker.addAll(FELLES_ÅRSAKER);
        return alleÅrsaker;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<OmsorgsovertakelseVilkårType, String> {
        @Override
        public String convertToDatabaseColumn(OmsorgsovertakelseVilkårType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OmsorgsovertakelseVilkårType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static OmsorgsovertakelseVilkårType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent OmsorgsovertakelseVilkårType: " + kode);
            }
            return ad;
        }
    }
}
