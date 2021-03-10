package no.nav.foreldrepenger.web.app.exceptions;

import java.util.Collections;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.KanIkkeUtledeGjeldendeFødselsdatoException;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningException;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;

@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<ApplicationException> {

    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);

    private static final String BEHANDLING_ENDRET_FEIL = "FP-837578";
    private static final String FRITEKST_TOM_FEIL = "FP-290952";

    @Override
    public Response toResponse(ApplicationException exception) {
        var cause = exception.getCause();

        if (cause instanceof Valideringsfeil) {
            return handleValideringsfeil((Valideringsfeil) cause);
        }
        if (cause instanceof KanIkkeUtledeGjeldendeFødselsdatoException) {
            return handleTomtResultatFeil((TekniskException) cause);
        }

        loggTilApplikasjonslogg(cause);
        var callId = MDCOperations.getCallId();

        if (cause instanceof VLException) {
            return handleVLException((VLException) cause, callId);
        }

        return handleGenerellFeil(cause, callId);
    }

    private static Response handleTomtResultatFeil(TekniskException tomtResultatException) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, tomtResultatException.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response handleValideringsfeil(Valideringsfeil valideringsfeil) {
        var feltNavn = valideringsfeil.getFeltFeil().stream()
            .map(felt -> felt.getNavn())
            .collect(Collectors.toList());
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new FeilDto("Det oppstod valideringsfeil på felt " + feltNavn
                    + ". Vennligst kontroller at alle feltverdier er korrekte.", valideringsfeil.getFeltFeil()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response handleVLException(VLException vlException, String callId) {
        if (vlException instanceof ManglerTilgangException) {
            return ikkeTilgang(vlException);
        }
        if (FRITEKST_TOM_FEIL.equals(vlException.getKode())) {
            return handleValideringsfeil(new Valideringsfeil(Collections.singleton(new FeltFeilDto("fritekst",
                    vlException.getMessage()))));
        }
        if (BEHANDLING_ENDRET_FEIL.equals(vlException.getKode())) {
            return behandlingEndret(vlException);
        }
        return serverError(callId, vlException);
    }

    private static Response serverError(String callId, VLException feil) {
        var feilmelding = getVLExceptionFeilmelding(callId, feil);
        var feilType = FeilType.GENERELL_FEIL;
        return Response.serverError()
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response ikkeTilgang(VLException feil) {
        var feilmelding = feil.getMessage();
        var feilType = FeilType.MANGLER_TILGANG_FEIL;
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response behandlingEndret(VLException feil) {
        var feilmelding = feil.getMessage();
        var feilType = FeilType.BEHANDLING_ENDRET_FEIL;
        return Response.status(Response.Status.CONFLICT)
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static String getVLExceptionFeilmelding(String callId, VLException feil) {
        var feilbeskrivelse = feil.getMessage();
        if (feil instanceof FunksjonellException) {
            var løsningsforslag = ((FunksjonellException) feil).getLøsningsforslag();
            return String.format("Det oppstod en feil: %s - %s. Referanse-id: %s", feilbeskrivelse, løsningsforslag, callId);
        }
        return String.format("Det oppstod en serverfeil: %s. Meld til support med referanse-id: %s", feilbeskrivelse, callId);
    }

    private static Response handleGenerellFeil(Throwable cause, String callId) {
        var generellFeilmelding = "Det oppstod en serverfeil: " + cause.getMessage() + ". Meld til support med referanse-id: " + callId; //$NON-NLS-1$ //$NON-NLS-2$
        return Response.serverError()
                .entity(new FeilDto(FeilType.GENERELL_FEIL, generellFeilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static void loggTilApplikasjonslogg(Throwable cause) {
        if (cause instanceof ManglerTilgangException) {
            // ikke logg
        } else if (cause instanceof VLException) {
            LOG.warn(cause.getMessage(), cause);
        } else if (cause instanceof UnsupportedOperationException) {
            var message = cause.getMessage() != null ? LoggerUtils.removeLineBreaks(cause.getMessage()) : "";
            LOG.info("Fikk ikke-implementert-feil: {}", message, cause);
        } else if (cause instanceof ForvaltningException) {
            LOG.warn("Feil i bruk av forvaltningstjenester", cause);
        } else {
            var message = cause.getMessage() != null ? LoggerUtils.removeLineBreaks(cause.getMessage()) : "";
            LOG.error("Fikk uventet feil: " + message, cause);
        }

        // key for å tracke prosess -- nullstill denne
        MDC.remove("prosess"); //$NON-NLS-1$
    }

}
