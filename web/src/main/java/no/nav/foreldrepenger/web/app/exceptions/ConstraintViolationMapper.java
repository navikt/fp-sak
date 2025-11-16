package no.nav.foreldrepenger.web.app.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.OverstyrteAksjonspunkterDto;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return lagResponse(exception);
    }

    private static Response lagResponse(ConstraintViolationException exception) {
        Collection<FeltFeilDto> feilene = new ArrayList<>();
        var koder = finnAksjonspunktKoder(exception);
        for (var constraintViolation : exception.getConstraintViolations()) {
            var feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
            feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage(), koder.toString()));
        }
        var feltNavn = feilene.stream().map(FeltFeilDto::getNavn).toList();
        var feilmelding = String.format("Det oppstod en valideringsfeil på felt %s. " + "Vennligst kontroller at alle feltverdier er korrekte.",
            feltNavn);
        return Response.status(Response.Status.BAD_REQUEST).entity(new FeilDto(feilmelding, feilene)).type(MediaType.APPLICATION_JSON).build();
    }

    private static List<String> finnAksjonspunktKoder(ConstraintViolationException exception) {
        var førsteConstraint = exception.getConstraintViolations().iterator().next();
        var executableParameters = førsteConstraint.getExecutableParameters();
        if (executableParameters.length > 0) {
            var executableParameter = executableParameters[0];
            if (executableParameter instanceof BekreftedeAksjonspunkterDto) {
                //Flere aksjonspunkt kan bekreftes i samme kall
                return ((BekreftedeAksjonspunkterDto) executableParameter).getBekreftedeAksjonspunktDtoer()
                    .stream()
                    .map(ConstraintViolationMapper::getKode)
                    .toList();
            }
            if (executableParameter instanceof OverstyrteAksjonspunkterDto) {
                return ((OverstyrteAksjonspunkterDto) executableParameter).getOverstyrteAksjonspunktDtoer()
                    .stream()
                    .map(ConstraintViolationMapper::getKode)
                    .toList();
            }
        }
        var aksjonspunktKode = getKode(førsteConstraint.getLeafBean());
        if (aksjonspunktKode != null) {
            return List.of(aksjonspunktKode);
        }
        return List.of();
    }

    private static String getKode(Object leafBean) {
        return leafBean instanceof AksjonspunktKode ? ((AksjonspunktKode) leafBean).getAksjonspunktDefinisjon().getKode() : null;
    }

    private static String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof org.hibernate.validator.path.Path  pi ? pi.getLeafNode().toString() : null;
    }

}
