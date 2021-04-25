package no.nav.foreldrepenger.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.jboss.resteasy.spi.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ch.qos.logback.classic.Level;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.log.util.MemoryAppender;

@Execution(ExecutionMode.SAME_THREAD)
public class GeneralRestExceptionMapperTest {

    private static MemoryAppender logSniffer;

    private final GeneralRestExceptionMapper generalRestExceptionMapper = new GeneralRestExceptionMapper();

    @BeforeEach
    public void setUp() {
        logSniffer = MemoryAppender.sniff(GeneralRestExceptionMapper.class);
    }

    @AfterEach
    public void afterEach() {
        logSniffer.reset();
    }

    @Test
    public void skalMappeValideringsfeil() {
        var feltFeilDto = new FeltFeilDto("Et feltnavn", "En feilmelding");
        var valideringsfeil = new Valideringsfeil(Collections.singleton(feltFeilDto));

        var response = generalRestExceptionMapper.toResponse(new ApplicationException(valideringsfeil));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        var feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding())
                .isEqualTo("Det oppstod valideringsfeil på felt [Et feltnavn]. Vennligst kontroller at alle feltverdier er korrekte.");
        assertThat(feilDto.getFeltFeil()).hasSize(1);
        assertThat(feilDto.getFeltFeil().iterator().next()).isEqualTo(feltFeilDto);
    }

    @Test
    public void skalIkkeMappeManglerTilgangFeil() {
        var manglerTilgangFeil = manglerTilgangFeil();

        var response = generalRestExceptionMapper.toResponse(new ApplicationException(manglerTilgangFeil));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        var feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getType()).isEqualTo(FeilType.MANGLER_TILGANG_FEIL);
        assertThat(feilDto.getFeilmelding()).contains("ManglerTilgangFeilmeldingKode");
        assertThat(logSniffer.search("ManglerTilgangFeilmeldingKode", Level.WARN)).hasSize(0);
    }

    @Test
    public void skalMappeFunksjonellFeil() {
        var funksjonellFeil = funksjonellFeil();

        var response = generalRestExceptionMapper.toResponse(new ApplicationException(funksjonellFeil));

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        var feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains("FUNK_FEIL");
        assertThat(feilDto.getFeilmelding()).contains("en funksjonell feilmelding");
        assertThat(feilDto.getFeilmelding()).contains("et løsningsforslag");
        assertThat(logSniffer.search("en funksjonell feilmelding", Level.WARN)).hasSize(1);
    }

    @Test
    public void skalMappeVLException() {
        VLException vlException = tekniskFeil();

        var response = generalRestExceptionMapper.toResponse(new ApplicationException(vlException));

        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        var feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains("TEK_FEIL");
        assertThat(feilDto.getFeilmelding()).contains("en teknisk feilmelding");
        assertThat(logSniffer.search("en teknisk feilmelding", Level.WARN)).hasSize(1);
    }

    @Test
    public void skalMappeGenerellFeil() {
        var feilmelding = "en helt generell feil";
        var generellFeil = new RuntimeException(feilmelding);

        var response = generalRestExceptionMapper.toResponse(new ApplicationException(generellFeil));

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isInstanceOf(FeilDto.class);
        var feilDto = (FeilDto) response.getEntity();

        assertThat(feilDto.getFeilmelding()).contains(feilmelding);
        assertThat(logSniffer.search(feilmelding, Level.ERROR)).hasSize(1);
    }

    private static FunksjonellException funksjonellFeil() {
        return new FunksjonellException("FUNK_FEIL", "en funksjonell feilmelding", "et løsningsforslag");
    }

    private static TekniskException tekniskFeil() {
        return new TekniskException("TEK_FEIL", "en teknisk feilmelding");
    }

    private static ManglerTilgangException manglerTilgangFeil() {
        return new ManglerTilgangException("MANGLER_TILGANG_FEIL","ManglerTilgangFeilmeldingKode");
    }

}
