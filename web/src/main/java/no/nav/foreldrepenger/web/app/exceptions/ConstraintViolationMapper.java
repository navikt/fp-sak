package no.nav.foreldrepenger.web.app.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.validering.FeltFeilDto;

public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ConstraintViolationMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var constraintViolations = exception.getConstraintViolations();

        if (constraintViolations.isEmpty() && exception instanceof ResteasyViolationException) {
            return håndterFeilKonfigurering((ResteasyViolationException) exception);
        }
        log(exception);
        return lagResponse(constraintViolations);
    }

    private void log(ConstraintViolationException exception) {
        LOG.warn("Det oppstod en valideringsfeil: {}", toString(exception));
    }

    private Response lagResponse(Set<ConstraintViolation<?>> constraintViolations) {
        Collection<FeltFeilDto> feilene = new ArrayList<>();
        for (var constraintViolation : constraintViolations) {
            var kode = getKode(constraintViolation.getLeafBean());
            var feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
            feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage(), kode));
        }
        var feltNavn = feilene.stream().map(FeltFeilDto::getNavn).collect(Collectors.toList());
        var feilmelding = String.format(
            "Det oppstod en valideringsfeil på felt %s. " + "Vennligst kontroller at alle feltverdier er korrekte.",
            feltNavn);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(feilmelding, feilene))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private Response håndterFeilKonfigurering(ResteasyViolationException exception) {
        var message = exception.getException().getMessage();
        LOG.error(message);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(FeilType.GENERELL_FEIL, "Det oppstod en serverfeil: Validering er feilkonfigurert."))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private List<String> toString(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(cv -> cv.getRootBeanClass().getSimpleName() + " - " + cv.getLeafBean().getClass().getSimpleName()
                + " - " + cv.getMessage())
            .collect(Collectors.toList());
    }

    private String getKode(Object leafBean) {
        return leafBean instanceof AksjonspunktKode ? ((AksjonspunktKode) leafBean).getKode() : null;
    }

    private String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof PathImpl ? ((PathImpl) propertyPath).getLeafNode().toString() : null;
    }

}
