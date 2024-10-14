package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.domene.typer.AktørId;


public record Statsborgerskap(AktørId aktørId, StatsborgerskapPeriode statsborgerskapPeriode) {
}
