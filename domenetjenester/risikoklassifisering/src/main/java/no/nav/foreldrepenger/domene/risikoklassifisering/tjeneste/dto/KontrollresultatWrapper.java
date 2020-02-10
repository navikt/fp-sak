package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.domene.risikoklassifisering.modell.Kontrollresultat;

public class KontrollresultatWrapper {

    private UUID behandlingUuid;

    private Kontrollresultat kontrollresultatkode;

    public KontrollresultatWrapper(UUID behandlingUuid, Kontrollresultat kontrollresultatkode) {
        Objects.requireNonNull(behandlingUuid, "behandlingUuid");
        Objects.requireNonNull(kontrollresultatkode, "kontrollresultatKode");
        this.behandlingUuid = behandlingUuid;
        this.kontrollresultatkode = kontrollresultatkode;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Kontrollresultat getKontrollresultatkode() {
        return kontrollresultatkode;
    }
}
