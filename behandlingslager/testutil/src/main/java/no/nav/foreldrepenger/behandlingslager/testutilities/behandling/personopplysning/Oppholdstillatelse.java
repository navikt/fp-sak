package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.historikk.OppholdstillatelsePeriode;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record Oppholdstillatelse(AktørId aktørId, OppholdstillatelsePeriode oppholdstillatelsePeriode)  {

}
