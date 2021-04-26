package no.nav.foreldrepenger.behandlingslager.aktør;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record PersonIdentMedDiskresjonskode(PersonIdent personIdent, Diskresjonskode diskresjonskode) {}
