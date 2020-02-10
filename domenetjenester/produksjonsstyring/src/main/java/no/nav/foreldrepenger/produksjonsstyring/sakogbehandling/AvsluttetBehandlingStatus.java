package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

public class AvsluttetBehandlingStatus extends Behandlingsstatus {

    private String avslutningsStatus; //Kodeverk. http://nav.no/kodeverk/Kodeverk/Avslutningsstatuser

    public String getAvslutningsStatus() {
        return avslutningsStatus;
    }

    public void setAvslutningsStatus(String avslutningsStatus) {
        this.avslutningsStatus = avslutningsStatus;
    }
}
