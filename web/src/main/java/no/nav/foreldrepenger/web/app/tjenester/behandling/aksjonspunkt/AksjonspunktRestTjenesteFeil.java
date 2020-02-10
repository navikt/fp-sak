package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

public interface AksjonspunktRestTjenesteFeil extends DeklarerteFeil {
    AksjonspunktRestTjenesteFeil FACTORY = FeilFactory.create(AksjonspunktRestTjenesteFeil.class);

    @FunksjonellFeil(feilkode = "FP-760743",
        feilmelding = "Det kan ikke akseptere endringer siden totrinnsbehandling er startet og behandlingen med behandlingId: %s er hos beslutter",
        løsningsforslag = "Avklare med beslutter", logLevel = WARN)
    Feil totrinnsbehandlingErStartet(String behandlingId);
}
