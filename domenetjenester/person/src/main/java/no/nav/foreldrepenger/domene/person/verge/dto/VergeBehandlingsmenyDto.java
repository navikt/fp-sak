package no.nav.foreldrepenger.domene.person.verge.dto;

public class VergeBehandlingsmenyDto {
    private Long behandlingId;
    private VergeBehandlingsmenyEnum vergeBehandlingsmeny;

    public VergeBehandlingsmenyDto(Long behandlingId, VergeBehandlingsmenyEnum vergeBehandlingsmeny) {
        this.behandlingId = behandlingId;
        this.vergeBehandlingsmeny = vergeBehandlingsmeny;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public VergeBehandlingsmenyEnum getVergeBehandlingsmeny() {
        return vergeBehandlingsmeny;
    }
}
