package no.nav.foreldrepenger.domene.vedtak.intern;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;

public interface UtledeAvslutningsdatoFagsak {

    LocalDate utledAvslutningsdato(Long fagsakId, FagsakRelasjon relasjon);
}
