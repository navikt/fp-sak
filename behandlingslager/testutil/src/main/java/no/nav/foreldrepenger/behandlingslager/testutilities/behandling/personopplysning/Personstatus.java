package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record Personstatus(AktørId aktørId, PersonstatusPeriode personstatusPeriode) {
}
