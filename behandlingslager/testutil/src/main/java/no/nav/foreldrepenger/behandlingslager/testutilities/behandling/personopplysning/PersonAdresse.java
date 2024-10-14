package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record PersonAdresse(AktørId aktørId, AdressePeriode adressePeriode) {
}
