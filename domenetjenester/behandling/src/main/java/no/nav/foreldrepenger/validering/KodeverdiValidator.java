package no.nav.foreldrepenger.validering;

import java.util.Objects;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.vedtak.util.InputValideringRegex;

public class KodeverdiValidator implements ConstraintValidator<ValidKodeverk, Kodeverdi> {

    static final String invKode = "kodeverks kode feilet validering"; // NOSONAR
    static final String invNavn = "kodeverks navn feilet validering"; // NOSONAR

    Pattern kodeverkPattern = Pattern.compile(InputValideringRegex.KODEVERK);

    @Override
    public void initialize(ValidKodeverk validKodeverk) {
        // ikke noe å gjøre
    }

    @Override
    public boolean isValid(Kodeverdi kodeverdi, ConstraintValidatorContext context) {
        if (Objects.equals(null, kodeverdi)) {
            return true;
        }
        var ok = true;

        if (!gyldigKode(kodeverdi.getKode())) {
            context.buildConstraintViolationWithTemplate(invKode);
            ok = false;
        }

        if (!gyldigKodeverk(kodeverdi.getKodeverk())) {
            context.buildConstraintViolationWithTemplate(invNavn);
            ok = false;
        }

        return ok;
    }

    private boolean erTomEllerNull(String str) {
        return (Objects.equals(null, str) || str.isEmpty());
    }

    private boolean gyldigKode(String kode) {
        return (!erTomEllerNull(kode) && gyldigLengde(kode, 1, 100) && kodeverkPattern.matcher(kode).matches());
    }

    private boolean gyldigKodeverk(String kodeverk) {
        return (!erTomEllerNull(kodeverk) && gyldigLengde(kodeverk, 0, 256) && kodeverkPattern.matcher(kodeverk).matches());
    }

    private boolean gyldigLengde(String str, int min, int max) {
        return ((str.length() >= min) && (str.length() <= max));
    }

}
