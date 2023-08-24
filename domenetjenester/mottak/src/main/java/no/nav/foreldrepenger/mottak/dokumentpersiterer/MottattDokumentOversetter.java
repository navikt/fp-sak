package no.nav.foreldrepenger.mottak.dokumentpersiterer;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;

import java.time.LocalDate;
import java.util.Optional;

public interface MottattDokumentOversetter<T extends MottattDokumentWrapper<?>> {

    void trekkUtDataOgPersister(T wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra);

}
