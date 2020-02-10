package no.nav.foreldrepenger.økonomi.simulering.kontrakt;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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
            .map(str -> {
                try {
                    return new String(Base64.getDecoder().decode(str.getBytes(Charset.forName("UTF-8"))), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("Klarte ikke finne tegnestt (skal ikke kunne skje)", e);
                }
            })
            .collect(Collectors.toList());
    }

    @JsonIgnore
    public static SimulerOppdragDto lagDto(Long behandlingId, List<String> råXml) {
        Objects.requireNonNull(råXml, "Rå XML kan ikke være null");
        List<String> encoded = råXml.stream()
            .map(str -> Base64.getEncoder()
                .encodeToString(str.getBytes(Charset.forName("UTF-8"))))
            .collect(Collectors.toList());
        return new SimulerOppdragDto(behandlingId, encoded);
    }

}
