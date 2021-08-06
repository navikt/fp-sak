package no.nav.foreldrepenger.web.app.exceptions;

import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.KanIkkeUtledeGjeldendeFødselsdatoException;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingEndretException;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningException;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.jpa.TomtResultatException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;

@Provider
public class GenerellVLExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(GenerellVLExceptionMapper.class);


    @Override
    public Response toResponse(Throwable feil) {
        try {
            return handleException(feil);
        } finally {
            MDC.remove("prosess"); //$NON-NLS-1$
        }
    }

    public static Response handleException(Throwable feil) {
        if (feil instanceof KanIkkeUtledeGjeldendeFødselsdatoException || feil instanceof TomtResultatException) {
            return handleTomtResultatFeil(getTextForField(feil.getMessage()));
        }
        if (feil instanceof ManglerTilgangException) {
            return ikkeTilgang(getTextForField(feil.getMessage()));
        }
        if (feil instanceof BehandlingEndretException) {
            return behandlingEndret();
        }
        if (feil instanceof Valideringsfeil vfe) {
            return valideringsfeil(vfe);
        }
        loggTilApplikasjonslogg(feil);
        return serverError(getExceptionFeilmelding(feil));
    }

    private static Response handleTomtResultatFeil(String feilmelding) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response serverError(String feilmelding) {
        var feilType = FeilType.GENERELL_FEIL;
        return Response.serverError()
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response ikkeTilgang(String feilmelding) {
        var feilType = FeilType.MANGLER_TILGANG_FEIL;
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response behandlingEndret() {
        var feilmelding = "Behandlingen er endret av en annen saksbehandler, eller har blitt oppdatert med ny informasjon av systemet. Last inn behandlingen på nytt.";
        var feilType = FeilType.BEHANDLING_ENDRET_FEIL;
        return Response.status(Response.Status.CONFLICT)
                .entity(new FeilDto(feilType, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response valideringsfeil(Valideringsfeil valideringsfeil) {
        var feltNavn = valideringsfeil.getFeltFeil().stream()
            .map(FeltFeilDto::getNavn)
            .collect(Collectors.toList());
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto("Det oppstod valideringsfeil på felt " + feltNavn
                + ". Vennligst kontroller at alle feltverdier er korrekte.", valideringsfeil.getFeltFeil()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static String getExceptionFeilmelding(Throwable feil) {
        var callId = MDCOperations.getCallId();
        var feilbeskrivelse = getTextForField(feil.getMessage());
        if (feil instanceof FunksjonellException fe) {
            var løsningsforslag = getTextForField(fe.getLøsningsforslag());
            return String.format("Det oppstod en feil: %s - %s. Referanse-id: %s", feilbeskrivelse, løsningsforslag, callId);
        }
        return String.format("Det oppstod en serverfeil: %s. Meld til support med referanse-id: %s", feilbeskrivelse, callId);
    }

    private static void loggTilApplikasjonslogg(Throwable feil) {
        if (feil instanceof ForvaltningException) {
            LOG.warn("Feil i bruk av forvaltningstjenester", feil);
        } else {
            var melding = "Fikk uventet feil: " + getTextForField(feil.getMessage());
            LOG.warn(melding, feil);
        }
    }

    private static String getTextForField(String input) {
        return input != null ? LoggerUtils.removeLineBreaks(input) : "";
    }

}
