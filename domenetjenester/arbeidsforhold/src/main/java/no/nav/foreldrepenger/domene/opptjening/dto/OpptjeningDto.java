package no.nav.foreldrepenger.domene.opptjening.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OpptjeningDto {

    @NotNull private FastsattOpptjeningDto fastsattOpptjening;
    @NotNull private List<OpptjeningAktivitetDto> opptjeningAktivitetList;
    @NotNull private List<FerdiglignetNæringDto> ferdiglignetNæring;


    public OpptjeningDto() {
        // trengs for deserialisering av JSON
    }

    OpptjeningDto(FastsattOpptjeningDto fastsattOpptjening) {
        this.fastsattOpptjening = fastsattOpptjening;
    }

    public FastsattOpptjeningDto getFastsattOpptjening() {
        return fastsattOpptjening;
    }

    public void setFastsattOpptjening(FastsattOpptjeningDto fastsattOpptjening) {
        this.fastsattOpptjening = fastsattOpptjening;
    }

    public List<OpptjeningAktivitetDto> getOpptjeningAktivitetList() {
        return opptjeningAktivitetList;
    }

    public void setOpptjeningAktivitetList(List<OpptjeningAktivitetDto> opptjeningAktivitetList) {
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }

    public List<FerdiglignetNæringDto> getFerdiglignetNæring() {
        return ferdiglignetNæring;
    }

    public void setFerdiglignetNæring(List<FerdiglignetNæringDto> ferdiglignetNæring) {
        this.ferdiglignetNæring = ferdiglignetNæring;
    }
}
