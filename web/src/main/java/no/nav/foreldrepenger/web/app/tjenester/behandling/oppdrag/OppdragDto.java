package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public record OppdragDto(String saksnummer)  {

    public OppdragDto(Oppdragskontroll entitet) {
        this(entitet.getSaksnummer().getVerdi());
    }

}
