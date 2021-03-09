package no.nav.foreldrepenger.web.app.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ConstraintViolationMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var constraintViolations = exception.getConstraintViolations();

        if (constraintViolations.isEmpty() && exception instanceof ResteasyViolationException) {
            var message = ((ResteasyViolationException) exception).getException().getMessage();
            LOG.error(message);
            return lagResponse(new FeilDto(FeilType.GENERELL_FEIL, "Det oppstod en serverfeil: Validering er feilkonfigurert."));
        }

        Collection<FeltFeilDto> feilene = new ArrayList<>();
        for (var constraintViolation : constraintViolations) {
            var kode = getKode(constraintViolation.getLeafBean());
            var feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
            feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage(), kode));
        }
        var feltNavn = feilene.stream().map(FeltFeilDto::getNavn).collect(Collectors.toList());

        var feilmelding = feilmelding(feltNavn);
        LOG.warn(feilmelding);
        return lagResponse(new FeilDto(feilmelding, feilene));
    }

    private String feilmelding(List<String> feltNavn) {
        if (erServerkall()) {
            return String.format("Det oppstod en valideringsfeil på felt %s ved kall fra server. "
                + "Vennligst kontroller at alle feltverdier er korrekte.", feltNavn);
        }
        return String.format("Det oppstod en valideringsfeil på felt %s. "
            + "Vennligst kontroller at alle feltverdier er korrekte.", feltNavn);
    }

    private static boolean erServerkall() {
        var brukerUuid = SubjectHandler.getSubjectHandler().getUid();
        return brukerUuid != null && brukerUuid.startsWith("srv");
    }

    private static Response lagResponse(FeilDto entity) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String getKode(Object leafBean) {
        return leafBean instanceof AksjonspunktKode ? ((AksjonspunktKode) leafBean).getKode() : null;
    }

    private String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof PathImpl ? ((PathImpl) propertyPath).getLeafNode().toString() : null;
    }

}
