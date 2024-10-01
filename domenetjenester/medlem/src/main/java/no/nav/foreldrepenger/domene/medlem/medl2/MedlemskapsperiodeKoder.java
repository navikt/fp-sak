package no.nav.foreldrepenger.domene.medlem.medl2;

import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;

public class MedlemskapsperiodeKoder {

    // Mappes til MEDLEMSKAP_TYPE
    public enum Lovvalg {
        FORL,  // Foreløpig
        UAVK,  // Under avklaring
        ENDL   // Endelig
    }

    // Kodeverdier fra MEDL2 som mappes til kodeverk MedlemskapDekningType
    public static final Map<String, MedlemskapDekningType> DEKNING_TYPE_MAP = Map.ofEntries(
        Map.entry("FTL_2-6", MedlemskapDekningType.FTL_2_6),                 // Folketrygdloven § 2-6
        Map.entry("FTL_2-7_3_ledd_a", MedlemskapDekningType.FTL_2_7_A),      // Folketrygdloven § 2-7, 3. ledd bokstav a
        Map.entry("FTL_2-7_bok_a", MedlemskapDekningType.FTL_2_7_A),         // Folketrygdloven § 2-7 bokstav a
        Map.entry("FTL_2-7_3_ledd_b", MedlemskapDekningType.FTL_2_7_B),      // Folketrygdloven § 2-7, 3. ledd bokstav b
        Map.entry("FTL_2-7_bok_b", MedlemskapDekningType.FTL_2_7_B),         // Folketrygdloven § 2-7 bokstav b
        Map.entry("FTL_2-9_1_ledd_a", MedlemskapDekningType.FTL_2_9_1_A),    // Folketrygdloven § 2-9, 1. ledd bokstav a
        Map.entry("FTL_2-9_a", MedlemskapDekningType.FTL_2_9_1_A),           // Folketrygdloven § 2-9 a
        Map.entry("FTL_2-9_1_ledd_b", MedlemskapDekningType.FTL_2_9_1_B),    // Folketrygdloven § 2-9, 1. ledd bokstav b
        Map.entry("FTL_2-9_b", MedlemskapDekningType.FTL_2_9_1_B),           // Folketrygdloven § 2-9 b
        Map.entry("FTL_2-9_1_ledd_c", MedlemskapDekningType.FTL_2_9_1_C),    // Folketrygdloven § 2-9, 1. ledd bokstav c
        Map.entry("FTL_2-9_c", MedlemskapDekningType.FTL_2_9_1_C),           // Folketrygdloven § 2-9 c
        Map.entry("FTL_2-9_2_ld_jfr_1a", MedlemskapDekningType.FTL_2_9_2_A), // Folketrygdloven § 2-9, annet ledd, jfr. 1. ledd bokstav a
        Map.entry("FTL_2-9_2_ld_jfr_1c", MedlemskapDekningType.FTL_2_9_2_C), // Folketrygdloven § 2-9, annet ledd, jfr. 1. ledd bokstav c
        Map.entry("Full", MedlemskapDekningType.FULL),                       // Full
        Map.entry("IHT_Avtale", MedlemskapDekningType.IHT_AVTALE),           // Untatt pensjonsdel
        Map.entry("IHT_Avtale_Forord", MedlemskapDekningType.IHT_AVTALE),    // I henhold til avtale for land i forordningen
        Map.entry("Opphor", MedlemskapDekningType.OPPHOR),                   // Opphør
        Map.entry("Unntatt", MedlemskapDekningType.UNNTATT),                 // Unntatt

        // Øvrige kodeverdier fra MEDL2 som ikke har avklart mapping - bruker IHT_AVTALE for å lage aksjonspunkt
        Map.entry("FTL_2-9_2_ledd", MedlemskapDekningType.IHT_AVTALE),       // Folketrygdloven § 2-9, annet ledd
        Map.entry("IKKEPENDEL", MedlemskapDekningType.IHT_AVTALE),           // Ikke pensjonsdel
        Map.entry("PENDEL", MedlemskapDekningType.IHT_AVTALE),               // Pensjonsdel
        Map.entry("IT_DUMMY", MedlemskapDekningType.IHT_AVTALE),             // Konvertert fra Infotrygd, dekning ukjent
        Map.entry("IT_DUMMY_EOS", MedlemskapDekningType.IHT_AVTALE));

}
