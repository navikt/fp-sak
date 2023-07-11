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
import javax.ws.rs.ext.Provider;

import org.hibernate.validator.internal.engine.path.PathImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.OverstyrteAksjonspunkterDto;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ConstraintViolationMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        log(exception);
        return lagResponse(exception);
    }

    private static void log(ConstraintViolationException exception) {
        var aksjonspunktKoder = finnAksjonspunktKoder(exception);
        //De fleste innkommende dto er klyttet til et aksjonspunkt
        var constraints = constraints(exception);
        LOG.warn("Det oppstod en valideringsfeil: Aksjonspunkt {} {}", aksjonspunktKoder, constraints);
    }

    private static Response lagResponse(ConstraintViolationException exception) {
        Collection<FeltFeilDto> feilene = new ArrayList<>();
        var koder = finnAksjonspunktKoder(exception);
        for (var constraintViolation : exception.getConstraintViolations()) {
            var feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
            feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage(), koder.toString()));
        }
        var feltNavn = feilene.stream().map(FeltFeilDto::getNavn).toList();
        var feilmelding = String.format(
            "Det oppstod en valideringsfeil på felt %s. " + "Vennligst kontroller at alle feltverdier er korrekte.",
            feltNavn);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(feilmelding, feilene))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Set<String> constraints(ConstraintViolationException exception) {
        return exception.getConstraintViolations()
            .stream()
            .map(cv -> cv.getRootBeanClass().getSimpleName() + "." + cv.getLeafBean().getClass().getSimpleName()
                + "." + fieldName(cv) + " - " + cv.getMessage())
            .collect(Collectors.toSet());
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

    private static String fieldName(ConstraintViolation<?> cv) {
        String field = null;
        for (var node : cv.getPropertyPath()) {
            field = node.getName();
        }
        return field;
    }

    private static String getKode(Object leafBean) {
        return leafBean instanceof AksjonspunktKode ? ((AksjonspunktKode) leafBean).getAksjonspunktDefinisjon().getKode() : null;
    }

    private static String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof PathImpl ? ((PathImpl) propertyPath).getLeafNode().toString() : null;
    }

}
