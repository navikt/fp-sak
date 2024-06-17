package no.nav.foreldrepenger.historikk;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.vedtak.exception.TekniskException;

final class HistorikkInnsalgFeil {

    private HistorikkInnsalgFeil() {
    }

    static TekniskException manglerFeltForHistorikkInnslag(String type,
                                                           List<HistorikkinnslagFeltType> requiredFelt,
                                                           Set<HistorikkinnslagFeltType> foundFelter) {
        var requiredKoder = map(requiredFelt);
        var foundKoder = map(foundFelter);
        return new TekniskException("FP-876694",
            String.format("Historikkinnslag %s ikke riktig bygd. Required: %s. Found: %s", type, requiredKoder, foundKoder));
    }

    private static List<String> map(Collection<HistorikkinnslagFeltType> feltTyper) {
        return feltTyper.stream().map(HistorikkinnslagFeltType::getKode).toList();
    }

    static TekniskException manglerMinstEtFeltForHistorikkinnslag(String type, List<String> manglendeFelt) {
        return new TekniskException("FP-876693", String.format("For type %s, forventer minst et felt av type %s", type, manglendeFelt));
    }

    static TekniskException ukjentHistorikkinnslagType(String kode) {
        return new TekniskException("FP-876692", String.format("Ukjent historikkinnslagstype: %s", kode));
    }
}
