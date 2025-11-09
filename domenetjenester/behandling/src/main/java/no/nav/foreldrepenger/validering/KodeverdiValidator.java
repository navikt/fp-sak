package no.nav.foreldrepenger.validering;

import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.vedtak.util.InputValideringRegex;

public class KodeverdiValidator implements ConstraintValidator<ValidKodeverk, Kodeverdi> {

    private static final String KODE_FEILET_VALIDERING = "kodeverks kode feilet validering";
    private static final String NAVN_FEILET_VALIDERING = "kodeverks navn feilet validering";

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
            context.buildConstraintViolationWithTemplate(KODE_FEILET_VALIDERING);
            ok = false;
        }

        return ok;
    }

    private boolean erIkkeNullEllerTom(String str) {
        return !Objects.isNull(str) && !str.isEmpty();
    }

    private boolean gyldigKode(String kode) {
        return erIkkeNullEllerTom(kode) && gyldigLengde(kode, 1, 100) && kodeverkPattern.matcher(kode).matches();
    }

    private boolean gyldigLengde(String str, int min, int max) {
        return str.length() >= min && str.length() <= max;
    }

}
