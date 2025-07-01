package no.nav.foreldrepenger.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingEndretException;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaOmUttakReutledetAksjonspunkt;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningException;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.OppdragForventetNedetidException;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.jpa.TomtResultatException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;

@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(GeneralRestExceptionMapper.class);


    @Override
    public Response toResponse(Throwable feil) {
        try {
            return handleException(feil);
        } finally {
            MDC.remove("prosess");
        }
    }

    public static Response handleException(Throwable feil) {
        if (feil instanceof TomtResultatException) {
            return handleTomtResultatFeil(getExceptionMelding(feil));
        }
        if (feil instanceof ManglerTilgangException) {
            return ikkeTilgang(getExceptionMelding(feil));
        }
        if (feil instanceof BehandlingEndretException) {
            return behandlingEndret();
        }
        if (feil instanceof Valideringsfeil vfe) {
            return valideringsfeil(vfe);
        }
        if (feil instanceof OppdragForventetNedetidException) {
            return oppdragNedetid(getExceptionMelding(feil));
        }
        if (feil instanceof FaktaOmUttakReutledetAksjonspunkt) {
            return faktaUttakReutledetAksjonspunkt(getExceptionMelding(feil));
        }
        loggTilApplikasjonslogg(feil);
        return serverError(getExceptionFullFeilmelding(feil));
    }

    private static Response oppdragNedetid(String feilmelding) {
        return Response
            .status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(new FeilDto(FeilType.OPPDRAG_FORVENTET_NEDETID, feilmelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response handleTomtResultatFeil(String feilmelding) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response serverError(String feilmelding) {
        return Response.serverError()
                .entity(new FeilDto(FeilType.GENERELL_FEIL, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response ikkeTilgang(String feilmelding) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new FeilDto(FeilType.MANGLER_TILGANG_FEIL, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response behandlingEndret() {
        var feilmelding = "Behandlingen er endret av en annen saksbehandler, eller har blitt oppdatert med ny informasjon av systemet. Last inn behandlingen på nytt.";
        return Response.status(Response.Status.CONFLICT)
                .entity(new FeilDto(FeilType.BEHANDLING_ENDRET_FEIL, feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response valideringsfeil(Valideringsfeil valideringsfeil) {
        var feltNavn = valideringsfeil.getFeltFeil().stream()
            .map(FeltFeilDto::getNavn)
            .toList();
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto("Det oppstod valideringsfeil på felt " + feltNavn
                + ". Vennligst kontroller at alle feltverdier er korrekte.", valideringsfeil.getFeltFeil()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response faktaUttakReutledetAksjonspunkt(String exceptionMelding) {
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(FeilType.GENERELL_FEIL, exceptionMelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static String getExceptionFullFeilmelding(Throwable feil) {
        var callId = MDCOperations.getCallId();
        var feilbeskrivelse = getExceptionMelding(feil);
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
            var melding = "Fikk uventet feil: " + getExceptionMelding(feil);
            LOG.warn(melding, feil);
        }
    }

    private static String getExceptionMelding(Throwable feil) {
        return getTextForField(feil.getMessage());
    }

    private static String getTextForField(String input) {
        return input != null ? LoggerUtils.removeLineBreaks(input) : "";
    }

}
