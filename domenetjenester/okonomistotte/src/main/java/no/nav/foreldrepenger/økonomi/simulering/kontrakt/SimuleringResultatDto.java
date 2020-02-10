package no.nav.foreldrepenger.økonomi.simulering.kontrakt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimuleringResultatDto {

    private Long sumFeilutbetaling;
    private Long sumInntrekk;
    private boolean slåttAvInntrekk;

    public SimuleringResultatDto() {
    }

    public SimuleringResultatDto(Long sumFeilutbetaling, Long sumInntrekk, boolean slåttAvInntrekk) {
        this.sumFeilutbetaling = sumFeilutbetaling;
        this.sumInntrekk = sumInntrekk;
        this.slåttAvInntrekk = slåttAvInntrekk;
    }

    public Long getSumFeilutbetaling() {
        return sumFeilutbetaling;
    }

    public Long getSumInntrekk() {
        return sumInntrekk;
    }

    public boolean isSlåttAvInntrekk() {
        return slåttAvInntrekk;
    }
}
