package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ØkonomiKodeFagområde {
    REFUTG, //For engangsstønad
    FP,     //For foreldrepenger, bruker
    FPREF,  //For foreldrepenger, arbeidsgiver
    SVP,    //For svangerskapspenger, bruker
    SVPREF;  //For svangerskapspenger, arbeidsgiver


    public static boolean gjelderForeldrepenger(String fagområde) {
        valider(fagområde);
        return FP.name().equals(fagområde) || FPREF.name().equals(fagområde);
    }

    public static boolean gjelderRefusjonTilArbeidsgiver(String fagområde) {
        return FPREF.name().equals(fagområde) || SVPREF.name().equals(fagområde);
    }

    public static void valider(String fagområde) {
        List<String> values = Arrays.stream(ØkonomiKodeFagområde.values())
            .map(ØkonomiKodeFagområde::name)
            .collect(Collectors.toList());
        if (REFUTG.name().equals(fagområde) || !values.contains(fagområde)) {
            throw new IllegalArgumentException("Utvikler feil: ikke støtett fagområde: " + fagområde);
        }
    }
}
