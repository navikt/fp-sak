package no.nav.foreldrepenger.domene.vedtak.intern;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;

import java.time.LocalDate;

public interface UtledeAvslutningsdatoFagsak {

    LocalDate utledAvslutningsdato(Long fagsakId, FagsakRelasjon relasjon);
}
