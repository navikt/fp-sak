package no.nav.foreldrepenger.validering;

import java.util.Collection;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.VLLogLevel;
import no.nav.vedtak.feil.Feilkode;

public class Valideringsfeil extends FunksjonellException {

    public Valideringsfeil(Collection<FeltFeilDto> feltFeil) {
        super(null, Valideringsfeil.valideringsfeil(feltFeil));
    }

    private static String valideringsfeil(Collection<FeltFeilDto> feltFeil) {
        var feltNavn = feltFeil.stream()
            .map(FeltFeilDto::getNavn)
            .toList();
        var feltMelding = feltFeil.stream()
            .map(f -> f.getNavn() + " - " + f.getMelding())
            .toList();
        return String.format("Validering av felt %s. Vennligst kontroller feltverdier: %s", feltNavn, feltMelding);
    }

    @Override
    public String getFeilkode() {
        return Feilkode.VALIDERING.name();
    }

    @Override
    public VLLogLevel getLogLevel() {
        return VLLogLevel.NOLOG;
    }

}
