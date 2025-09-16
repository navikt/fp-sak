package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;
import java.util.UUID;

public interface KlageFormKravLagreDto {
    boolean erKlagerPart();

    boolean erFristOverholdt();

    boolean erKonkret();

    boolean erSignert();

    UUID påKlagdBehandlingUuid();

    boolean erTilbakekreving();

    KlageTilbakekrevingDto klageTilbakekreving();

    default UUID hentpåKlagdEksternBehandlingUuId() {
        return erTilbakekreving() && klageTilbakekreving() != null ? klageTilbakekreving().tilbakekrevingUuid() : null;
    }

    LocalDate mottattDato();
}
