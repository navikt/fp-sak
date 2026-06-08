package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;


public record TilretteleggingEndringStatus(boolean erEndret,
                                           SvpTilretteleggingEntitet nyTilrettelegging,
                                           SvpTilretteleggingEntitet gammelTilrettelegging) {
}
