package no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SimulerOppdragDto {

    private Long behandlingId;
    private List<String> oppdragPrMottaker;

    public SimulerOppdragDto() {
        //
    }

    public SimulerOppdragDto(Long behandlingId, List<String> oppdragPrMottaker) {
        this.behandlingId = behandlingId;
        this.oppdragPrMottaker = oppdragPrMottaker;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public List<String> getOppdragPrMottaker() {
        return oppdragPrMottaker;
    }

    @JsonIgnore
    public List<String> getOppdragPrMottakerDecoded() {
        return oppdragPrMottaker.stream()
            .filter(Objects::nonNull)
            .map(str -> new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))
            .collect(Collectors.toList());
    }

    @JsonIgnore
    public static SimulerOppdragDto lagDto(Long behandlingId, List<String> råXml) {
        Objects.requireNonNull(råXml, "Rå XML kan ikke være null");
        var encoded = råXml.stream()
            .map(str -> Base64.getEncoder()
                .encodeToString(str.getBytes(StandardCharsets.UTF_8)))
            .collect(Collectors.toList());
        return new SimulerOppdragDto(behandlingId, encoded);
    }

}
