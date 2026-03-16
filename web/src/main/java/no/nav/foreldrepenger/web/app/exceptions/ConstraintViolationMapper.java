package no.nav.foreldrepenger.web.app.exceptions;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.OverstyrteAksjonspunkterDto;
import no.nav.vedtak.feil.FeilType;

@Priority(Priorities.USER - 100)
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ConstraintViolationMapper.class);


    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var koder = finnAksjonspunktKodeTekst(exception);
        var brukKoder = koder.isEmpty() ? koder : " " + koder;
        var feltFeil = getFeltFeil(exception, brukKoder);
        var feilTekst = getLoggTekst(feltFeil);
        var feilmelding = String.format("Det oppstod en valideringsfeil på felt %s. Vennligst kontroller at verdier er korrekte.", feilTekst);
        var feil = new no.nav.vedtak.feil.FeilDto(FeilType.VALIDERINGSFEIL, feilmelding);
        LOG.warn(feilTekst);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(feil)
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Set<FeltFeil> getFeltFeil(ConstraintViolationException exception, String aksjonspunktKoder) {
        return exception.getConstraintViolations()
            .stream()
            .map(v -> getFeltFeil(v, aksjonspunktKoder))
            .collect(Collectors.toSet());
    }

    // Hvis du vil ta med constraintViolation.getInvalidValue() - så vask teksten for å unngå logg-injeksjon (se testcase)
    private static FeltFeil getFeltFeil(ConstraintViolation<?> constraintViolation, String aksjonspunktKoder) {
        var root = Optional.ofNullable(constraintViolation.getRootBeanClass()).map(Class::getSimpleName).orElse("null");
        var leaf = Optional.ofNullable(constraintViolation.getLeafBean()).map(Object::getClass).map(Class::getSimpleName).orElse("null");
        var field = Optional.ofNullable(constraintViolation.getPropertyPath()).map(Path::toString).orElse("null");
        var start = Objects.equals(root, leaf) ? leaf : root + "." + leaf;
        var message = constraintViolation.getMessage() + aksjonspunktKoder;
        return new FeltFeil(start + "." + field, message);
    }

    private static String getLoggTekst(Collection<FeltFeil> feil) {
        return feil.stream()
            .map(f -> f.navn() + ": " + f.melding())
            .collect(Collectors.joining(", "));
    }

    private record FeltFeil(String navn, String melding) {}


    private static String finnAksjonspunktKodeTekst(ConstraintViolationException exception) {
        return String.join(",", finnAksjonspunktKoder(exception));
    }
    private static List<String> finnAksjonspunktKoder(ConstraintViolationException exception) {
        var førsteConstraint = exception.getConstraintViolations().iterator().next();
        var executableParameters = førsteConstraint.getExecutableParameters();
        if (executableParameters.length > 0) {
            var executableParameter = executableParameters[0];
            if (executableParameter instanceof BekreftedeAksjonspunkterDto bapd) {
                //Flere aksjonspunkt kan bekreftes i samme kall
                return bapd.getBekreftedeAksjonspunktDtoer().stream()
                    .map(ConstraintViolationMapper::getKode)
                    .filter(Objects::nonNull)
                    .toList();
            }
            if (executableParameter instanceof OverstyrteAksjonspunkterDto oad) {
                return oad.getOverstyrteAksjonspunktDtoer().stream()
                    .map(ConstraintViolationMapper::getKode)
                    .filter(Objects::nonNull)
                    .toList();
            }
        }
        return Optional.ofNullable(getKode(førsteConstraint.getLeafBean())).map(List::of).orElseGet(List::of);
    }

    private static String getKode(Object leafBean) {
        return leafBean instanceof AksjonspunktKode aksjonspunktKode ? aksjonspunktKode.getAksjonspunktDefinisjon().getKode() : null;
    }

}
