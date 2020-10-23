package no.nav.foreldrepenger.økonomi.simulering.kontrakt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimuleringResultatDto {

    private Long sumFeilutbetaling;
    private boolean slåttAvInntrekk;

    public SimuleringResultatDto() {
    }

    public SimuleringResultatDto(Long sumFeilutbetaling, boolean slåttAvInntrekk) {
        this.sumFeilutbetaling = sumFeilutbetaling;
        this.slåttAvInntrekk = slåttAvInntrekk;
    }

    public Long getSumFeilutbetaling() {
        return sumFeilutbetaling;
    }


    public boolean isSlåttAvInntrekk() {
        return slåttAvInntrekk;
    }
}
