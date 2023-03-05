package no.nav.foreldrepenger.domene.medlem.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;

public class MedlemskapsperiodeKoder {

    // Brukes i request til MEDL2
    public enum PeriodeStatus {
        INNV,   // Innvilget
        AVSL,   // Avslått
        AVST,   // Avvist
        GYLD,   // Gyldig
        UAVK    // Uavklart
    }

    // Mappes til boolean erMedlem
    public enum PeriodeType {
        PMMEDSKP,  // Periode med medlemskap
        PUMEDSKP,  // Periode uten medlemskap
        E500INFO   // Utenlandsk id
    }

    // Mappes til MEDLEMSKAP_TYPE
    public enum Lovvalg {
        FORL,  // Foreløpig
        UAVK,  // Under avklaring
        ENDL   // Endelig
    }

    // Kodeverdier fra MEDL2 som mappes til kodeverk MedlemskapDekningType
    private static final Map<String, MedlemskapDekningType> dekningMap = initDekningMap();

    private static Map<String, MedlemskapDekningType> initDekningMap(){
        Map<String, MedlemskapDekningType> result = new HashMap<>(23 * 2); // InitialCapacity = N * 2, N is number of elements.

        result.put("FTL_2-6", MedlemskapDekningType.FTL_2_6);                 // Folketrygdloven § 2-6
        result.put("FTL_2-7_3_ledd_a", MedlemskapDekningType.FTL_2_7_A);      // Folketrygdloven § 2-7, 3. ledd bokstav a
        result.put("FTL_2-7_bok_a", MedlemskapDekningType.FTL_2_7_A);         // Folketrygdloven § 2-7 bokstav a
        result.put("FTL_2-7_3_ledd_b", MedlemskapDekningType.FTL_2_7_B);      // Folketrygdloven § 2-7, 3. ledd bokstav b
        result.put("FTL_2-7_bok_b", MedlemskapDekningType.FTL_2_7_B);         // Folketrygdloven § 2-7 bokstav b
        result.put("FTL_2-9_1_ledd_a", MedlemskapDekningType.FTL_2_9_1_A);    // Folketrygdloven § 2-9, 1. ledd bokstav a
        result.put("FTL_2-9_a", MedlemskapDekningType.FTL_2_9_1_A);           // Folketrygdloven § 2-9 a
        result.put("FTL_2-9_1_ledd_b", MedlemskapDekningType.FTL_2_9_1_B);    // Folketrygdloven § 2-9, 1. ledd bokstav b
        result.put("FTL_2-9_b", MedlemskapDekningType.FTL_2_9_1_B);           // Folketrygdloven § 2-9 b
        result.put("FTL_2-9_1_ledd_c", MedlemskapDekningType.FTL_2_9_1_C);    // Folketrygdloven § 2-9, 1. ledd bokstav c
        result.put("FTL_2-9_c", MedlemskapDekningType.FTL_2_9_1_C);           // Folketrygdloven § 2-9 c
        result.put("FTL_2-9_2_ld_jfr_1a", MedlemskapDekningType.FTL_2_9_2_A); // Folketrygdloven § 2-9, annet ledd, jfr. 1. ledd bokstav a
        result.put("FTL_2-9_2_ld_jfr_1c", MedlemskapDekningType.FTL_2_9_2_C); // Folketrygdloven § 2-9, annet ledd, jfr. 1. ledd bokstav c
        result.put("Full", MedlemskapDekningType.FULL);                       // Full
        result.put("IHT_Avtale", MedlemskapDekningType.IHT_AVTALE);           // Untatt pensjonsdel
        result.put("IHT_Avtale_Forord", MedlemskapDekningType.IHT_AVTALE);    // I henhold til avtale for land i forordningen
        result.put("Opphor", MedlemskapDekningType.OPPHOR);                   // Opphør
        result.put("Unntatt", MedlemskapDekningType.UNNTATT);                 // Unntatt

        // Øvrige kodeverdier fra MEDL2 som ikke har avklart mapping - bruker IHT_AVTALE for å lage aksjonspunkt
        result.put("FTL_2-9_2_ledd", MedlemskapDekningType.IHT_AVTALE);       // Folketrygdloven § 2-9, annet ledd
        result.put("IKKEPENDEL", MedlemskapDekningType.IHT_AVTALE);           // Ikke pensjonsdel
        result.put("PENDEL", MedlemskapDekningType.IHT_AVTALE);               // Pensjonsdel
        result.put("IT_DUMMY", MedlemskapDekningType.IHT_AVTALE);             // Konvertert fra Infotrygd, dekning ukjent
        result.put("IT_DUMMY_EOS", MedlemskapDekningType.IHT_AVTALE);         // Konvertert fra Infotrygd, dekning ukjent

        return Collections.unmodifiableMap(result);
    }

    public static Map<String, MedlemskapDekningType> getDekningMap() {
        return dekningMap;
    }
}
