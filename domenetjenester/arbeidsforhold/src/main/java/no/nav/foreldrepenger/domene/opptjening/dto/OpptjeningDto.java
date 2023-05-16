package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.List;

public class OpptjeningDto {

    private FastsattOpptjeningDto fastsattOpptjening;
    private List<OpptjeningAktivitetDto> opptjeningAktivitetList;
    private List<FerdiglignetNæringDto> ferdiglignetNæring;


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
