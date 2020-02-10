package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class BekreftTerminbekreftelseValidator {

    private Period tidlistUtstedelseAvTerminBekreftelse;

    @SuppressWarnings("unused")
    private BekreftTerminbekreftelseValidator() {
        // for CDI
    }

    /**
     * @param tidligsteUtstedelseAvTerminBekreftelse - Periode for tidligst utstedelse av terminbekreftelse før termindato
     */
    @Inject
    public BekreftTerminbekreftelseValidator(
        @KonfigVerdi(value = "terminbekreftelse.tidligst.utstedelse.før.termin", defaultVerdi = "P18W3D") Period tidligsteUtstedelseAvTerminBekreftelse) {
        this.tidlistUtstedelseAvTerminBekreftelse = tidligsteUtstedelseAvTerminBekreftelse;
    }

    /*
     * Lagt opp til å returnere true hvis utstedt er for tidlig (termin - konfig)
     */
    boolean validerOpplysninger(BekreftTerminbekreftelseAksjonspunktDto dto) {
        return validerUtstedtdato(dto);

    }

    boolean validerUtstedtdato(BekreftTerminbekreftelseAksjonspunktDto dto) {
        LocalDate utstedtdato = dto.getUtstedtdato();
        LocalDate termindato = dto.getTermindato();
        return Objects.nonNull(termindato) && Objects.nonNull(utstedtdato) &&
            utstedtdato.isBefore(termindato.minus(tidlistUtstedelseAvTerminBekreftelse));
    }
}
