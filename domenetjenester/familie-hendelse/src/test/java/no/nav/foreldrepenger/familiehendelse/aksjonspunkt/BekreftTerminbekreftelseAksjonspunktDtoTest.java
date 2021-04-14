package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;

public class BekreftTerminbekreftelseAksjonspunktDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        var module = new SimpleModule();
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        module.addSerializer(LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer(formatter));
        module.addDeserializer(LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer(formatter));
        objectMapper.registerModule(module);
    }

    @Test
    public void test_av_json_mapping() throws IOException {
        var terminbekreftelseAksjonspunktDto = bekreftFødselAksjonspunktDto();
        Writer jsonWriter = new StringWriter();

        objectMapper.writeValue(jsonWriter, terminbekreftelseAksjonspunktDto);

        jsonWriter.flush();
        var json = jsonWriter.toString();

        var objektFraJson = objectMapper.readValue(json, BekreftTerminbekreftelseAksjonspunktDto.class);

        assertThat(objektFraJson.getAntallBarn()).isEqualTo(terminbekreftelseAksjonspunktDto.getAntallBarn());
        assertThat(objektFraJson.getTermindato()).isEqualTo(terminbekreftelseAksjonspunktDto.getTermindato());
        assertThat(objektFraJson.getUtstedtdato()).isEqualTo(terminbekreftelseAksjonspunktDto.getUtstedtdato());
        assertThat(objektFraJson.getBegrunnelse()).isEqualTo(terminbekreftelseAksjonspunktDto.getBegrunnelse());
    }

    private BekreftTerminbekreftelseAksjonspunktDto bekreftFødselAksjonspunktDto() {
        return new BekreftTerminbekreftelseAksjonspunktDto("Test", LocalDate.now().plusDays(30), LocalDate.now().minusDays(10), 1);
    }

}
