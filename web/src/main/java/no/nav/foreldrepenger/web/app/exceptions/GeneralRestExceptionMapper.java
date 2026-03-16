package no.nav.foreldrepenger.web.app.exceptions;

import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.MDC;

import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingEndretException;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaOmUttakReutledetAksjonspunkt;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.OppdragForventetNedetidException;
import no.nav.vedtak.log.util.LoggerUtils;

@Provider
public class GeneralRestExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable feil) {
        try {
            return handleException(feil);
        } finally {
            MDC.remove("prosess");
        }
    }

    // if (feil instanceof ForvaltningException) - warn("Feil i bruk av forvaltningstjenester", feil); else "Fikk uventet feil: " + getExceptionMelding(feil)

    public static Response handleException(Throwable feil) {
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
        no.nav.vedtak.server.rest.GeneralRestExceptionMapper.loggTilApplikasjonslogg(feil, true);
       return no.nav.vedtak.server.rest.GeneralRestExceptionMapper.handleException(feil);
    }

    private static Response oppdragNedetid(String feilmelding) {
        return Response
            .status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(new no.nav.vedtak.feil.FeilDto("OPPDRAG_FORVENTET_NEDETID", feilmelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response behandlingEndret() {
        var feilmelding = "Behandlingen er endret av en annen saksbehandler, eller har blitt oppdatert med ny informasjon av systemet. Last inn behandlingen på nytt.";
        return Response.status(Response.Status.CONFLICT)
                .entity(new no.nav.vedtak.feil.FeilDto("BEHANDLING_ENDRET_FEIL", feilmelding))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response valideringsfeil(Valideringsfeil valideringsfeil) {
        var feltNavn = valideringsfeil.getFeltFeil().stream()
            .map(FeltFeilDto::getNavn)
            .toList();
        var feltMelding = valideringsfeil.getFeltFeil().stream()
            .map(f -> f.getNavn() + ": " + f.getMelding())
            .collect(Collectors.joining(", "));
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new no.nav.vedtak.feil.FeilDto("Det oppstod valideringsfeil på felt " + feltNavn
                + ". Vennligst kontroller at alle feltverdier er korrekte.", feltMelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static Response faktaUttakReutledetAksjonspunkt(String exceptionMelding) {
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new no.nav.vedtak.feil.FeilDto(no.nav.vedtak.feil.FeilType.GENERELL_FEIL, exceptionMelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private static String getExceptionMelding(Throwable feil) {
        return getTextForField(feil.getMessage());
    }

    private static String getTextForField(String input) {
        return input != null ? LoggerUtils.removeLineBreaks(input) : "";
    }

}
